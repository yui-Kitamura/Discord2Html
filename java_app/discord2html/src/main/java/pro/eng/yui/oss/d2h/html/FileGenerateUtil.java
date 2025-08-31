package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Contract;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.field.CategoryId;
import pro.eng.yui.oss.d2h.db.field.CategoryName;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.github.GitConfig;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileGenerateUtil {

    private static final Pattern CUSTOM_EMOJI_PATTERN = Pattern.compile("<(a?):([A-Za-z0-9_~\\-]+):(\\d+)>" );

    public static class Link {
        // --- Utilities to preserve/merge existing list links ---
        public static final Pattern A_TAG_PATTERN = Pattern.compile("<a\\s+[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE);
        private final String href;
        private final String label;
        private final String id; // optional id for anchor
        // Optional data-* attributes for anchor tags
        private final Long dataEpochMillis;
        private final String dataThreadName;
        private final String dataThreadId;
        public Link(String href, String label) {
            this(href, label, null, null, null, null);
        }
        public Link(String href, String label, String id) {
            this(href, label, id, null, null, null);
        }
        public Link(String href, String label, String id, Long dataEpochMillis, String dataThreadName, String dataThreadId) {
            this.href = href;
            this.label = label;
            this.id = id;
            this.dataEpochMillis = dataEpochMillis;
            this.dataThreadName = dataThreadName;
            this.dataThreadId = dataThreadId;
        }
        public String getHref() { return href; }
        public String getLabel() { return label; }
        public String getId() { return id; }
        public Long getDataEpochMillis() { return dataEpochMillis; }
        public String getDataThreadName() { return dataThreadName; }
        public String getDataThreadId() { return dataThreadId; }
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
    
    private FileGenerateUtil() {}

    /**
     * Write content to a file only if it changed. Creates the file when missing.
     */
    public static void writeIfChanged(Path target, String newContent) {
        try {
            String existing = null;
            if (Files.exists(target)) {
                existing = Files.readString(target, StandardCharsets.UTF_8);
            }
            if (existing == null || !existing.equals(newContent)) {
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                try (FileWriter writer = new FileWriter(target.toString())) {
                    writer.write(newContent);
                }
            }
        }catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    @Contract("_ -> !null")
    public static String repoBase(GitConfig.Repo repo) {
        try {
            String name = repo.getName();
            if (name == null) { return ""; }
            name = name.trim();
            while (name.startsWith("/")) { name = name.substring(1); }
            while (name.endsWith("/")) { name = name.substring(0, name.length() - 1); }
            return name;
        } catch (Exception ignore) {
            return "";
        }
    }

    @Contract("_ -> !null")
    public static String repoBaseWithPrefix(GitConfig.Repo repo) {
        return "/" + repoBase(repo);
    }

    
    @Contract("_,_ -> !null")
    public static String normalizeHref(String href, GitConfig.Repo repo) {
        if (href == null) { return ""; }
        String val = href;
        String base = FileGenerateUtil.repoBase(repo);
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

    @Contract("_,null -> null; null,_ -> null")
    public static String resolveGuildIconUrl(JDA jda, GuildId guildId) {
        if (jda == null || guildId == null) { return null; }
        try {
            Guild guild = jda.getGuildById(guildId.getValue());
            if (guild != null && guild.getIconUrl() != null && !guild.getIconUrl().isEmpty()) {
                return guild.getIconUrl();
            }
        } catch (Exception ignore) { }
        return null;
    }

    public static void archiveCustomEmojis(Path outputPath, List<MessageInfo> messages) throws IOException {
        if (messages == null || messages.isEmpty()){ return; }
        Path emojiDir = outputPath.resolve("archives").resolve("emoji");
        Files.createDirectories(emojiDir);
        String today = DateTimeUtil.date8().format(new Date());

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
}
