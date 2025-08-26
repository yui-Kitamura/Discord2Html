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
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.github.GitUtil;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.dao.AnonStatsDAO;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.db.model.Users;
import pro.eng.yui.oss.d2h.consts.UserAnon;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

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

    public static class CategoryGroup {
        private final CategoryName name;
        private final CategoryId id; // unsigned decimal string or "0" for uncategorized
        private final List<Link> channels;
        private final boolean deleted; // category deleted or missing in live guild
        public CategoryGroup(CategoryId id, CategoryName name, boolean deleted) {
            this.id = id;
            this.name = name;
            this.deleted = deleted;
            this.channels = new ArrayList<>();
        }
        public CategoryName getName() { return name; }
        public CategoryId getId() { return id; }
        public List<Link> getChannels() { return channels; }
        public boolean isDeleted() { return deleted; }
    }

    private static final String TEMPLATE_NAME = "message";
    private static final String THREAD_TEMPLATE_NAME = "thread_message";

    private static final Pattern CUSTOM_EMOJI_PATTERN = Pattern.compile("<(a?):([A-Za-z0-9_~\\-]+):(\\d+)>" );

    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat folderFormat;
    private final SimpleDateFormat date8Format;
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final GitUtil gitUtil;
    private final GuildsDAO guildsDao;
    private final UsersDAO usersDao;
    private final AnonStatsDAO anonStatsDao;
    private final ChannelsDAO channelsDao;
    private final DiscordJdaProvider jdaProvider;
    private Long lastGuildId = null;
    private final String botVersion;
    
    public FileGenerator(ApplicationConfig config, Secrets secrets, TemplateEngine templateEngine,
                         GitUtil gitUtil, GuildsDAO guildsDao,
                         UsersDAO usersDao, AnonStatsDAO anonStatsDao,
                         ChannelsDAO channelsDao, DiscordJdaProvider jdaProvider) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.gitUtil = gitUtil;
        this.guildsDao = guildsDao;
        this.usersDao = usersDao;
        this.anonStatsDao = anonStatsDao;
        this.channelsDao = channelsDao;
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
        
        // Archive custom emojis used in messages to gh_pages resources
        try {
            archiveCustomEmojis(messages);
        } catch (IOException ioe) {
            // non-fatal: continue even if emoji archiving fails
            System.out.println("[EmojiArchive] failed: " + ioe.getMessage());
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

            // Always render the template, even if segmentMessages is empty.
            Context context = new Context();
            context.setVariable("channel", channel);
            context.setVariable("messages", segmentMessages);
            context.setVariable("begin", timeFormat.format(cur.getTime()));
            context.setVariable("end", timeFormat.format(segmentEnd.getTime()));
            context.setVariable("sequence", seq);
            context.setVariable("backToChannelHref", basePrefix() + "/archives/" + channel.getChannelId().toString() + ".html");
            context.setVariable("backToTopHref", basePrefix() + "/index.html");
            context.setVariable("guildIconUrl", resolveGuildIconUrl());
            context.setVariable("botVersion", botVersion);
            context.setVariable("basePrefix", basePrefix());
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
                    channel.getChannelId().toString() + ".html"
            );
            writeHtml(output, htmlContent);

            lastOutput = output;

            // Mark affected date8 for indices only when there were messages
            if (!segmentMessages.isEmpty()) {
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

        // If the run ends today and the execution time is not midnight (00:00),
        // ensure today's daily archive is regenerated to include 00:00 -> now,
        // even if no new messages were included in this run's segments.
        try {
            Calendar nowJst = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
            String today8 = date8Format.format(nowJst.getTime());
            String until8 = date8Format.format(until.getTime());
            boolean isMidnight = until.get(Calendar.HOUR_OF_DAY) == 0
                    && until.get(Calendar.MINUTE) == 0
                    && until.get(Calendar.SECOND) == 0;
            if (today8.equals(until8) && !isMidnight) {
                affectedDate8.add(until8);
            }
        } catch (Exception ignore) {
            // best-effort: do not block generation
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
                regenerateDailyIndex(channel.getChannelId(), any);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to regenerate daily index page(s)", e);
        }
        
        // After generating archive page(s), update listing pages by prepending new links without scanning directories
        try {
            // Prepend per-channel archive links for each affected date
            for (String d8 : affectedDate8) {
                prependChannelArchiveEntry(channel.getChannelId(), d8);
            }
            regenerateTopIndex();
            regenerateHelpPage();
        } catch (IOException e) {
            // Do not fail the main generation if index regeneration fails; log via RuntimeException to keep visibility
            throw new RuntimeException("Failed to regenerate archives/index pages", e);
        }
        
        return lastOutput;
    }

    private void prependChannelArchiveEntry(ChannelId channelId, String date8) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path archivesRoot = base.resolve("archives");
        Files.createDirectories(archivesRoot);
        // Build single link for the given date without scanning directories
        String href = basePrefix() + "/archives/" + date8 + "/" + channelId + ".html";
        // Resolve display name from JDA for labels
        String displayName = channelId.toString();
        try {
            var tc = jdaProvider.getJda().getTextChannelById(channelId.getValue());
            if (tc != null && tc.getName() != null && !tc.getName().isEmpty()) {
                displayName = tc.getName();
            }
        } catch (Throwable ignore) {}
        // Determine display timestamp similar to regenerateChannelArchives
        String displayTs;
        try {
            String today8 = date8Format.format(Calendar.getInstance().getTime());
            Path file = archivesRoot.resolve(date8).resolve(channelId.toString() + ".html");
            if (today8.equals(date8) && Files.exists(file)) {
                displayTs = timeFormat.format(new Date(Files.getLastModifiedTime(file).toMillis()));
            } else {
                Date endOfDay = folderFormat.parse(date8 + "235959");
                displayTs = timeFormat.format(endOfDay);
            }
        } catch (Exception e) {
            displayTs = date8;
        }
        String label = displayName + " (" + displayTs + ")";
        List<Link> items = new ArrayList<>();
        items.add(new Link(href, label));
        Path channelArchive = archivesRoot.resolve(channelId.toString() + ".html");
        List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(channelArchive));
        // Exclude thread index link from items to avoid duplication in the list
        final String threadIndexNorm = "archives/" + channelId + "/threads/index.html";
        merged = merged.stream()
                .filter(l -> !normalizeHref(l.getHref()).endsWith(threadIndexNorm))
                .collect(Collectors.toList());
        Context ctx = new Context();
        ctx.setVariable("title", displayName + " のアーカイブ一覧");
        ctx.setVariable("description", "以下のアーカイブから選択してください:");
        ctx.setVariable("items", merged);
        ctx.setVariable("threadIndexHref", basePrefix() + "/" + threadIndexNorm);
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("list", ctx);
        writeIfChanged(channelArchive, page);
        // Ensure the thread index page exists for this channel (even if no thread pages yet)
        try {
            regenerateThreadIndex(channelId);
        } catch (IOException ignore) { /* best-effort */ }
    }


    private void regenerateChannelArchives(ChannelId channelId) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path archivesRoot = base.resolve("archives");
        if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) {
            return;
        }
        // Find all date (yyyyMMdd) directories and collect daily files for this channel
        List<Path> dateDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archivesRoot)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (Files.isDirectory(p) && name.matches("\\d{8}")) {
                    dateDirs.add(p);
                }
            }
        }
        // Sort by directory name (date) descending
        dateDirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());

        List<Link> items = new ArrayList<>();
        // Resolve display name from JDA for labels
        String displayName = channelId.toString();
        try {
            var tc = jdaProvider.getJda().getTextChannelById(channelId.getValue());
            if (tc != null && tc.getName() != null && !tc.getName().isEmpty()) {
                displayName = tc.getName();
            }
        } catch (Throwable ignore) {}
        for (Path dateDir : dateDirs) {
            String date8 = dateDir.getFileName().toString();
            Path file = dateDir.resolve(channelId.toString() + ".html");
            if (Files.exists(file)) {
                String href = basePrefix() + "/archives/" + date8 + "/" + channelId + ".html";

                // Display timestamp policy for channel archive list:
                // - If the date is today (Asia/Tokyo), show the file's last updated time (最終更新日時)
                // - Otherwise, show the end of that day as 23:59:59
                String displayTs;
                try {
                    String today8 = date8Format.format(Calendar.getInstance().getTime());
                    if (today8.equals(date8)) {
                        // Use the last modified time of the generated daily file
                        displayTs = timeFormat.format(new Date(Files.getLastModifiedTime(file).toMillis()));
                    } else {
                        // Use 23:59:59 of the archive date
                        Date endOfDay = folderFormat.parse(date8 + "235959");
                        displayTs = timeFormat.format(endOfDay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    displayTs = date8;
                }
                String label = displayName + " (" + displayTs + ")";
                items.add(new Link(href, label));
            }
        }
        Path channelArchive = archivesRoot.resolve(channelId.toString() + ".html");
        Files.createDirectories(archivesRoot);
        // If no items found, avoid overwriting an existing archive list to preserve past links
        if (items.isEmpty() && Files.exists(channelArchive)) {
            return;
        }
        List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(channelArchive));
        // Exclude thread index link from items to avoid duplication in the list
        final String threadIndexNorm = "archives/" + channelId + "/threads/index.html";
        merged = merged.stream()
                .filter(l -> !normalizeHref(l.getHref()).endsWith(threadIndexNorm))
                .collect(Collectors.toList());
        Context ctx = new Context();
        ctx.setVariable("title", displayName + " のアーカイブ一覧");
        ctx.setVariable("description", "以下のアーカイブから選択してください:");
        ctx.setVariable("items", merged);
        // Add link to this channel's thread list page
        ctx.setVariable("threadIndexHref", basePrefix() + "/archives/" + channelId + "/threads/index.html");
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("list", ctx);
        // Ensure the thread index page exists for this channel
        try {
            regenerateThreadIndex(channelId);
        } catch (IOException ignore) { /* best-effort */ }
        writeIfChanged(channelArchive, page);
    }
    
    // Discover channel IDs that have at least one archived daily file under output/archives/yyyyMMdd/*.html
    private Set<String> discoverArchivedChannelIds() {
        Set<String> ids = new HashSet<>();
        try {
            Path base = Paths.get(appConfig.getOutputPath());
            if (!Files.exists(base)) { return ids; }
            Path archivesRoot = base.resolve("archives");
            if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) { 
                return ids;
            }
            try (DirectoryStream<Path> days = Files.newDirectoryStream(archivesRoot)) {
                for (Path dayDir : days) {
                    String name = dayDir.getFileName().toString();
                    if (!Files.isDirectory(dayDir) || !name.matches("\\d{8}")) { continue; }
                    try (DirectoryStream<Path> htmls = Files.newDirectoryStream(dayDir, "*.html")) {
                        for (Path p : htmls) {
                            String fileName = p.getFileName().toString();
                            ids.add(fileName.substring(0, fileName.length() - 5));
                        }
                    } catch (IOException ignore) {
                        // best-effort per day dir
                    }
                }
            }
        } catch (IOException ignore) {
            // best-effort: return what we have
        }
        return ids;
    }
    
    // Helper to ensure a CategoryGroup exists in the map
    private void ensureCategoryGroup(Map<CategoryId, CategoryGroup> map, Guild guild, CategoryId id, CategoryName name) {
        if (map.get(id) == null) {
            boolean deleted;
            CategoryName resolvedName;
            if (id.getValue() == 0) {
                // 未分類カテゴリ''
                deleted = false; 
                resolvedName = new CategoryName("");
            } else {
                boolean live = guild.getCategories().stream().anyMatch(c -> {
                    return new CategoryId(c).equals(id);
                });
                deleted = !live;
                resolvedName = name;
            }
            map.put(id, new CategoryGroup(id, resolvedName, deleted));
        }
    }

    private List<CategoryGroup> buildCategoryGroups() {
        if (lastGuildId == null) { return List.of(); }
        try {
            Guild guild = jdaProvider.getJda().getGuildById(lastGuildId);
            List<Channels> dbChannels = channelsDao.selectAllInGuild(new GuildId(lastGuildId));
            Set<String> archivedIds = discoverArchivedChannelIds();
            // Map categories by id string (or "0" for uncategorized)
            Map<CategoryId, CategoryGroup> map = new LinkedHashMap<>();
            List<CategoryId> liveOrder = new ArrayList<>();
            if (guild != null) {
                guild.getCategories().forEach(cat -> liveOrder.add(new CategoryId(cat)));
            }
            // Compose links per channel, mark deleted channels; only include archived channels
            for (Channels ch : dbChannels) {
                String chId = ch.getChannelId().toString();
                if (!archivedIds.contains(chId)) {
                    continue; // skip channels with no archives
                }
                CategoryId catId = ch.getCategoryId() == null ? new CategoryId(0) : ch.getCategoryId();
                CategoryName catName = ch.getCategoryName() == null ? new CategoryName(""): ch.getCategoryName();
                // If DB lacks category info, try to supplement from live JDA
                if (guild != null && catId.getValue() == 0) {
                    try {
                        TextChannel tc = jdaProvider.getJda().getTextChannelById(ch.getChannelId().getValue());
                        if (tc != null && tc.getParentCategory() != null) {
                            catId = new CategoryId(tc.getParentCategory());
                            CategoryName liveName = new CategoryName(tc.getParentCategory());
                            if (!liveName.getValue().isEmpty()) {
                                catName = liveName;
                            }
                        }
                    } catch (Throwable ignore) { /* best-effort */ }
                }
                ensureCategoryGroup(map, guild, catId, catName);
                CategoryGroup group = map.get(catId);
                String label = ch.getChannelName().getValue();
                if(group.isDeleted()) {
                    label += " (削除済み)";
                }
                String href = "archives/" + chId + ".html";
                group.getChannels().add(new Link(href, label));
            }
            // Order groups: live categories in guild order, then non-live (deleted) categories by name
            List<CategoryGroup> groups = new ArrayList<>();
            for (CategoryId id : liveOrder) {
                CategoryGroup g = map.get(id);
                if (g != null) { groups.add(g); }
            }
            // Ensure uncategorized ("0") comes after live categories but before deleted categories
            if (map.containsKey("0")) {
                boolean included = groups.stream().anyMatch(g -> g.getId().equals("0"));
                if (!included) { groups.add(map.get("0")); }
            }
            // Add remaining deleted categories (not live and not uncategorized) at the end sorted by name
            List<CategoryGroup> deletedGroups = map.values().stream()
                    .filter(g -> !liveOrder.contains(g.getId()) && g.getId().getValue()!= 0)
                    .sorted(Comparator.comparing(g -> g.getName().getValue()))
                    .toList();
            groups.addAll(deletedGroups);
            // Within each group, sort: existing channels first (no "(削除済み)" suffix), then deleted
            for (CategoryGroup g : groups) {
                List<Link> live = new ArrayList<>();
                List<Link> gone = new ArrayList<>();
                for (Link l : g.getChannels()) {
                    if (l.getLabel() != null && l.getLabel().endsWith("(削除済み)")) {
                        gone.add(l);
                    } else {
                        live.add(l);
                    }
                }
                live.sort(Comparator.comparing(Link::getLabel));
                gone.sort(Comparator.comparing(Link::getLabel));
                g.getChannels().clear();
                g.getChannels().addAll(live);
                g.getChannels().addAll(gone);
            }
            return groups;
        } catch (Throwable ignore) {
            return List.of();
        }
    }

    private void regenerateTopIndex() throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path archivesRoot = base.resolve("archives");
        if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) {
            return;
        }
        // Collect channelIds by scanning daily archive folders (yyyyMMdd)
        Set<String> channelIds = new HashSet<>();
        try (DirectoryStream<Path> days = Files.newDirectoryStream(archivesRoot)) {
            for (Path dayDir : days) {
                String name = dayDir.getFileName().toString();
                if (!Files.isDirectory(dayDir) || !name.matches("\\d{8}")) {
                    continue;
                }
                try (DirectoryStream<Path> htmls = Files.newDirectoryStream(dayDir, "*.html")) {
                    for (Path p : htmls) {
                        String fileName = p.getFileName().toString();
                        if (fileName.toLowerCase().endsWith(".html")) {
                            channelIds.add(fileName.substring(0, fileName.length() - 5));
                        }
                    }
                }
            }
        }
        // Ensure per-channel list pages exist for each discovered channel
        for (String id : channelIds) {
            try {
                regenerateChannelArchives(new ChannelId(Long.parseLong(id)));
            } catch (Throwable ignore) {
                // best-effort, continue
            }
        }
        // Build links list with Help at top and archives/channelId.html entries
        List<Link> items = new ArrayList<>(channelIds.stream()
                .sorted()
                .map(id -> {
                    String label = id;
                    try {
                        var tc = jdaProvider.getJda().getTextChannelById(id);
                        if (tc != null && tc.getName().isEmpty() == false) {
                            label = tc.getName();
                        }
                    } catch (Throwable ignore) { }
                    return new Link("archives/" + id + ".html", label);
                })
                .toList());
        Path index = base.resolve("index.html");
        Context ctx = new Context();
        
        if (items.isEmpty() && Files.exists(index)) {
            return;
        }
        List<CategoryGroup> groups = buildCategoryGroups();
        if (groups.isEmpty() == false) {
            // アクティブなcategoryで生成
            ctx.setVariable("categories", groups);
        } else {
            // カテゴリが存在しない場合
            List<Link> merged = mergeLinksPreserveAll(items, readExistingLinks(index));
            CategoryGroup synthetic = new CategoryGroup(new CategoryId(0), new CategoryName(""), false);
            for (Link l : merged) {
                synthetic.getChannels().add(l);
            }
            List<CategoryGroup> synthList = new ArrayList<>();
            synthList.add(synthetic);
            ctx.setVariable("categories", synthList);
        }

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

    private void regenerateDailyIndex(ChannelId channelId, Calendar end) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            base.toFile().mkdirs();
        }
        String date8 = date8Format.format(end.getTime());

        // Compute begin/end range for the day in Asia/Tokyo
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        String today8 = date8Format.format(now.getTime());

        Calendar beginCal = (Calendar) end.clone();
        Calendar endCal = (Calendar) end.clone();

        // Initialize begin to 00:00:00.000 of the target day
        beginCal.set(Calendar.HOUR_OF_DAY, 0);
        beginCal.set(Calendar.MINUTE, 0);
        beginCal.set(Calendar.SECOND, 0);
        beginCal.set(Calendar.MILLISECOND, 0);

        // Resolve channel from JDA using channel ID
        List<MessageInfo> messages = new ArrayList<>();
        TextChannel target = null;
        String displayChannelName = channelId.toString();
        try {
            target = jdaProvider.getJda().getTextChannelById(channelId.getValue());
            if (target != null) {
                if (!target.getName().isEmpty()) {
                    displayChannelName = target.getName();
                }
                messages = fetchMessagesForDaily(target, beginCal, endCal);
            }
        } catch (Throwable ignore) {
            // Fall through with empty messages on errors
        }

        // Sort chronologically just in case
        messages.sort(Comparator.comparing(MessageInfo::getCreatedTimestamp));

        // Prepare output path
        Path archiveBase = base.resolve("archives").resolve(date8);
        Files.createDirectories(archiveBase);
        Path dailyCombined = archiveBase.resolve(channelId.toString() + ".html");

        // Template variables
        String yyyy = date8.substring(0, 4);
        String mm = date8.substring(4, 6);
        String dd = date8.substring(6, 8);
        String humanDate = yyyy + "/" + mm + "/" + dd;

        String endText;
        if (today8.equals(date8)) {
            SimpleDateFormat hm = new SimpleDateFormat("HH:mm:ss");
            hm.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            endText = humanDate + " " + hm.format(now.getTime());
        } else {
            //0時実行のフルアーカイブ
            endText = humanDate + " 23:59:59";
        }

        Context ctx = new Context();
        ctx.setVariable("channelName", displayChannelName);
        ctx.setVariable("humanDate", humanDate);
        ctx.setVariable("endText", endText);
        ctx.setVariable("messages", messages);
        ctx.setVariable("backToTopHref", basePrefix() + "/index.html");
        ctx.setVariable("backToChannelHref", basePrefix() + "/archives/" + channelId + ".html");
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix());

        String rendered = templateEngine.process("daily", ctx);
        writeIfChanged(dailyCombined, rendered);
    }

    private List<MessageInfo> fetchMessagesForDaily(TextChannel channel, Calendar beginDate, Calendar endDate) {
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();

        try {
            var history = channel.getHistory();
            GuildId guildId = new GuildId(channel.getGuild());
            Guilds guildInfo = guildsDao.selectGuildInfo(guildId);
            int anonCycle = guildInfo != null && guildInfo.getAnonCycle() != null ? guildInfo.getAnonCycle().getValue() : 24;
            if (anonCycle < 1 || 24 < anonCycle) { 
                anonCycle = 24; 
            }
            final int finalAnonCycle = anonCycle;
            boolean more = true;
            while (more) {
                var batch = history.retrievePast(100).complete();
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                var oldest = batch.get(batch.size() - 1);
                var oldestInstant = oldest.getTimeCreated().toInstant();
                batch.stream()
                        .filter(msg -> msg.getTimeCreated().toInstant().isAfter(beginDate.toInstant())
                                && msg.getTimeCreated().toInstant().isBefore(endDate.toInstant()))
                        .forEach(msg -> {
                            Users author = null;
                            if (msg.getMember() != null) {
                                author = new Users(msg.getMember());
                                try {
                                    var anon = anonStatsDao.extractAnonStats(msg.getMember());
                                    author.setAnonStats(new AnonStats(anon));
                                } catch (Exception ignore) {
                                    // fallback below if needed
                                }
                            } else if (msg.getAuthor() != null) {
                                author = new Users(msg.getAuthor(), channel.getGuild());
                                UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
                                author.setAnonStats(new AnonStats(anonStatus));
                            }
                            if (author != null) {
                                try {
                                    if (!marked.contains(author)) {
                                        usersDao.upsertUserInfo(author);
                                        marked.add(author);
                                    }
                                } catch (Exception ignore) { /* ignore DB issues */ }
                                Date msgDate = Date.from(msg.getTimeCreated().toInstant());
                                Calendar calJst = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                                calJst.setTime(msgDate);
                                int hour = calJst.get(Calendar.HOUR_OF_DAY);
                                int cycleIndex = hour / finalAnonCycle;
                                String dateStr = date8Format.format(msgDate);
                                String scopeKey = guildId.toString() + "-" + dateStr + "-c" + cycleIndex + "-n" + finalAnonCycle;
                                messages.add(new MessageInfo(msg, author, scopeKey));
                            }
                        });
                if (!oldestInstant.isAfter(beginDate.toInstant())) {
                    more = false;
                }
            }
        } catch (Throwable ignore) {
            // best-effort: return what we have
        }
        return messages;
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

    private void regenerateThreadIndex(ChannelId parentChannelId) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Path parentThreadsDir = base.resolve("archives").resolve(parentChannelId.toString()).resolve("threads");
        // Ensure the directory exists so we can still generate an empty index page
        Files.createDirectories(parentThreadsDir);
        List<Link> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentThreadsDir, "*.html")) {
            for (Path p : stream) {
                String file = p.getFileName().toString();
                String href = basePrefix() + "/archives/" + parentChannelId + "/threads/" + file;
                String label = file.replaceFirst("\\.html$", "");
                // Derive thread name from JDA if possible (filename is t-<id>.html)
                String threadName = label;
                String idPart = label;
                if (label.startsWith("t-")) {
                    idPart = label.substring(2);
                }
                if (label.equals("index")) {
                    continue;
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
        // write list page under archives/<parent>/threads/index.html
        Path indexDir = parentThreadsDir;
        Files.createDirectories(indexDir);
        Path index = indexDir.resolve("index.html");
        // resolve display name of parent channel for title
        String parentDisplayName = parentChannelId.toString();
        try {
            var tc = jdaProvider.getJda().getTextChannelById(parentChannelId.getValue());
            if (tc != null && tc.getName() != null && !tc.getName().isEmpty()) {
                parentDisplayName = tc.getName();
            }
        } catch (Throwable ignore) {}
        Context ctx = new Context();
        ctx.setVariable("title", parentDisplayName + " のスレッド一覧");
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
            TextChannel raw = jda.getChannelById(TextChannel.class, channel.getChannelId().getValue());
            if (raw == null) { return List.of(); }
            List<ThreadChannel> threads = raw.getThreadChannels();
            List<Link> links = new ArrayList<>();
            for (ThreadChannel t : threads) {
                if (!t.isArchived()) {
                    String href = basePrefix() + "/archives/" + String.valueOf(channel.getChannelId()) + "/threads/t-" + t.getId() + ".html";
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
        // no-op comment to keep method start in place
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

    private void archiveCustomEmojis(List<MessageInfo> messages) throws IOException {
        if (messages == null || messages.isEmpty()){ return; }
        Path emojiDir = Paths.get(appConfig.getOutputPath(), "archives", "emoji");
        Files.createDirectories(emojiDir);
        String today = date8Format.format(new Date());

        // Avoid duplicate downloads in the same run by emoji id+ext
        Set<String> processed = new HashSet<>();
        for (MessageInfo mi : messages) {
            // --- 1) From message content ---
            String content = (mi == null) ? null : mi.getContentRaw();
            if (content != null && !content.isEmpty()) {
                Matcher m = CUSTOM_EMOJI_PATTERN.matcher(content);
                while (m.find()) {
                    boolean animated = m.group(1) != null && !m.group(1).isEmpty();
                    String name = m.group(2);
                    String id = m.group(3);
                    String ext = animated ? "gif" : "png";
                    String key = id + "." + ext;
                    if (processed.contains(key)) {
                        continue; // skip
                    }
                    processed.add(key);

                    String url = "https://cdn.discordapp.com/emojis/" + id + "." + ext;
                    byte[] bytes;
                    try (var in = new URL(url).openStream()) {
                        bytes = in.readAllBytes();
                    } catch (IOException ioe) {
                        // skip this emoji if fetch fails
                        bytes = null;
                    }
                    if (bytes != null) {
                        String safeName = (name == null) ? "emoji" : name;
                        safeName = safeName.replaceAll("[^A-Za-z0-9_-]", "_"); //不許容文字のreplace
                        Path target = emojiDir.resolve(safeName + "_" + today + "." + ext);

                        if (Files.exists(target)) {
                            try {
                                byte[] existing = Files.readAllBytes(target);
                                if (Arrays.equals(existing, bytes)) {
                                    // 同一ファイルの場合スキップ
                                } else {
                                    int idx = 2;
                                    Path alternativePath;
                                    do {
                                        alternativePath = emojiDir.resolve(safeName + "_" + today + "_" + idx + "." + ext);
                                        idx++;
                                    } while (Files.exists(alternativePath));
                                    Files.write(alternativePath, bytes);
                                }
                            } catch (IOException ioe) {
                                // ignore single emoji failure
                            }
                        } else {
                            try {
                                Files.write(target, bytes);
                            } catch (IOException ioe) {
                                // ignore single emoji failure
                            }
                        }
                        // Also write stable id-based copy for template reference
                        try {
                            Path idStable = emojiDir.resolve(id + "." + ext);
                            Files.write(idStable, bytes); // replace or create
                        } catch (IOException ioe) {
                            // ignore
                        }
                        // Write new path: emoji_{id}_{yyyyMMdd}.{ext}
                        try {
                            Path newNaming = emojiDir.resolve("emoji_" + id + "_" + today + "." + ext);
                            Files.write(newNaming, bytes); // replace or create
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }
            }

            // --- 2) From reactions (custom emojis) ---
            try {
                List<MessageInfo.ReactionView> rvs = mi.getReactionViews();
                if (rvs != null) {
                    for (MessageInfo.ReactionView rv : rvs) {
                        if (rv == null || !rv.isCustom()) { continue; }
                        String url = rv.getEmojiUrl();
                        String name = rv.getAlt();
                        if (url == null || url.isEmpty()) { continue; }
                        // Extract id and ext from CDN URL: https://cdn.discordapp.com/emojis/{id}.{ext}
                        String id = null;
                        String ext = null;
                        int lastSlash = url.lastIndexOf('/');
                        if (lastSlash >= 0 && lastSlash + 1 < url.length()) {
                            String file = url.substring(lastSlash + 1); // e.g., 123456789012345678.png
                            int dot = file.lastIndexOf('.');
                            if (dot > 0) {
                                id = file.substring(0, dot);
                                ext = file.substring(dot + 1).toLowerCase();
                            }
                        }
                        if (id == null || ext == null) { continue; }
                        String key = id + "." + ext;
                        if (processed.contains(key)) { continue; }
                        processed.add(key);

                        byte[] bytes;
                        try (var in = new URL(url).openStream()) {
                            bytes = in.readAllBytes();
                        } catch (IOException ioe) {
                            // skip this emoji if fetch fails
                            continue;
                        }
                        String safeName = (name == null) ? "emoji" : name;
                        safeName = safeName.replaceAll("[^A-Za-z0-9_-]", "_");
                        Path target = emojiDir.resolve(safeName + "_" + today + "." + ext);
                        if (Files.exists(target)) {
                            try {
                                byte[] existing = Files.readAllBytes(target);
                                if (Arrays.equals(existing, bytes)) {
                                    // same content, no extra name_date copy
                                } else {
                                    int idx = 2;
                                    Path alternativePath;
                                    do {
                                        alternativePath = emojiDir.resolve(safeName + "_" + today + "_" + idx + "." + ext);
                                        idx++;
                                    } while (Files.exists(alternativePath));
                                    Files.write(alternativePath, bytes);
                                }
                            } catch (IOException ioe) {
                                // ignore single emoji failure
                            }
                        } else {
                            try {
                                Files.write(target, bytes);
                            } catch (IOException ioe) {
                                // ignore single emoji failure
                            }
                        }
                        // Also write stable id-based copy
                        try {
                            Path idStable = emojiDir.resolve(id + "." + ext);
                            Files.write(idStable, bytes);
                        } catch (IOException ioe) {
                            // ignore
                        }
                        // Write new path: emoji_{id}_{yyyyMMdd}.{ext}
                        try {
                            Path newNaming = emojiDir.resolve("emoji_" + id + "_" + today + "." + ext);
                            Files.write(newNaming, bytes); // replace or create
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }
            } catch (Throwable ignore) {
                // best-effort
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
            ctx.setVariable("backToParentThreadsHref", String.format(basePrefix() + "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
            ctx.setVariable("backToParentArchiveHref", String.format(basePrefix() + "/archives/%s.html", channel.getParentChannelId().toString()));
        } else {
            ctx.setVariable("backToParentThreadsHref", basePrefix() + "/index.html");
            ctx.setVariable("backToParentArchiveHref", basePrefix() + "/index.html");
        }
        ctx.setVariable("backToTopHref", repoBase() + "/index.html");
        ctx.setVariable("guildIconUrl", resolveGuildIconUrl());
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix());
        String html = templateEngine.process(THREAD_TEMPLATE_NAME, ctx);
        Path out = Path.of(
                appConfig.getOutputPath(),
                "archives",
                channel.getParentChannelId() == null ? "unknown" : channel.getParentChannelId().toString(),
                "threads",
                "t-" + channel.getChannelId().toString() + ".html"
        );
        writeHtml(out, html);
        try {
            if (channel.getParentChannelId() != null) {
                regenerateThreadIndex(channel.getParentChannelId());
                // Also ensure the parent channel's archive list page exists/updated
                regenerateChannelArchives(channel.getParentChannelId());
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

    private String repoBase() {
        try {
            String name = appConfig.getGithubRepoName();
            if (name == null) { return ""; }
            name = name.trim();
            while (name.startsWith("/")) { 
                name = name.substring(1); 
            }
            while (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1); 
            }
            return name;
        } catch (Exception ignore) {
            return "";
        }
    }

    private String basePrefix() {
        String base = repoBase();
        return (base == null || base.isEmpty()) ? "" : "/" + base;
    }

    private String normalizeHref(String href) {
        if (href == null) { return ""; }
        String val = href;
        String base = repoBase();
        if (base != null && !base.isEmpty()) {
            String pref = "/" + base + "/";
            if (val.startsWith(pref)) {
                val = val.substring(pref.length());
            }
        }
        // also strip leading ./ if any
        if (val.startsWith("./")) {
            val = val.substring(2);
        }
        return val;
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
            String norm = key == null ? "" : key;
            // Allow only archive-like links:
            // - archives/... (top page and channel archive pages)
            // - ../ or ../../ ... (daily index page relative links)
            boolean allowed = norm.startsWith("archives/") || norm.startsWith("../");
            if (!allowed) {
                continue;
            }
            if (!byHref.containsKey(key)) {
                byHref.put(key, l);
            }
        }
        return new ArrayList<>(byHref.values());
    }
}
