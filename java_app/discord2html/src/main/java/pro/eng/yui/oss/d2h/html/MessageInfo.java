package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageEmbed;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessageInfo {

    public static class ReactionView {
        private final boolean custom;
        private final String emojiText; // for unicode
        private final String emojiUrl;  // for custom (CDN fallback)
        private final String emojiLocalPath; // relative path under gh_pages, e.g., archives/emoji/{id}.{ext}
        private final String alt;       // name
        private final int count;
        public ReactionView(boolean custom, String emojiText, String emojiUrl, String emojiLocalPath, String alt, int count) {
            this.custom = custom;
            this.emojiText = emojiText;
            this.emojiUrl = emojiUrl;
            this.emojiLocalPath = emojiLocalPath;
            this.alt = alt;
            this.count = count;
        }
        public boolean isCustom() { return custom; }
        public String getEmojiText() { return emojiText; }
        public String getEmojiUrl() { return emojiUrl; }
        public String getEmojiLocalPath() { return emojiLocalPath; }
        public String getAlt() { return alt; }
        public int getCount() { return count; }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat DATE8 = new SimpleDateFormat("yyyyMMdd");
    static{
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        DATE8.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }
    
    private final String contentRaw;
    public String getContentRaw() {
        return this.contentRaw;
    }
    /**
     * HTML-renderable content with https:// links converted to <a href="url">url</a>(label) form.
     * - Markdown [label](https://url) becomes <a href="url">url</a>(label)
     * - Plain https://url becomes <a href="url">url</a>
     * Non-link parts are HTML-escaped for safety.
     */
    public String getContentHtml() {
        return toHtmlWithLinks(this.contentRaw);
    }
    
    private final Users userInfo;
    public Users getUserInfo(){
        return this.userInfo;
    }
    
    private final MessageUserInfo messageUserInfo;
    public MessageUserInfo getMessageUserInfo() {
        return this.messageUserInfo;
    }
    public String getAvatarUrl(){
        return this.messageUserInfo.getAvatarUrl();
    }
    public String getUsername() {
        return this.messageUserInfo.getUsername();
    }
    
    private final String createdTimestamp;
    public String getCreatedTimestamp(){
        return this.createdTimestamp;
    }

    // Scope key for anonymization (e.g., guild-date-cycle). Nullable for backward compatibility.
    private final String anonymizeScopeKey;
    public String getAnonymizeScopeKey() {
        return anonymizeScopeKey;
    }
    
    private final List<Message.Attachment> attachments;
    public List<Message.Attachment> getAttachments(){
        return this.attachments;
    }

    private final List<MessageReaction> reactions;
    public List<MessageReaction> getReactions(){
        return this.reactions;
    }

    // Derived views for rendering reactions with custom emoji support
    public List<ReactionView> getReactionViews() {
        List<ReactionView> views = new ArrayList<>();
        if (reactions == null) return views;
        for (MessageReaction r : reactions) {
            try {
                Object emoji = r.getEmoji();
                String name = (String) emoji.getClass().getMethod("getName").invoke(emoji);
                int count = r.getCount();
                // Try detect custom via presence of asCustom and id
                String typeName = null;
                try {
                    Object type = emoji.getClass().getMethod("getType").invoke(emoji);
                    typeName = String.valueOf(type);
                } catch (Throwable ignore) { /* JDA API compatibility */ }
                boolean isCustom = false;
                boolean isAnimated = false;
                String id = null;
                if (typeName != null && typeName.toUpperCase().contains("CUSTOM")) {
                    isCustom = true;
                } else {
                    // Fallback: try asCustom() reflectively
                    try {
                        Object custom = emoji.getClass().getMethod("asCustom").invoke(emoji);
                        if (custom != null) {
                            isCustom = true;
                            emoji = custom;
                        }
                    } catch (Throwable ignore) { /* not custom */ }
                }
                if (isCustom) {
                    try {
                        Object animated = emoji.getClass().getMethod("isAnimated").invoke(emoji);
                        isAnimated = (animated instanceof Boolean) ? (Boolean)animated : false;
                    } catch (Throwable ignore) { }
                    try {
                        Object idObj = null;
                        try { idObj = emoji.getClass().getMethod("getId").invoke(emoji); } catch (Throwable ignore) { }
                        if (idObj == null) {
                            Object idLong = emoji.getClass().getMethod("getIdLong").invoke(emoji);
                            id = String.valueOf(idLong);
                        } else {
                            id = String.valueOf(idObj);
                        }
                    } catch (Throwable ignore) { }
                    String ext = isAnimated ? "gif" : "png";
                    String url = (id != null) ? ("https://cdn.discordapp.com/emojis/" + id + "." + ext) : null;
                    String localPath = (id != null) ? ("archives/emoji/emoji_" + id + "_" + DATE8.format(new Date()) + "." + ext) : null;
                    views.add(new ReactionView(true, null, url, localPath, name, count));
                } else {
                    views.add(new ReactionView(false, name, null, null, name, count));
                }
            } catch (Throwable ignore) {
                // Last resort: show name and count
                try {
                    views.add(new ReactionView(false, r.getEmoji().getName(), null, null, r.getEmoji().getName(), r.getCount()));
                } catch (Throwable e2) {
                    // ignore
                }
            }
        }
        return views;
    }
    
    /** nullable */
    private final String refOriginMessageContent;
    public String getRefOriginMessageContent(){
        return this.refOriginMessageContent;
    }
    
    public MessageInfo(Message msg, Users authorInfo){
        this.createdTimestamp = DATE_FORMAT.format(Date.from(msg.getTimeCreated().toInstant()));
        this.userInfo = authorInfo;
        this.anonymizeScopeKey = null;
        this.messageUserInfo = AnonymizationUtil.anonymizeUser(authorInfo);
        this.contentRaw = extractContentIncludingEmbeds(msg);
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            String content = extractContentIncludingEmbeds(msg.getReferencedMessage());
            this.refOriginMessageContent = content.length() > 30 ? content.substring(0, 30) : content;
        }
    }

    public MessageInfo(Message msg, Users authorInfo, String anonymizeScopeKey){
        this.createdTimestamp = DATE_FORMAT.format(Date.from(msg.getTimeCreated().toInstant()));
        this.userInfo = authorInfo;
        this.anonymizeScopeKey = anonymizeScopeKey;
        this.messageUserInfo = (anonymizeScopeKey == null)
                ? AnonymizationUtil.anonymizeUser(authorInfo)
                : AnonymizationUtil.anonymizeUser(authorInfo, anonymizeScopeKey);
        this.contentRaw = extractContentIncludingEmbeds(msg);
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            String content = extractContentIncludingEmbeds(msg.getReferencedMessage());
            this.refOriginMessageContent = content.length() > 30 ? content.substring(0, 30) : content;
        }
    }
    
    public static String toHtmlWithLinks(String content) {
        String escaped = htmlEscape(content == null ? "" : content);
        // 1) Markdown-style: [label](https://url)
        escaped = escaped.replaceAll("\\[([^\\]]+)\\]\\((https://[^)\\s]+)\\)",
                "<a href=\"$2\">$2</a>($1)");
        // 2) Plain https://... (stop before whitespace, <, ), or an HTML entity starting with & that is NOT &amp;). Allow &amp; within URLs for query params.
        escaped = escaped.replaceAll("(?<![\\\"'>])https://[^\\s<)]+?(?=(?:&(?!amp;))|\\s|<|\\)|$)",
                "<a href=\"$0\">$0</a>");
        // 3) Custom Discord emoji tokens (escaped) -> <img> tags
        // Animated: &lt;a:NAME:ID&gt; -> .gif
        escaped = escaped.replaceAll(
                "&lt;a:([A-Za-z0-9_~\\-]+):(\\d+)&gt;",
                "<img class='emoji' src='/Discord2Html/archives/emoji/$2.gif' alt='$1' onerror='this.onerror=null;this.src=\"https://cdn.discordapp.com/emojis/$2.gif\"' />");
        // Static: &lt;:NAME:ID&gt; -> .png
        escaped = escaped.replaceAll(
                "&lt;:([A-Za-z0-9_~\\-]+):(\\d+)&gt;",
                "<img class='emoji' src='/Discord2Html/archives/emoji/$2.png' alt='$1' onerror='this.onerror=null;this.src=\"https://cdn.discordapp.com/emojis/$2.png\"' />");
        // 4) Convert newline characters to <br> so original message line breaks render on HTML
        escaped = escaped.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>");
        return escaped;
    }
    
    public static String htmlEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String extractContentIncludingEmbeds(Message msg) {
        String content = msg.getContentRaw();
        if (content == null || content.trim().isEmpty()) {
            List<MessageEmbed> embeds = msg.getEmbeds();
            if (embeds != null && !embeds.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (MessageEmbed e : embeds) {
                    if (e.getTitle() != null && !e.getTitle().isEmpty()) {
                        sb.append(e.getTitle()).append("\n");
                    }
                    if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                        sb.append(e.getDescription()).append("\n");
                    }
                    List<MessageEmbed.Field> fields = e.getFields();
                    if (fields != null) {
                        for (MessageEmbed.Field f : fields) {
                            if (f == null) continue;
                            boolean hasName = f.getName() != null && !f.getName().isEmpty();
                            boolean hasValue = f.getValue() != null && !f.getValue().isEmpty();
                            if (hasName || hasValue) {
                                if (hasName) sb.append(f.getName());
                                if (hasName && hasValue) sb.append(": ");
                                if (hasValue) sb.append(f.getValue());
                                sb.append("\n");
                            }
                        }
                    }
                    if (e.getFooter() != null && e.getFooter().getText() != null && !e.getFooter().getText().isEmpty()) {
                        sb.append(e.getFooter().getText()).append("\n");
                    }
                    if (e.getAuthor() != null && e.getAuthor().getName() != null && !e.getAuthor().getName().isEmpty()) {
                        sb.append(e.getAuthor().getName()).append("\n");
                    }
                    if (e.getUrl() != null && !e.getUrl().isEmpty()) {
                        sb.append(e.getUrl()).append("\n");
                    }
                }
                content = sb.toString().trim();
            }
        }
        if (content == null) content = "";
        return content;
    }
}
