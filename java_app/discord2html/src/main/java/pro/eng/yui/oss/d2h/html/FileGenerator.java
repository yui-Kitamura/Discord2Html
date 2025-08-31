package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.github.GitConfig;
import pro.eng.yui.oss.d2h.github.GitUtil;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.dao.AnonStatsDAO;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FileGenerator {


    private static final String TEMPLATE_NAME = "message";
    private static final String THREAD_TEMPLATE_NAME = "thread_message";

    private final ApplicationConfig appConfig;
    private final GitConfig gitConfig;
    private final TemplateEngine templateEngine;
    private final GitUtil gitUtil;
    private final GuildsDAO guildsDao;
    private final UsersDAO usersDao;
    private final AnonStatsDAO anonStatsDao;
    private final ChannelsDAO channelsDao;
    private final DiscordJdaProvider jdaProvider;
    private final IndexGenerator indexGenerator;
    private final String botVersion;

    /** 現在実行対象としているguildの情報 */
    private GuildId lastGuildId = null;

    public FileGenerator(ApplicationConfig config, Secrets secrets, GitConfig gitConfig,
                         GitUtil gitUtil, GuildsDAO guildsDao,
                         UsersDAO usersDao, AnonStatsDAO anonStatsDao,
                         ChannelsDAO channelsDao, DiscordJdaProvider jdaProvider,
                         TemplateEngine templateEngine, IndexGenerator indexGenerator) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.gitUtil = gitUtil;
        this.guildsDao = guildsDao;
        this.usersDao = usersDao;
        this.anonStatsDao = anonStatsDao;
        this.channelsDao = channelsDao;
        this.jdaProvider = jdaProvider;
        this.indexGenerator = indexGenerator;
        this.botVersion = secrets.getBotVersion();
        this.gitConfig = gitConfig;
    }

    /**
     * ファイル生成の入口となるインタフェース
     */
    public Path generate(
            ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end,
            int seq
    ) {
        AnonymizationUtil.clearCache();
        
        // Sync local repo to the latest before reading/writing outputs
        try {
            gitUtil.ensureRepoInitialized();
            gitUtil.fetch();
            gitUtil.pullRebase();
        } catch (Exception e) {
            // Non-fatal: continue generation even if git operations fail
            System.out.println("[GitSync] Skip or failed: " + e.getMessage());
        }
        
        // Ensure static assets like CSS exist in an output directory
        try {
            ensureStaticAssets();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare static assets", e);
        }
        
        // Archive custom emojis used in messages to gh_pages resources
        try {
            FileGenerateUtil.archiveCustomEmojis(appConfig.getOutputPath(), messages);
        } catch (IOException ioe) {
            // non-fatal: continue even if emoji archiving fails
            System.out.println("[EmojiArchive] failed: " + ioe.getMessage());
        }
        
        // remember guild context for subsequent index generation
        this.lastGuildId = new GuildId(channel);

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
        String runTimestamp = DateTimeUtil.folder().format(Calendar.getInstance(DateTimeUtil.JST).getTime());

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
            final String basePrefix = FileGenerateUtil.repoBaseWithPrefix(gitConfig.getRepo());
            Context context = new Context();
            context.setVariable("channel", channel);
            context.setVariable("messages", segmentMessages);
            context.setVariable("begin", DateTimeUtil.time().format(cur.getTime()));
            context.setVariable("end", DateTimeUtil.time().format(segmentEnd.getTime()));
            context.setVariable("sequence", seq);
            context.setVariable("backToChannelHref", basePrefix+ "/archives/" + channel.getChannelId().toString() + ".html");
            context.setVariable("backToTopHref", basePrefix + "/index.html");
            context.setVariable("guildIconUrl", FileGenerateUtil.resolveGuildIconUrl(jdaProvider.getJda(), lastGuildId));
            context.setVariable("botVersion", botVersion);
            context.setVariable("basePrefix", basePrefix);
            // Add active thread links for this channel at the top
            try {
                List<FileGenerateUtil.Link> activeThreadLinks = getActiveThreadLinks(channel);
                if (activeThreadLinks.size() > 0) {
                    context.setVariable("activeThreads", activeThreadLinks);
                }
            } catch (Exception ignore) {
                // best-effort; ignore failures
            }

            String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

            Path output = appConfig.getOutputPath().resolve(runTimestamp).resolve(channel.getChannelId().toString() + ".html");
            FileGenerateUtil.writeIfChanged(output, htmlContent);

            lastOutput = output;

            // Mark affected date8 for indices only when there were messages
            if (!segmentMessages.isEmpty()) {
                affectedDate8.add(DateTimeUtil.date8().format(segmentEnd.getTime()));
            }

            // Advance to the start of the next day to avoid infinite loop
            Calendar nextDay = (Calendar) cur.clone();
            nextDay.set(Calendar.HOUR_OF_DAY, 0);
            nextDay.set(Calendar.MINUTE, 0);
            nextDay.set(Calendar.SECOND, 0);
            nextDay.set(Calendar.MILLISECOND, 0);
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            cur = nextDay;
        }

        // If the run ends today and the execution time is not midnight (00:00),
        // ensure today's daily archive is regenerated to include 00:00 -> now,
        // even if no new messages were included in this run's segments.
        try {
            Calendar nowJst = Calendar.getInstance(DateTimeUtil.JST);
            String today8 = DateTimeUtil.date8().format(nowJst.getTime());
            String until8 = DateTimeUtil.date8().format(until.getTime());
            boolean isMidnight = until.get(Calendar.HOUR_OF_DAY) == 23
                    && until.get(Calendar.MINUTE) == 59 && until.get(Calendar.SECOND) == 59
                    && until.get(Calendar.MILLISECOND) == 999;
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
                Calendar any = Calendar.getInstance(DateTimeUtil.JST);
                any.set(Integer.parseInt(d8.substring(0,4)),
                        Integer.parseInt(d8.substring(4,6)) - 1,
                        Integer.parseInt(d8.substring(6,8)),
                        0,0,0);
                any.set(Calendar.MILLISECOND, 0);
                indexGenerator.regenerateDailyIndex(lastGuildId, channel.getChannelId(), any);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to regenerate daily index page(s)", e);
        }
        
        // After generating archive page(s), update listing pages by prepending new links without scanning directories
        try {
            // Prepend per-channel archive links for each affected date
            for (String d8 : affectedDate8) {
                indexGenerator.prependChannelArchiveEntry(lastGuildId, channel.getChannelId(), d8);
            }
        } catch (IOException e) {
            // Do not fail the main generation if index regeneration fails; log via RuntimeException to keep visibility
            throw new RuntimeException("Failed to regenerate archives/index pages", e);
        }
        
        return lastOutput;
    }

    // Discover channel IDs that have at least one archived daily file under output/archives/yyyyMMdd/*.html
    private Set<String> discoverArchivedChannelIds() {
        Set<String> ids = new HashSet<>();
        try {
            Path base = appConfig.getOutputPath();
            if (!Files.exists(base)) { return ids; }
            Path archivesRoot = base.resolve("archives");
            if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) { 
                return ids;
            }
            // 1) Collect from daily folders: archives/yyyyMMdd/*.html
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
            // 2) Also collect channel IDs that have per-channel list pages: archives/<channelId>.html
            try (DirectoryStream<Path> rootHtmls = Files.newDirectoryStream(archivesRoot, "*.html")) {
                for (Path p : rootHtmls) {
                    String fileName = p.getFileName().toString();
                    if (fileName.matches("\\d+\\.html")) {
                        ids.add(fileName.substring(0, fileName.length() - 5));
                    }
                }
            } catch (IOException ignore) {
                // best-effort on root html scan
            }
            // 3) Add forum channels that have thread directories: archives/<channelId>/threads/
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(archivesRoot)) {
                for (Path dir : dirs) {
                    if (!Files.isDirectory(dir)) {
                        continue;
                    }
                    String name = dir.getFileName().toString();
                    Path threads = dir.resolve("threads");
                    if (Files.exists(threads) && Files.isDirectory(threads)) {
                        ids.add(name);
                    }
                }
            } catch (IOException ignore) {
                // best-effort on forum channels scan
            }
        } catch (IOException ignore) {
            // best-effort: return what we have
        }
        return ids;
    }
    
    // Helper to ensure a CategoryGroup exists in the map
    private void ensureCategoryGroup(Map<CategoryId, FileGenerateUtil.CategoryGroup> map, Guild guild, CategoryId id, CategoryName name) {
        if (map.get(id) == null) {
            boolean deleted;
            CategoryName resolvedName;
            if (id.getValue() == CategoryId.NO_CATEGORY_ID) {
                // 未分類カテゴリ''
                deleted = false; 
                resolvedName = CategoryName.EMPTY;
            } else {
                boolean live = guild.getCategories().stream().anyMatch(c -> {
                    return new CategoryId(c).equals(id);
                });
                deleted = !live;
                resolvedName = name;
            }
            map.put(id, new FileGenerateUtil.CategoryGroup(id, resolvedName, deleted));
        }
    }

    private List<FileGenerateUtil.CategoryGroup> buildCategoryGroups() {
        if (lastGuildId == null) { return List.of(); }
        try {
            Guild guild = jdaProvider.getJda().getGuildById(lastGuildId.getValue());
            List<Channels> dbChannels = channelsDao.selectAllInGuild(lastGuildId);
            Set<String> archivedIds = discoverArchivedChannelIds();
            // Map categories by id string (or "0" for uncategorized)
            Map<CategoryId, FileGenerateUtil.CategoryGroup> map = new LinkedHashMap<>();
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
                CategoryId catId = ch.getCategoryId() == null ? CategoryId.NO_CATEGORY : ch.getCategoryId();
                CategoryName catName = ch.getCategoryName() == null ? CategoryName.EMPTY : ch.getCategoryName();
                // If DB lacks category info, try to supplement from live JDA
                if (guild != null && catId.getValue() == CategoryId.NO_CATEGORY_ID) {
                    try {
                        GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, ch.getChannelId().getValue());
                        if (gc instanceof ICategorizableChannel cc) {
                            Category parent = cc.getParentCategory();
                            if (parent != null) {
                                catId = new CategoryId(parent);
                                CategoryName liveName = new CategoryName(parent);
                                if (!liveName.getValue().isEmpty()) {
                                    catName = liveName;
                                }
                            }
                        }
                    } catch (Throwable ignore) { /* best-effort */ }
                }
                ensureCategoryGroup(map, guild, catId, catName);
                FileGenerateUtil.CategoryGroup group = map.get(catId);
                String label = ch.getChannelName().getValue();
                if(group.isDeleted()) {
                    label += " "+CategoryName.SUFFIX_DELETED;
                }
                String href = "archives/" + chId + ".html";
                group.getChannels().add(new FileGenerateUtil.Link(href, label));
            }
            // Order groups: live categories in guild order, then non-live (deleted) categories by name
            List<FileGenerateUtil.CategoryGroup> groups = new ArrayList<>();
            for (CategoryId id : liveOrder) {
                FileGenerateUtil.CategoryGroup g = map.get(id);
                if (g != null) { groups.add(g); }
            }
            // Ensure uncategorized ("0") comes after live categories but before deleted categories
            if (map.containsKey(CategoryId.NO_CATEGORY)) {
                boolean included = groups.stream().anyMatch(g -> g.getId().equals(CategoryId.NO_CATEGORY));
                if (!included) { groups.add(map.get(CategoryId.NO_CATEGORY)); }
            }
            // Add remaining deleted categories (not live and not uncategorized) at the end sorted by name
            List<FileGenerateUtil.CategoryGroup> deletedGroups = map.values().stream()
                    .filter(g -> !liveOrder.contains(g.getId()) && g.getId().getValue()!= 0)
                    .sorted(Comparator.comparing(g -> g.getName().getValue()))
                    .toList();
            groups.addAll(deletedGroups);
            // Within each group, sort: existing channels first (no "(削除済み)" suffix), then deleted
            for (FileGenerateUtil.CategoryGroup g : groups) {
                List<FileGenerateUtil.Link> live = new ArrayList<>();
                List<FileGenerateUtil.Link> gone = new ArrayList<>();
                for (FileGenerateUtil.Link l : g.getChannels()) {
                    if (l.getLabel() != null && l.getLabel().endsWith(CategoryName.SUFFIX_DELETED)) {
                        gone.add(l);
                    } else {
                        live.add(l);
                    }
                }
                live.sort(Comparator.comparing(FileGenerateUtil.Link::getLabel));
                gone.sort(Comparator.comparing(FileGenerateUtil.Link::getLabel));
                g.getChannels().clear();
                g.getChannels().addAll(live);
                g.getChannels().addAll(gone);
            }
            return groups;
        } catch (Throwable ignore) {
            return List.of();
        }
    }

    public void regenerateHelpPage() throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) {
            return;
        }
        Path help = base.resolve("help.html");
        Context ctx = new Context();
        ctx.setVariable("guildIconUrl", FileGenerateUtil.resolveGuildIconUrl(jdaProvider.getJda(), lastGuildId));
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("help", ctx);
        FileGenerateUtil.writeIfChanged(help, page);
    }

    private List<FileGenerateUtil.Link> getActiveThreadLinks(@NotNull ChannelInfo channel) {
        try {
            JDA jda = jdaProvider.getJda();
            GuildChannel raw = jda.getChannelById(GuildChannel.class, channel.getChannelId().getValue());
            if (raw == null || !(raw instanceof IThreadContainer container)) { return List.of(); }
            List<ThreadChannel> threads = container.getThreadChannels();
            List<FileGenerateUtil.Link> links = new ArrayList<>();
            for (ThreadChannel t : threads) {
                if (!t.isArchived()) {
                    String href = FileGenerateUtil.repoBaseWithPrefix(gitConfig.getRepo()) + "/archives/" + channel.getChannelId() + "/threads/t-" + t.getId() + ".html";
                    String label = t.getName();
                    links.add(new FileGenerateUtil.Link(href, label, null, null, label, t.getId()));
                }
            }
            // sort by id(t-href)
            links.sort(Comparator.comparing(FileGenerateUtil.Link::getHref));
            return links;
        } catch (Throwable e) {
            return List.of();
        }
    }

    private List<MessageInfo> filterMessagesByRange(List<MessageInfo> messages, Calendar start, Calendar end) {
        List<MessageInfo> result = new ArrayList<>();
        for (MessageInfo m : messages) {
            try {
                Date d = DateTimeUtil.time().parse(m.getCreatedTimestamp());
                if (!d.before(start.getTime()) && !d.after(end.getTime())) {
                    result.add(m);
                }
            } catch (Exception ignore) {
                // Skip messages with unparsable timestamps
            }
        }
        return result;
    }

    /**
     * Ensure required static assets (like CSS) are present under the output root for GitHub Pages/local viewing.
     */
    private void ensureStaticAssets() throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        // Copy classpath:/static/css/style.css -> {output}/css/style.css
        Path cssDir = base.resolve("css");
        Files.createDirectories(cssDir);
        Path target = cssDir.resolve("style.css");
        byte[] data = readClasspathResource("/static/css/style.css");
        if (data != null) {
            FileGenerateUtil.writeIfChanged(target, new String(data, StandardCharsets.UTF_8));
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

    private Path generateThreadArchive(ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end, int seq) {
        final String basePrefix = FileGenerateUtil.repoBaseWithPrefix(gitConfig.getRepo());
        Context ctx = new Context();
        ctx.setVariable("channel", channel);
        ctx.setVariable("messages", messages);
        ctx.setVariable("begin", DateTimeUtil.time().format(begin.getTime()));
        ctx.setVariable("end", DateTimeUtil.time().format(end.getTime()));
        // Provide machine-readable epoch millis to template
        try {
            ctx.setVariable("beginEpochMillis", begin.getTimeInMillis());
            ctx.setVariable("endEpochMillis", end.getTimeInMillis());
        } catch (Exception ignore) { /* best-effort */ }
        ctx.setVariable("sequence", seq);
        if (channel.getParentChannelName() != null) {
            // Links for thread page navigation
            ctx.setVariable("backToParentThreadsHref", String.format(basePrefix + "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
            ctx.setVariable("backToParentArchiveHref", String.format(basePrefix + "/archives/%s.html", channel.getParentChannelId().toString()));
        } else {
            ctx.setVariable("backToParentThreadsHref", basePrefix + "/index.html");
            ctx.setVariable("backToParentArchiveHref", basePrefix + "/index.html");
        }
        ctx.setVariable("backToTopHref", basePrefix + "/index.html");
        ctx.setVariable("guildIconUrl", FileGenerateUtil.resolveGuildIconUrl(jdaProvider.getJda(), lastGuildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix);
        // Standardized nav variables
        ctx.setVariable("isThread", true);
        if (channel.getParentChannelId() != null) {
            ctx.setVariable("threadIndexHref", String.format(basePrefix+ "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
        }
        String html = templateEngine.process(THREAD_TEMPLATE_NAME, ctx);
        Path outPath = appConfig.getOutputPath()
                .resolve("archives")
                .resolve(channel.getParentChannelId() == null ? "unknown" : channel.getParentChannelId().toString())
                .resolve("threads")
                .resolve("t-" + channel.getChannelId().toString() + ".html");
        FileGenerateUtil.writeIfChanged(outPath, html);
        try {
            if (channel.getParentChannelId() != null) {
                indexGenerator.regenerateThreadIndex(lastGuildId, channel.getParentChannelId());
                // Also ensure the parent channel's archive list page exists/updated
                indexGenerator.regenerateChannelArchives(lastGuildId, channel.getParentChannelId());
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to regenerate thread index", ioe);
        }
        return outPath;
    }
}
