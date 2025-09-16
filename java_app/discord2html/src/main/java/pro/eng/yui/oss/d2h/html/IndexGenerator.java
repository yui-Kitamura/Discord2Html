package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.Secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class IndexGenerator {
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final GuildsDAO guildsDao;
    private final ChannelsDAO channelsDao;
    private final DiscordJdaProvider jdaProvider;
    private final String botVersion;
    private final FileGenerateUtil fileUtil;

    public IndexGenerator(ApplicationConfig config, Secrets secrets, TemplateEngine templateEngine,
                          GuildsDAO guildsDao, ChannelsDAO channelsDao,
                          DiscordJdaProvider jdaProvider, FileGenerateUtil fileUtil) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.guildsDao = guildsDao;
        this.channelsDao = channelsDao;
        this.jdaProvider = jdaProvider;
        this.botVersion = secrets.getBotVersion();
        this.fileUtil = fileUtil;
    }

    // ===== Top index generation =====
    public void regenerateTopIndex(GuildId targetGuild) throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) { return; }
        Path archivesRoot = base.resolve("archives");
        if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) { return; }
        Set<ChannelId> channelIds = discoverArchivedChannelIds();
        for (ChannelId id : channelIds) {
            try { 
                regenerateChannelIndex(targetGuild, id);
            } catch (Throwable ignore) { /* run next channel */ }
        }
        List<FileGenerateUtil.Link> items = new ArrayList<>(channelIds.stream()
                .sorted(Comparator.comparing(ChannelId::toString))
                .map(id -> {
                    String label = id.toString();
                    String href = "archives/" + id + ".html";
                    try {
                        GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, id.getValue());
                        if (gc != null && !gc.getName().isEmpty()) { label = gc.getName(); }
                    } catch (Throwable ignore) { }
                    if (isForumLike(id)) { href = "archives/" + id + "/threads/index.html"; }
                    return new FileGenerateUtil.Link(href, label);
                }).toList());
        Path index = base.resolve("index.html");
        Context ctx = new Context();
        if (items.isEmpty() && Files.exists(index)) { return; }
        List<FileGenerateUtil.CategoryGroup> groups = buildCategoryGroups(targetGuild);
        if (!groups.isEmpty()) {
            ctx.setVariable("categories", groups);
        } else {
            List<FileGenerateUtil.Link> merged = mergeLinksPreserveAll(items, readExistingLinks(index));
            FileGenerateUtil.CategoryGroup synthetic = new FileGenerateUtil.CategoryGroup(CategoryId.NO_CATEGORY, CategoryName.EMPTY, false);
            for (FileGenerateUtil.Link l : merged) { synthetic.getChannels().add(l); }
            List<FileGenerateUtil.CategoryGroup> synthList = new ArrayList<>();
            synthList.add(synthetic);
            ctx.setVariable("categories", synthList);
        }
        String guildName = "Discord";
        try {
            Guilds g = guildsDao.selectGuildInfo(targetGuild);
            if (g != null && g.getGuildName() != null) { guildName = g.getGuildName().getValue(); }
        } catch (Exception ignore) { }
        ctx.setVariable("guildName", guildName);
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(targetGuild));
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("top", ctx);
        fileUtil.writeIfChanged(index, page);
    }

    // ===== Channel list pages =====
    public void prependChannelArchiveEntry(GuildId guildId, ChannelId channelId, String date8) throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) { return; }
        Path archivesRoot = base.resolve("archives");
        Files.createDirectories(archivesRoot);
        String href = fileUtil.repoBaseWithPrefix() + "/archives/" + date8 + "/" + channelId + ".html";
        String displayName = channelId.toString();
        try {
            GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, channelId.getValue());
            if (gc != null && !gc.getName().isEmpty()) { displayName = gc.getName(); }
        } catch (Throwable ignore) {}
        String displayTs = date8;
        try {
            displayTs = DateTimeUtil.dateOnly().format(DateTimeUtil.folder().parse(date8 + DateTimeUtil.endOfDay));
            
            String today8 = DateTimeUtil.date8().format(Calendar.getInstance().getTime());
            Path file = archivesRoot.resolve(date8).resolve(channelId.toString() + ".html");
            if (today8.equals(date8) && Files.exists(file)) {
                displayTs = DateTimeUtil.time().format(new Date(Files.getLastModifiedTime(file).toMillis()));
            }
        } catch (Exception ignore) { }
        List<FileGenerateUtil.Link> items = new ArrayList<>();
        items.add(new FileGenerateUtil.Link(href, displayTs, "d-"+date8));
        Path channelArchive = archivesRoot.resolve(channelId.toString() + ".html");
        List<FileGenerateUtil.Link> merged = mergeLinksPreserveAll(items, readExistingLinks(channelArchive));
        final String threadIndexNorm = "archives/" + channelId + "/threads/index.html";
        merged = merged.stream()
                .filter(l -> {
                    return !fileUtil.normalizeHref(l.getHref()).endsWith(threadIndexNorm);
                })
                .collect(Collectors.toList());
        Context ctx = new Context();
        ctx.setVariable("title", displayName + " のアーカイブ一覧");
        ctx.setVariable("description", "以下のアーカイブから選択してください:");
        ctx.setVariable("items", merged);
        ctx.setVariable("threadIndexHref", fileUtil.repoBaseWithPrefix() + "/" + threadIndexNorm);
        ctx.setVariable("backToTopHref", fileUtil.repoBaseWithPrefix() + "/index.html");
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("hideDateSearch", false);
        ctx.setVariable("rootHref", fileUtil.repoBaseWithPrefix() + "/index.html");
        ctx.setVariable("isThread", false);
        String page = templateEngine.process("list", ctx);
        fileUtil.writeIfChanged(channelArchive, page);
        try { regenerateThreadIndex(guildId, channelId); } catch (IOException ignore) { }
    }

    public void regenerateChannelIndex(GuildId guildId, ChannelId channelId) throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) { return; }
        Path archivesRoot = base.resolve("archives");
        if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) { return; }
        List<Path> dateDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archivesRoot)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (Files.isDirectory(p) && name.matches("\\d{8}")) { dateDirs.add(p); }
            }
        }
        dateDirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        List<FileGenerateUtil.Link> items = new ArrayList<>();
        String displayName = channelId.toString();
        try {
            GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, channelId.getValue());
            if (gc != null && !gc.getName().isEmpty()) { displayName = gc.getName(); }
        } catch (Throwable ignore) {}
        for (Path dateDir : dateDirs) {
            String date8 = dateDir.getFileName().toString();
            Path file = dateDir.resolve(channelId.toString() + ".html");
            if (Files.exists(file)) {
                String href = fileUtil.repoBaseWithPrefix() + "/archives/" + date8 + "/" + channelId + ".html";
                String displayTs;
                try {
                    String today8 = DateTimeUtil.date8().format(Calendar.getInstance().getTime());
                    if (today8.equals(date8)) {
                        displayTs = DateTimeUtil.time().format(new Date(Files.getLastModifiedTime(file).toMillis()));
                    } else {
                        Date endOfDay = DateTimeUtil.folder().parse(date8 + DateTimeUtil.endOfDay);
                        displayTs = DateTimeUtil.dateOnly().format(endOfDay);
                    }
                } catch (Exception e) { e.printStackTrace(); displayTs = date8; }
                items.add(new FileGenerateUtil.Link(href, displayTs, "d-"+date8));
            }
        }
        Path channelArchive = archivesRoot.resolve(channelId.toString() + ".html");
        Files.createDirectories(archivesRoot);
        List<FileGenerateUtil.Link> merged = mergeLinksPreserveAll(items, readExistingLinks(channelArchive));
        final String threadIndexNorm = "archives/" + channelId + "/threads/index.html";
        merged = merged.stream()
                .filter(l -> {
                    return !fileUtil.normalizeHref(l.getHref()).endsWith(threadIndexNorm);
                })
                .collect(Collectors.toList());
        Context ctx = new Context();
        ctx.setVariable("title", displayName + " のアーカイブ一覧");
        ctx.setVariable("description", "以下のアーカイブから選択してください:");
        ctx.setVariable("items", merged);
        ctx.setVariable("threadIndexHref", fileUtil.repoBaseWithPrefix() + "/archives/" + channelId + "/threads/index.html");
        ctx.setVariable("backToTopHref", fileUtil.repoBaseWithPrefix() + "/index.html");
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("hideDateSearch", false);
        ctx.setVariable("rootHref", fileUtil.repoBaseWithPrefix() + "/index.html");
        ctx.setVariable("isThread", false);
        String page = templateEngine.process("list", ctx);
        try { regenerateThreadIndex(guildId, channelId); } catch (IOException ignore) { }
        fileUtil.writeIfChanged(channelArchive, page);
    }

    public void regenerateThreadIndex(GuildId guildId, ChannelId parentChannelId) throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) { return; }
        Path parentThreadsDir = base.resolve("archives").resolve(parentChannelId.toString()).resolve("threads");
        Files.createDirectories(parentThreadsDir);
        class ThreadEntry { 
            String href; String label; boolean active; long lastModified; long created; 
        }
        List<ThreadEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentThreadsDir, "*.html")) {
            for (Path p : stream) {
                String file = p.getFileName().toString();
                String label = file.replaceFirst("\\.html$", "");
                if (label.equals("index")) { continue; }
                String href = fileUtil.repoBaseWithPrefix() + "/archives/" + parentChannelId + "/threads/" + file;
                String idPart = label;
                if (label.startsWith("t-")) { idPart = label.substring(2); }
                ThreadChannel thread = null; boolean active = false; long created = 0L;
                try { thread = jdaProvider.getJda().getThreadChannelById(idPart); } catch (Throwable ignore) { }
                String threadName = label;
                if (thread != null) {
                    threadName = thread.getName();
                    created = thread.getTimeCreated().toInstant().toEpochMilli();
                    active = !(thread.isArchived() || thread.isLocked());
                }
                long endEpoch;
                try {
                    String html = Files.readString(p, StandardCharsets.UTF_8);
                    Pattern mp = Pattern.compile("<meta\\s+[^>]*name=\\\"d2h-thread-end-epoch\\\"[^>]*content=\\\"(\\d+)\\\"", Pattern.CASE_INSENSITIVE);
                    Matcher mm = mp.matcher(html);
                    if (mm.find()) { endEpoch = Long.parseLong(mm.group(1)); } else { endEpoch = 0L; }
                } catch (Exception unexpected) { endEpoch = 0L; }
                String updatedLabel = threadName;
                if (endEpoch > 0L) { updatedLabel = threadName + " (" + DateTimeUtil.time().format(new Date(endEpoch)) + ")"; }
                ThreadEntry te = new ThreadEntry();
                te.href = href; te.label = updatedLabel; te.active = active; te.lastModified = endEpoch; te.created = created;
                entries.add(te);
            }
        }
        entries.sort(Comparator
                .comparing((ThreadEntry e) -> e.active).reversed()
                .thenComparingLong(e -> e.lastModified).reversed()
                .thenComparingLong(e -> e.created).reversed());
        List<FileGenerateUtil.Link> items = new ArrayList<>();
        for (ThreadEntry e : entries) {
            try {
                String threadId = null;
                int ti = e.href.lastIndexOf("/t-");
                if (ti >= 0) {
                    int dot = e.href.lastIndexOf('.');
                    if (dot > ti) { threadId = e.href.substring(ti + 3, dot); }
                }
                items.add(new FileGenerateUtil.Link(e.href, e.label, null,
                        (e.lastModified > 0 ? e.lastModified : null),
                        (e.label != null ? e.label.replaceFirst("\\s*\\(.*$", "").trim() : null),
                        threadId));
            } catch (Throwable ex) {
                items.add(new FileGenerateUtil.Link(e.href, e.label));
            }
        }
        Files.createDirectories(parentThreadsDir);
        Path index = parentThreadsDir.resolve("index.html");
        String parentDisplayName = parentChannelId.toString();
        try {
            GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, parentChannelId.getValue());
            if (gc != null && !gc.getName().isEmpty()) { 
                parentDisplayName = gc.getName(); 
            }
        } catch (Throwable ignore) {}
        Context ctx = new Context();
        ctx.setVariable("title", parentDisplayName + " のスレッド一覧");
        ctx.setVariable("description", "このチャンネルに属するスレッドのアーカイブ一覧");
        ctx.setVariable("items", items);
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("hideDateSearch", true);
        ctx.setVariable("backToTopHref", fileUtil.repoBaseWithPrefix() + "/index.html");
        if (!isForumLike(parentChannelId)) {
            ctx.setVariable("backToChannelArchivesHref", fileUtil.repoBaseWithPrefix() + "/archives/" + parentChannelId + ".html");
        }
        ctx.setVariable("isThread", false);
        String page = templateEngine.process("list", ctx);
        fileUtil.writeIfChanged(index, page);
    }

    // ===== Helpers =====
    private List<FileGenerateUtil.Link> readExistingLinks(Path file) {
        try {
            if (Files.exists(file)) {
                String html = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = FileGenerateUtil.Link.A_TAG_PATTERN.matcher(html);
                List<FileGenerateUtil.Link> links = new ArrayList<>();
                while (m.find()) {
                    String href = m.group(1);
                    String label = m.group(2);
                    links.add(new FileGenerateUtil.Link(href, label));
                }
                return links;
            }
        } catch (IOException ignore) { }
        return List.of();
    }

    private boolean isForumLike(ChannelId id) {
        try {
            ForumChannel fc = jdaProvider.getJda().getForumChannelById(id.getValue());
            if (fc != null) { return true; }
        } catch (Throwable ignore) { }
        try {
            Path base = appConfig.getOutputPath();
            Path threads = base.resolve("archives").resolve(id.toString()).resolve("threads");
            if (Files.exists(threads) && Files.isDirectory(threads)) { return true; }
        } catch (Throwable ignore) { }
        return false;
    }

    private Set<ChannelId> discoverArchivedChannelIds() {
        Set<ChannelId> ids = new HashSet<>();
        try {
            Path base = appConfig.getOutputPath();
            if (!Files.exists(base)) { return ids; }
            Path archivesRoot = base.resolve("archives");
            if (!Files.exists(archivesRoot) || !Files.isDirectory(archivesRoot)) { return ids; }
            try (DirectoryStream<Path> days = Files.newDirectoryStream(archivesRoot)) {
                for (Path dayDir : days) {
                    String name = dayDir.getFileName().toString();
                    if (!Files.isDirectory(dayDir) || !name.matches("\\d{8}")) { continue; }
                    try (DirectoryStream<Path> htmls = Files.newDirectoryStream(dayDir, "*.html")) {
                        for (Path p : htmls) {
                            String fileName = p.getFileName().toString();
                            String idStr = fileName.substring(0, fileName.length() - 5);
                            try { ids.add(new ChannelId(Long.parseLong(idStr))); } catch (Throwable ignore) { }
                        }
                    } catch (IOException ignore) { }
                }
            }
            try (DirectoryStream<Path> rootHtmls = Files.newDirectoryStream(archivesRoot, "*.html")) {
                for (Path p : rootHtmls) {
                    String fileName = p.getFileName().toString();
                    if (fileName.matches("\\d+\\.html")) { 
                        String idStr = fileName.substring(0, fileName.length() - 5);
                        try { ids.add(new ChannelId(Long.parseLong(idStr))); } catch (Throwable ignore) { }
                    }
                }
            } catch (IOException ignore) { }
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(archivesRoot)) {
                for (Path dir : dirs) {
                    if (!Files.isDirectory(dir)) { continue; }
                    String name = dir.getFileName().toString();
                    Path threads = dir.resolve("threads");
                    if (Files.exists(threads) && Files.isDirectory(threads)) { 
                        try { ids.add(new ChannelId(Long.parseLong(name))); } catch (Throwable ignore) { }
                    }
                }
            } catch (IOException ignore) { }
        } catch (IOException ignore) { }
        return ids;
    }

    private void ensureCategoryGroup(Map<CategoryId, FileGenerateUtil.CategoryGroup> map, Guild guild, CategoryId id, CategoryName name) {
        if (map.get(id) == null) {
            boolean deleted;
            CategoryName resolvedName;
            if (id.getValue() == 0) {
                deleted = false;
                resolvedName = new CategoryName("");
            } else {
                boolean live = guild.getCategories().stream().anyMatch(c -> new CategoryId(c).equals(id));
                deleted = !live;
                resolvedName = name;
            }
            map.put(id, new FileGenerateUtil.CategoryGroup(id, resolvedName, deleted));
        }
    }

    private List<FileGenerateUtil.CategoryGroup> buildCategoryGroups(GuildId guildId) {
        if (guildId == null) { return List.of(); }
        try {
            Guild guild = jdaProvider.getJda().getGuildById(guildId.getValue());
            List<Channels> dbChannels = channelsDao.selectAllInGuild(guildId);
            Set<ChannelId> archivedIds = discoverArchivedChannelIds();
            Map<CategoryId, FileGenerateUtil.CategoryGroup> map = new LinkedHashMap<>();
            List<CategoryId> liveOrder = new ArrayList<>();
            if (guild != null) { guild.getCategories().forEach(cat -> liveOrder.add(new CategoryId(cat))); }
            for (Channels ch : dbChannels) {
                ChannelId chId = ch.getChannelId();
                if (!archivedIds.contains(chId)) { continue; }
                CategoryId catId = ch.getCategoryId() == null ? CategoryId.NO_CATEGORY : ch.getCategoryId();
                CategoryName catName = ch.getCategoryName() == null ? CategoryName.EMPTY : ch.getCategoryName();
                if (guild != null && catId.getValue() == 0) {
                    try {
                        GuildChannel gc = jdaProvider.getJda().getChannelById(GuildChannel.class, ch.getChannelId().getValue());
                        if (gc instanceof ICategorizableChannel cc) {
                            Category parent = cc.getParentCategory();
                            if (parent != null) {
                                catId = new CategoryId(parent);
                                CategoryName liveName = new CategoryName(parent);
                                if (!liveName.getValue().isEmpty()) { catName = liveName; }
                            }
                        }
                    } catch (Throwable ignore) { }
                }
                ensureCategoryGroup(map, guild, catId, catName);
                FileGenerateUtil.CategoryGroup group = map.get(catId);
                String label = ch.getChannelName().getValue();
                if (group.isDeleted()) { label += (" " + CategoryName.SUFFIX_DELETED); }
                String href = isForumLike(chId) ? ("archives/" + chId + "/threads/index.html") : ("archives/" + chId + ".html");
                group.getChannels().add(new FileGenerateUtil.Link(href, label));
            }
            List<FileGenerateUtil.CategoryGroup> groups = new ArrayList<>();
            for (CategoryId id : liveOrder) {
                FileGenerateUtil.CategoryGroup g = map.get(id);
                if (g != null) { groups.add(g); }
            }
            if (map.containsKey(CategoryId.NO_CATEGORY)) {
                boolean included = groups.stream().anyMatch(g -> g.getId().equals(CategoryId.NO_CATEGORY));
                if (!included) { groups.add(map.get(CategoryId.NO_CATEGORY)); }
            }
            List<FileGenerateUtil.CategoryGroup> deletedGroups = map.values().stream()
                    .filter(g -> !liveOrder.contains(g.getId()) && g.getId().getValue() != 0)
                    .sorted(Comparator.comparing(g -> g.getName().getValue()))
                    .toList();
            groups.addAll(deletedGroups);
            for (FileGenerateUtil.CategoryGroup g : groups) {
                List<FileGenerateUtil.Link> live = new ArrayList<>();
                List<FileGenerateUtil.Link> gone = new ArrayList<>();
                for (FileGenerateUtil.Link l : g.getChannels()) {
                    if (l.getLabel() != null && l.getLabel().endsWith(CategoryName.SUFFIX_DELETED)) { gone.add(l); }
                    else { live.add(l); }
                }
                live.sort(Comparator.comparing(FileGenerateUtil.Link::getLabel));
                gone.sort(Comparator.comparing(FileGenerateUtil.Link::getLabel));
                g.getChannels().clear();
                g.getChannels().addAll(live);
                g.getChannels().addAll(gone);
            }
            return groups;
        } catch (Throwable ignore) { return List.of(); }
    }

    private List<FileGenerateUtil.Link> mergeLinksPreserveAll(List<FileGenerateUtil.Link> newlyComputed, List<FileGenerateUtil.Link> existing) {
        Map<String, FileGenerateUtil.Link> byHref = new LinkedHashMap<>();
        for (FileGenerateUtil.Link l : newlyComputed) { 
            byHref.put(fileUtil.normalizeHref(l.getHref()), l); 
        }
        for (FileGenerateUtil.Link l : existing) {
            String key = fileUtil.normalizeHref(l.getHref());
            boolean allowed = key.startsWith("archives/") || key.startsWith("../");
            if (!allowed) { continue; }
            if (!byHref.containsKey(key)) { byHref.put(key, l); }
        }
        return new ArrayList<>(byHref.values());
    }
}
