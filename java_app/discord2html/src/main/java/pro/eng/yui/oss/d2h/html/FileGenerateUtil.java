package pro.eng.yui.oss.d2h.html;

import org.jetbrains.annotations.Contract;
import pro.eng.yui.oss.d2h.db.field.CategoryId;
import pro.eng.yui.oss.d2h.db.field.CategoryName;
import pro.eng.yui.oss.d2h.github.GitConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class FileGenerateUtil {

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
    public static void writeIfChanged(Path target, String newContent) throws IOException {
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
}
