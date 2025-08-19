package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.github.GitUtil;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileGenerator {

    public static class Link {
        // --- Utilities to preserve/merge existing list links ---
        public static final Pattern A_TAG_PATTERN = Pattern.compile("<a\\s+[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE);
        private final String href;
        private final String label;
        public Link(String href, String label) {
            this.href = href;
            this.label = label;
        }
        public String getHref() { return href; }
        public String getLabel() { return label; }
    }

    private static final String TEMPLATE_NAME = "message";
    private static final String THREAD_TEMPLATE_NAME = "thread_message";

    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat folderFormat;
    private final SimpleDateFormat date8Format;
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final GitUtil gitUtil;
    private final GuildsDAO guildsDao;
    private final DiscordJdaProvider jdaProvider;
    private Long lastGuildId = null;
    private final String botVersion;
    
    public FileGenerator(ApplicationConfig config, Secrets secrets, TemplateEngine templateEngine,
                         GitUtil gitUtil, GuildsDAO guildsDao,
                         DiscordJdaProvider jdaProvider) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.gitUtil = gitUtil;
        this.guildsDao = guildsDao;
        this.jdaProvider = jdaProvider;
        this.timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        this.folderFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.folderFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        this.date8Format = new SimpleDateFormat("yyyyMMdd");
        this.date8Format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        this.botVersion = secrets.getBotVersion();
    }

    public Path generate(
            ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end,
            int seq
    ) {
        AnonymizationUtil.clearCache();
        
        // Sync local repo to latest before reading/writing outputs
        try {
            gitUtil.ensureRepoInitialized();
            gitUtil.fetch();
            gitUtil.pullRebase();
        } catch (Exception e) {
            // Non-fatal: continue generation even if git operations fail
            System.out.println("[GitSync] Skip or failed: " + e.getMessage());
        }
        
        // Ensure static assets like CSS exist in output directory
        try {
            ensureStaticAssets();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare static assets", e);
        }
        
        // remember guild context for subsequent index generation
        this.lastGuildId = channel.getGuildId();

        // If target is a Thread, generate a single archive file (no daily split)
        if (channel.isThread()) {
            return generateThreadArchive(channel, messages, begin, end, seq);
        }

        // Normalize copies of begin/end to avoid mutating caller's calendars
        Calendar cur = (Calendar) begin.clone();
        Calendar until = (Calendar) end.clone();

        // Track affected date8 values for index regeneration
        Set<String> affectedDate8 = new HashSet<>();

        Path lastOutput = null;
        // Capture a per-execution timestamp directory name so each run creates a new archive folder
        String runTimestamp = folderFormat.format(Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).getTime());

        while (!cur.after(until)) {
            // Segment end is end of current day (23:59:59) or the global end, whichever is earlier
            Calendar segmentEnd = (Calendar) cur.clone();
            segmentEnd.set(Calendar.HOUR_OF_DAY, 23);
            segmentEnd.set(Calendar.MINUTE, 59);
            segmentEnd.set(Calendar.SECOND, 59);
            segmentEnd.set(Calendar.MILLISECOND, 999);
            if (segmentEnd.after(until)) {
                segmentEnd = (Calendar) until.clone();
            }

            // Filter messages for [cur, segmentEnd]
            List<MessageInfo> segmentMessages = filterMessagesByRange(messages, cur, segmentEnd);

            if (!segmentMessages.isEmpty()) {
                Context context = new Context();
                context.setVariable("channel", channel);
                context.setVariable("messages", segmentMessages);
                context.setVariable("begin", timeFormat.format(cur.getTime()));
                context.setVariable("end", timeFormat.format(segmentEnd.getTime()));
                context.setVariable("sequence", seq);
                context.setVariable("backToChannelHref", String.format("../../archives/%s.html", channel.getName()));
                context.setVariable("backToTopHref", "/Discord2Html/index.html");
                context.setVariable("guildIconUrl", resolveGuildIconUrl());
                context.setVariable("botVersion", botVersion);
                // Add active thread links for this channel at the top
                try {
                    List<Link> activeThreadLinks = getActiveThreadLinks(channel);
                    if (activeThreadLinks != null && !activeThreadLinks.isEmpty()) {
                        context.setVariable("activeThreads", activeThreadLinks);
                    }
                } catch (Exception ignore) {
                    // best-effort; ignore failures
                }

                String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

                Path output = Path.of(
                        appConfig.getOutputPath(),
                        runTimestamp,
                        channel.getName()+ ".html"
                );
                writeHtml(output, htmlContent);

                lastOutput = output;

                // Mark affected date8 for indices
                affectedDate8.add(date8Format.format(segmentEnd.getTime()));
            }

            // Move to next day 00:00:00.000
            Calendar next = (Calendar) cur.clone();
            next.add(Calendar.DAY_OF_MONTH, 1);
            next.set(Calendar.HOUR_OF_DAY, 0);
            next.set(Calendar.MINUTE, 0);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);
            cur = next;
        }
        
        // Update per-day (date8) index for all affected dates
        try {
            for (String d8 : affectedDate8) {
                // Use a calendar set to the date for regenerateDailyIndex
                Calendar any = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                any.set(Integer.parseInt(d8.substring(0,4)),
                        Integer.parseInt(d8.substring(4,6)) - 1,
                        Integer.parseInt(d8.substring(6,8)),
                        0,0,0);
                any.set(Calendar.MILLISECOND, 0);
                regenerateDailyIndex(channel.getName(), any);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to regenerate daily index page(s)", e);
        }
        
        // After generating archive page(s), refresh static listings for GitHub Pages
        try {
            regenerateChannelArchives(channel.getName());
            regenerateTopIndex();
            regenerateHelpPage();
        } catch (IOException e) {
            // Do not fail the main generation if index regeneration fails; log via RuntimeException to keep visibility
            throw new RuntimeException("Failed to regenerate archives/index pages", e);
        }
        
        return lastOutput;
    }

    private void regenerateChannelArchives(String channelName) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        // Find all timestamp directories and collect files for this channel
        List<Path> timestampDirs = listTimestampDirs(base);
        // Sort by directory name (timestamp) descending
        timestampDirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());

        List<Link> items = new ArrayList<>();
        for (Path tsDir : timestampDirs) {
            Path file = tsDir.resolve(channelName + ".html");
            if (Files.exists(file)) {
                String ts = tsDir.getFileName().toString();
                String date8 = ts.length() >= 8 ? ts.substring(0, 8) : ts; // fallback if unexpected
                String href = "/Discord2Html/archives/" + date8 + "/" + channelName + ".html";
                String displayTs;
                try {
                    displayTs = timeFormat.format(folderFormat.parse(ts));
                } catch (Exception e) {
                    e.printStackTrace();
                    displayTs = ts;
                }
                String label = channelName + " (" + displayTs + ")";
                items.add(new Link(href, label));
            }
        }
        Path archivesDir = base.resolve("archives");
        Files.createDirectories(archivesDir);
        Path channelArchive = archivesDir.resolve(channelName + ".html");
        // If no items found, avoid overwriting an existing archive list to preserve past links
        if (items.isEmpty() && Files.exists(channelArchive)) {
            return;
        }
        List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(channelArchive));
        Context ctx = new Context();
        ctx.setVariable("title", channelName + " のアーカイブ一覧");
        ctx.setVariable("description", "以下のアーカイブから選択してください:");
        ctx.setVariable("items", merged);
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("list", ctx);
        writeIfChanged(channelArchive, page);
    }

    private void regenerateTopIndex() throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Set<String> channelNames = new HashSet<>();
        for (Path tsDir : listTimestampDirs(base)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tsDir, "*.html")) {
                for (Path p : stream) {
                    String fileName = p.getFileName().toString();
                    if (fileName.toLowerCase().endsWith(".html")) {
                        channelNames.add(fileName.substring(0, fileName.length() - 5));
                    }
                }
            }
        }
        // Build links list with Help at top and archives/channel.html entries
        List<Link> items = new ArrayList<>();
        items.addAll(channelNames.stream()
                .sorted()
                .map(name -> new Link("archives/" + name + ".html", name))
                .collect(Collectors.toList()));
        Path index = base.resolve("index.html");
        // Preserve existing index if nothing to list (e.g., output cleared)
        if (items.isEmpty() && Files.exists(index)) {
            return;
        }
        List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(index));
        Context ctx = new Context();
        ctx.setVariable("channels", merged);
        // Resolve guild name from DB if possible
        String guildName = "Discord";
        try {
            if (lastGuildId != null) {
                Guilds g = guildsDao.selectGuildInfo(new GuildId(lastGuildId));
                if (g != null && g.getGuildName() != null) {
                    guildName = g.getGuildName().getValue();
                }
            }
        } catch (Exception ignore) {
            // fallback to default on any error
        }
        ctx.setVariable("guildName", guildName);
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("top", ctx);
        writeIfChanged(index, page);
    }

    private void regenerateDailyIndex(String channelName, Calendar end) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        String date8 = date8Format.format(end.getTime());
        // Collect all timestamp directories for this date
        List<Path> tsDirs = listTimestampDirs(base).stream()
                .filter(p -> p.getFileName().toString().startsWith(date8))
                .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                .toList();

        List<Link> items = new ArrayList<>();
        for (Path tsDir : tsDirs) {
            Path file = tsDir.resolve(channelName + ".html");
            if (Files.exists(file)) {
                String ts = tsDir.getFileName().toString();
                // From archive/date8/channel.html to tsDir/channel.html -> ../../{ts}/{channel}.html
                String href = String.format("../../%s/%s.html", ts, channelName);
                String displayTs;
                try {
                    displayTs = timeFormat.format(folderFormat.parse(ts));
                } catch (Exception e) {
                    e.printStackTrace();
                    displayTs = ts;
                }
                String label = channelName + " (" + displayTs + ")";
                items.add(new Link(href, label));
            }
        }

        Path archiveBase = base.resolve("archives").resolve(date8);
        Files.createDirectories(archiveBase);
        Path dailyIndex = archiveBase.resolve(channelName + ".html");
        // If no items for this date, keep existing daily list if present
        if (items.isEmpty() && Files.exists(dailyIndex)) {
            return;
        }
        List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(dailyIndex));
        Context ctx = new Context();
        ctx.setVariable("title", channelName + " の" + date8 + " のログ一覧");
        ctx.setVariable("description", "同日のアーカイブへのリンク:");
        ctx.setVariable("items", merged);
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("list", ctx);
        writeIfChanged(dailyIndex, page);
    }

    private List<Path> listTimestampDirs(Path base) throws IOException {
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path p : stream) {
                if (Files.isDirectory(p) && p.getFileName().toString().matches("\\d{14}")) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private void regenerateHelpPage() throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path help = base.resolve("help.html");
        Context ctx = new Context();
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("help", ctx);
        writeIfChanged(help, page);
    }

    private void regenerateThreadIndex(String parentChannelName) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path parentThreadsDir = base.resolve("archives").resolve(parentChannelName).resolve("threads");
        if (!Files.exists(parentThreadsDir)) {
            return; // nothing to index
        }
        List<Link> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentThreadsDir, "*.html")) {
            for (Path p : stream) {
                String file = p.getFileName().toString();
                String href = "/Discord2Html/archives/" + parentChannelName + "/threads/" + file;
                String label = file.replaceFirst("\\.html$", "");
                // Derive thread name from JDA if possible (filename is t-<id>.html)
                String threadName = label;
                String idPart = label;
                if (label.startsWith("t-")) {
                    idPart = label.substring(2);
                }
                ThreadChannel thread = jdaProvider.getJda().getThreadChannelById(idPart);
                if (thread != null && !thread.getName().isEmpty()) {
                    threadName = thread.getName();
                }
                String updatedLabel = threadName;
                try {
                    updatedLabel = threadName
                            + " (" + timeFormat.format(new Date(Files.getLastModifiedTime(p).toMillis()))
                            + ")";
                } catch (IOException ignore) {
                    // ignore and use threadName only
                }
                items.add(new Link(href, updatedLabel));
            }
        }
        // write list page under archives/threads/{parent}/index.html
        Path indexDir = base.resolve("archives").resolve("threads").resolve(parentChannelName);
        Files.createDirectories(indexDir);
        Path index = indexDir.resolve("index.html");
        Context ctx = new Context();
        ctx.setVariable("title", parentChannelName + " のスレッド一覧");
        ctx.setVariable("description", "このチャンネルに属するスレッドのアーカイブ一覧");
        ctx.setVariable("items", items);
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("list", ctx);
        writeIfChanged(index, page);
    }

    private List<Link> getActiveThreadLinks(@NotNull ChannelInfo channel) {
        try {
            JDA jda = jdaProvider.getJda();
            TextChannel raw = jda.getChannelById(TextChannel.class, channel.getChannelId());
            if (raw == null) { return List.of(); }
            List<ThreadChannel> threads = raw.getThreadChannels();
            List<Link> links = new ArrayList<>();
            for (ThreadChannel t : threads) {
                if (!t.isArchived()) {
                    String href = "/Discord2Html/archives/" + channel.getName() + "/threads/t-" + t.getId() + ".html";
                    String label = t.getName();
                    links.add(new Link(href, label));
                }
            }
            // sort by id(t-href)
            links.sort(Comparator.comparing(Link::getHref));
            return links;
        } catch (Throwable e) {
            return List.of();
        }
    }

    private List<MessageInfo> filterMessagesByRange(List<MessageInfo> messages, Calendar start, Calendar end) {
        List<MessageInfo> result = new ArrayList<>();
        for (MessageInfo m : messages) {
            try {
                Date d = timeFormat.parse(m.getCreatedTimestamp());
                if (!d.before(start.getTime()) && !d.after(end.getTime())) {
                    result.add(m);
                }
            } catch (Exception ignore) {
                // Skip messages with unparsable timestamps
            }
        }
        return result;
    }

    private void writeHtml(Path output, String htmlContent) {
        try {
            Files.createDirectories(output.getParent());
            if (!Files.exists(output)) {
                output.toFile().createNewFile();
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to prepare HTML file", ioe);
        }
        try {
            writeIfChanged(output, htmlContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write HTML file", e);
        }
    }

    private void writeIfChanged(Path target, String newContent) throws IOException {
        String existing = null;
        if (Files.exists(target)) {
            existing = Files.readString(target, StandardCharsets.UTF_8);
        }
        if (existing == null || !existing.equals(newContent)) {
            try (FileWriter writer = new FileWriter(target.toString())) {
                writer.write(newContent);
            }
        }
    }

    /**
     * Ensure required static assets (like CSS) are present under the output root for GitHub Pages/local viewing.
     */
    private void ensureStaticAssets() throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        // Copy classpath:/static/css/style.css -> {output}/css/style.css
        Path cssDir = base.resolve("css");
        Files.createDirectories(cssDir);
        Path target = cssDir.resolve("style.css");
        byte[] data = readClasspathResource("/static/css/style.css");
        if (data != null) {
            String newContent = new String(data, StandardCharsets.UTF_8);
            writeIfChanged(target, newContent);
        }
        // Copy D2H_logo.png from classpath root to output root for favicon/header in help.html
        Path logoTarget = base.resolve("D2H_logo.png");
        byte[] logo = readClasspathResource("/D2H_logo.png");
        if (logo != null) {
            boolean shouldWrite = true;
            if (Files.exists(logoTarget)) {
                byte[] existing = Files.readAllBytes(logoTarget);
                shouldWrite = (existing == null || existing.length != logo.length || !java.util.Arrays.equals(existing, logo));
            }
            if (shouldWrite) {
                Files.write(logoTarget, logo);
            }
        }
    }

    private byte[] readClasspathResource(String resourcePath) throws IOException {
        try (var in = FileGenerator.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }

    /**
     * Resolve current guild icon URL if available.
     */
    private String resolveGuildIconUrl() {
        if (lastGuildId == null) {
            return null;
        }
        try {
            Guild guild = jdaProvider.getJda().getGuildById(lastGuildId);
            if (guild != null && guild.getIconUrl() != null && guild.getIconUrl().isEmpty() == false) {
                return guild.getIconUrl();
            }
        } catch (Exception ignore) {
            // ignore and return null
        }
        return null;
    }

    private Path generateThreadArchive(ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end, int seq) {
        Context ctx = new Context();
        ctx.setVariable("channel", channel);
        ctx.setVariable("messages", messages);
        ctx.setVariable("begin", timeFormat.format(begin.getTime()));
        ctx.setVariable("end", timeFormat.format(end.getTime()));
        ctx.setVariable("sequence", seq);
        if (channel.getParentChannelName() != null) {
            // Links for thread page navigation
            ctx.setVariable("backToParentThreadsHref", String.format("/Discord2Html/archives/threads/%s/index.html", channel.getParentChannelName()));
            ctx.setVariable("backToParentArchiveHref", String.format("/Discord2Html/archives/%s.html", channel.getParentChannelName()));
        } else {
            ctx.setVariable("backToParentThreadsHref", "/Discord2Html/index.html");
            ctx.setVariable("backToParentArchiveHref", "/Discord2Html/index.html");
        }
        ctx.setVariable("backToTopHref", "/Discord2Html/index.html");
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String html = templateEngine.process(THREAD_TEMPLATE_NAME, ctx);
        Path out = Path.of(
                appConfig.getOutputPath(),
                "archives",
                channel.getParentChannelName() == null ? "unknown" : channel.getParentChannelName(),
                "threads",
                "t-" + channel.getChannelId() + ".html"
        );
        writeHtml(out, html);
        try {
            if (channel.getParentChannelName() != null) {
                regenerateThreadIndex(channel.getParentChannelName());
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to regenerate thread index", ioe);
        }
        return out;
    }

    private List<Link> readExistingLinks(Path file) {
        try {
            if (Files.exists(file)) {
                String html = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = Link.A_TAG_PATTERN.matcher(html);
                List<Link> links = new ArrayList<>();
                while (m.find()) {
                    String href = m.group(1);
                    String label = m.group(2);
                    links.add(new Link(href, label));
                }
                return links;
            }
        } catch (IOException ignore) {
            // best effort
        }
        return List.of();
    }

    private String normalizeHref(String href) {
        if (href == null) { return ""; }
        if (href.startsWith("/Discord2Html/")) {
            return href.substring("/Discord2Html/".length());
        }
        // also strip leading ./ if any
        if (href.startsWith("./")) {
            return href.substring(2);
        }
        return href;
    }

    /**
     * Use LinkedHashMap to keep insertion order: 
     * prefer newly computed order then append any missing existing
     */
    private List<Link> mergeLinksPreserveAll(List<Link> newlyComputed, List<Link> existing) {

        Map<String, Link> byHref = new LinkedHashMap<>();
        for (Link l : newlyComputed) {
            byHref.put(normalizeHref(l.getHref()), l);
        }
        for (Link l : existing) {
            String key = normalizeHref(l.getHref());
            if (!byHref.containsKey(key)) {
                byHref.put(key, l);
            }
        }
        return new ArrayList<>(byHref.values());
    }
}
