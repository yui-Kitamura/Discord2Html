package pro.eng.yui.oss.d2h.html;

import com.google.gson.Gson;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.Contract;
import pro.eng.yui.oss.d2h.db.field.ChannelName;
import pro.eng.yui.oss.d2h.db.model.Users;

import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.field.AbstName;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageInfo {

    // Placeholder constants for internal replacements (prefixes are combined with per-instance nonce)
    private static final String D2H_MSG_PLACEHOLDER_PREFIX = "{{D2H_MSG_";
    private static final String D2H_INLINE_U_PREFIX = "{{D2H_INLINE_U_";
    private static final String D2H_INLINE_R_PREFIX = "{{D2H_INLINE_R_";
    private static final String D2H_INLINE_C_PREFIX = "{{D2H_INLINE_C_";
    // Nonce to prevent user-forgeable placeholders
    private final String placeholderNonce = Long.toHexString(Double.doubleToLongBits(Math.random())) + Long.toHexString(System.nanoTime());

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

    private final String contentRaw;
    private final String contentProcessed;
    private final Map<String, String> msgLinkHtmlMap;
    private final Map<String, String> inlineHtmlMap;
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
        String base = (this.contentProcessed != null) ? this.contentProcessed : this.contentRaw;
        String html = toHtmlWithLinks(base);
        // Inject prepared HTML for Discord message links (placeholders)
        if (!msgLinkHtmlMap.isEmpty() && html.isEmpty() == false) {
            for (Map.Entry<String, String> e : msgLinkHtmlMap.entrySet()) {
                html = html.replace(e.getKey(), e.getValue());
            }
        }
        if (!inlineHtmlMap.isEmpty() && html.isEmpty() == false) {
            for (Map.Entry<String, String> e : inlineHtmlMap.entrySet()) {
                html = html.replace(e.getKey(), e.getValue());
            }
        }
        return html;
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

    // Hex color string like #RRGGBB for the display name, derived from the member's highest-colored role.
    // Null if no colored role or not resolvable (e.g., webhook/external).
    private final String nameColor;
    public String getNameColor() {
        return this.nameColor;
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
    /** Derived views for rendering reactions with custom emoji support */
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
                    String localPath = (id != null) ? ("archives/emoji/emoji_" + id + "_" + DateTimeUtil.date8().format(new Date()) + "." + ext) : null;
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

    // Forwarded message support
    private final boolean forwarded;
    private final String forwardedHtml; // prebuilt HTML block for a forwarded source (blockquote)
    public boolean isForwarded() { return this.forwarded; }
    public String getForwardedHtml() { return this.forwardedHtml; }
    
    /** コンストラクタ */
    public MessageInfo(Message msg, Users authorInfo, String anonymizeScopeKey){
        this.msgLinkHtmlMap = new HashMap<>();
        this.inlineHtmlMap = new HashMap<>();
        this.createdTimestamp = DateTimeUtil.time().format(Date.from(msg.getTimeCreated().toInstant()));
        this.userInfo = authorInfo;
        this.anonymizeScopeKey = anonymizeScopeKey;
        this.messageUserInfo = (anonymizeScopeKey == null)
                ? AnonymizationUtil.anonymizeUser(authorInfo)
                : AnonymizationUtil.anonymizeUser(authorInfo, anonymizeScopeKey);
        this.contentRaw = extractContentIncludingEmbeds(msg);
        this.contentProcessed = preprocessArchiveText(msg, this.contentRaw);
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        String colorHex = null;
        try {
            Member m = msg.getMember();
            if (m != null) {
                Color c = m.getColor();
                if (c != null) {
                    colorHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
                }
            }
        } catch (Throwable ignore) { /* fallback leaves null */ }
        this.nameColor = colorHex;
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            String content = extractContentIncludingEmbeds(msg.getReferencedMessage());
            this.refOriginMessageContent = content.length() > 30 ? content.substring(0, 30) : content;
        }
        // Determine forwarded status and build forwarded HTML
        boolean tmpForwarded = false;
        String tmpForwardedHtml = null;
        try {
            MessageReference ref = msg.getMessageReference();
            if (ref != null) {
                tmpForwarded = (ref.getType() == MessageReference.MessageReferenceType.FORWARD);
                tmpForwardedHtml = buildForwardedBlockquoteHtml(msg.getGuild(), ref);
            }
        } catch (NullPointerException ignore) { }
        this.forwarded = tmpForwarded;
        this.forwardedHtml = tmpForwardedHtml;
    }
    
    @Contract("_ -> new")
    public static String toHtmlWithLinks(String content) {
        String escaped = htmlEscape(content == null ? "" : content);
        // 1) Markdown-style: [label](https://url)
        escaped = escaped.replaceAll("\\[([^\\]]+)\\]\\((https://[^)\\s]+)\\)",
                "<a href=\"$2\">$2</a>($1)");
        // 2) Plain https://... (stop before whitespace, <, ), or an HTML entity starting with & that is NOT &amp;). Allow &amp; within URLs for query params.
        escaped = escaped.replaceAll("(?<![\\\"'>])https://[^\\s<)]+?(?=(?:&(?!amp;))|\\s|<|\\)|$)",
                "<a href=\"$0\">$0</a>");
        // 3) Custom Discord emoji tokens (escaped) -> <img> with multi-fallback (archive-day -> id-stable -> CDN)
        {
            Pattern p = Pattern.compile("&lt;(a?):([A-Za-z0-9_~\\-]+):(\\d+)&gt;");
            Matcher m = p.matcher(escaped);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                boolean animated = m.group(1) != null && !m.group(1).isEmpty();
                String name = m.group(2);
                String id = m.group(3);
                String img = FileGenerateUtil.buildEmojiImgHtml(name, id, animated);
                m.appendReplacement(sb, Matcher.quoteReplacement(img));
            }
            m.appendTail(sb);
            escaped = sb.toString();
        }
        // 4) Blockquote: lines starting with '>' become <blockquote>...</blockquote> and consecutive lines are grouped
        {
            String[] lines = escaped.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
            StringBuilder out = new StringBuilder();
            boolean inQuote = false;
            StringBuilder quoteBuf = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.startsWith("&gt;")) { // original '>' escaped to &gt;
                    String body = line.substring(4); // remove '&gt;'
                    if (body.startsWith(" ")) { body = body.substring(1); }
                    if (!inQuote) { inQuote = true; quoteBuf.setLength(0); }
                    if (!quoteBuf.isEmpty()) { quoteBuf.append("<br>"); }
                    quoteBuf.append(body.isEmpty() ? "&nbsp;" : body);
                } else {
                    if (inQuote) {
                        out.append("<blockquote>").append(quoteBuf).append("</blockquote>");
                        inQuote = false;
                    }
                    out.append(line);
                    if (i < lines.length - 1) { out.append("\n"); }
                }
            }
            if (inQuote) {
                out.append("<blockquote>").append(quoteBuf).append("</blockquote>");
            }
            escaped = out.toString();
        }
        // 5) Convert remaining newline characters to <br> so original message line breaks render on HTML
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
        if (content.trim().isEmpty()) {
            List<MessageEmbed> embeds = msg.getEmbeds();
            if (!embeds.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (MessageEmbed e : embeds) {
                    if (e.getTitle() != null && !e.getTitle().isEmpty()) {
                        sb.append(e.getTitle()).append("\n");
                    }
                    if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                        sb.append(e.getDescription()).append("\n");
                    }
                    List<MessageEmbed.Field> fields = e.getFields();
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
        return content;
    }

    /**
     * forward/reply連携を重視した1行監視ログを生成します。
     * 固定項目は前方、可変のテキストは末尾（PREVIEW）に配置されます。
     * 例:
     * TYPE=REPLY | G=123 | C=456 | M=789 | REF_TYPE=REPLY | REF_G=123 | REF_C=456 | REF_M=111 | WEBHOOK=0 | BOT=0 | ATT=1 | EMB=0 | REAC=2 | TS=2025-09-06T12:34:56Z | PREVIEW="Hello world ..."
     */
    public static String formatLinkAuditLine(Message msg) {
        String type = "NORMAL";
        String refType = "NONE";
        String guildId = "-";
        String channelId = "-";
        String messageId = "-";
        String refGuildId = "-";
        String refChannelId = "-";
        String refMessageId = "-";
        String ts = "-";
        int att = 0, emb = 0, reac = 0;
        int webhook = 0, bot = 0;
        String preview = "";

        try {
            messageId = safeStr(() -> msg.getId());
            ts = safeStr(() -> String.valueOf(msg.getTimeCreated()));

            try {
                guildId = safeStr(() -> msg.getGuild().getId());
            } catch (Throwable ignore) { /* DMsなど */ }

            try {
                channelId = safeStr(() -> msg.getChannel().getId());
            } catch (Throwable ignore) { }

            try {
                type = (msg.getMessageReference() != null)
                        ? ((msg.getMessageReference().getType() == MessageReference.MessageReferenceType.FORWARD) ? "FWD" : "REPLY")
                        : "NORMAL";
            } catch (Throwable ignore) { }

            try {
                MessageReference ref = msg.getMessageReference();
                if (ref != null) {
                    try { refType = String.valueOf(ref.getType()); } catch (Throwable ignore) { refType = "UNKNOWN"; }
                    try { refMessageId = safeStr(ref::getMessageId); } catch (Throwable ignore) { }
                    try { refChannelId = safeStr(ref::getChannelId); } catch (Throwable ignore) { }
                    try { refGuildId = safeStr(ref::getGuildId); } catch (Throwable ignore) { }
                }
            } catch (Throwable ignore) { }

            try { webhook = bool01(() -> msg.isWebhookMessage()); } catch (Throwable ignore) { }
            try { bot = bool01(() -> msg.getAuthor() != null && msg.getAuthor().isBot()); } catch (Throwable ignore) { }

            try { att = sizeSafe(msg.getAttachments()); } catch (Throwable ignore) { }
            try { emb = sizeSafe(msg.getEmbeds()); } catch (Throwable ignore) { }
            try { reac = sizeSafe(msg.getReactions()); } catch (Throwable ignore) { }

            try { preview = abbreviate(cleanOneLine(extractContentIncludingEmbeds(msg)), 120); } catch (Throwable ignore) { }
        } catch (Throwable ignore) {
            // 取り得る最小情報のみ
        }

        // フィールドは固定順序で前方に、PREVIEWは末尾
        StringBuilder sb = new StringBuilder(256);
        sb.append("TYPE=").append(type)
                .append(" | G=").append(guildId)
                .append(" | C=").append(channelId)
                .append(" | M=").append(messageId)
                .append(" | REF_TYPE=").append(refType)
                .append(" | REF_G=").append(refGuildId)
                .append(" | REF_C=").append(refChannelId)
                .append(" | REF_M=").append(refMessageId)
                .append(" | WEBHOOK=").append(webhook)
                .append(" | BOT=").append(bot)
                .append(" | ATT=").append(att)
                .append(" | EMB=").append(emb)
                .append(" | REAC=").append(reac)
                .append(" | TS=").append(ts)
                .append(" | PREVIEW=\"").append(escapeForField(preview)).append('"');
        return sb.toString();
    }

    // --- 以下は小さな補助 ---
    private static int sizeSafe(Collection<?> c) { return (c == null) ? 0 : c.size(); }

    private static int bool01(Supplier0<Boolean> s) {
        try { return Boolean.TRUE.equals(s.get()) ? 1 : 0; } catch (Throwable t) { return 0; }
    }

    private static String abbreviate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private static String cleanOneLine(String s) {
        if (s == null) return "";
        // 改行やタブはスペースに、連続空白は1つへ
        String x = s.replace("\r", " ").replace("\n", " ").replace("\t", " ");
        x = x.replaceAll("\\s{2,}", " ").trim();
        return x;
    }

    private static String escapeForField(String s) {
        if (s == null) return "";
        // 区切り「|」とダブルクォートを視認性の高い別文字に置換
        return s.replace("|", "¦").replace("\"", "”");
    }

    private static String safeStr(Supplier0<String> s) {
        try { return s.get(); } catch (Throwable t) { return "-"; }
    }

    @FunctionalInterface
    private interface Supplier0<T> { T get(); }

    /** Discord内部リンクの表示形式対応 */
    protected String preprocessArchiveText(Message msg, String text) {
        if (text == null){ return ""; }
        String processed = replaceEveryoneHereMentions(text); // @here/everyone
        // 1) Replace Discord message links
        processed = replaceDiscordMessageLinksWithPlaceholders(msg, processed);
        // 2) Replace user and role mentions: <@123>, <@!123>, <@&456> -> @表示名
        processed = replaceUserAndRoleMentions(msg, processed);
        // 3) Replace channel mentions: <#789> -> #表示名
        processed = replaceChannelMentions(msg, processed);

        return processed;
    }

    private String replaceEveryoneHereMentions(String text) {
        int idx = 0;
        Pattern p = Pattern.compile("(?<![\\w@.#])@(everyone|here)(?![\\w@])");
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group(1);
            String label = "@" + token; // exactly as typed
            String placeholder = D2H_INLINE_R_PREFIX + placeholderNonce + "_EH_" + (idx++) + "}}";
            // Reuse mention-role styling to keep same blue style
            String html = "<span class=\"mention-role\">" + htmlEscape(label) + "</span>";
            inlineHtmlMap.put(placeholder, html);
            m.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceUserAndRoleMentions(Message msg, String text) {
        int idx = 0;
        // Users: <@123> or <@!123>
        Pattern pUser = Pattern.compile("<@!?([0-9]+)>");
        Matcher mUser = pUser.matcher(text);
        StringBuilder sbUser = new StringBuilder();
        while (mUser.find()) {
            String id = mUser.group(1);
            String name = null;
            try {
                Member member = msg.getGuild().getMemberById(id);
                if (member != null) {
                    name = member.getEffectiveName();
                } else if (msg.getJDA().getUserById(id) != null) {
                    name = msg.getJDA().getUserById(id).getName();
                }
            } catch (Throwable ignore) { }
            if (name == null || name.isBlank()) {
                name = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED;
            }
            String placeholder = D2H_INLINE_U_PREFIX + placeholderNonce + "_" + (idx++) + "}}";
            String html = "<span class=\"mention-user\">" + htmlEscape("@" + name) + "</span>";
            inlineHtmlMap.put(placeholder, html);
            mUser.appendReplacement(sbUser, Matcher.quoteReplacement(placeholder));
        }
        mUser.appendTail(sbUser);
        String out = sbUser.toString();

        // Roles: <@&456>
        Pattern pRole = Pattern.compile("<@&([0-9]+)>");
        Matcher mRole = pRole.matcher(out);
        StringBuilder sbRole = new StringBuilder();
        while (mRole.find()) {
            String id = mRole.group(1);
            String roleName = null;
            try {
                Role role = msg.getGuild().getRoleById(id);
                if (role != null) { roleName = role.getName(); }
            } catch (Throwable ignore) { }
            if (roleName == null || roleName.isBlank()) {
                roleName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED;
            }
            String placeholder = D2H_INLINE_R_PREFIX + placeholderNonce + "_" + (idx++) + "}}";
            String html;
            try {
                Role role = msg.getGuild().getRoleById(id);
                if (role != null && role.getColor() != null) {
                    Color rc = role.getColor();
                    String fg = "#" + String.format("%02x%02x%02x", rc.getRed(), rc.getGreen(), rc.getBlue());
                    // Compute background with alpha over dark base to keep contrast. Use 0.24 opacity similar to Discord.
                    String bg = "rgba(" + rc.getRed() + "," + rc.getGreen() + "," + rc.getBlue() + ",0.24)";
                    html = "<span class=\"mention-role\" style=\"color:" + fg + ";background:" + bg + ";\">" + htmlEscape("@" + roleName) + "</span>";
                } else {
                    html = "<span class=\"mention-role\">" + htmlEscape("@" + roleName) + "</span>";
                }
            } catch (Throwable t) {
                html = "<span class=\"mention-role\">" + htmlEscape("@" + roleName) + "</span>";
            }
            inlineHtmlMap.put(placeholder, html);
            mRole.appendReplacement(sbRole, Matcher.quoteReplacement(placeholder));
        }
        mRole.appendTail(sbRole);
        return sbRole.toString();
    }

    private String replaceChannelMentions(Message msg, String text) {
        // 1) Replace <#channelId> tokens
        Pattern p = Pattern.compile("<#([0-9]+)>");
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (m.find()) {
            String id = m.group(1);
            String display;
            try {
                GuildChannel chAny = null;
                try { chAny = msg.getJDA().getChannelById(GuildChannel.class, id); } catch (Throwable ignore) { }
                if (chAny != null) {
                    display = formatChannelDisplay(chAny, msg);
                } else {
                    // Not resolvable from cache: external vs deleted
                    boolean isExternal = false;
                    try {
                        GuildChannel same = null;
                        try { same = msg.getGuild().getGuildChannelById(id); } catch (Throwable ignore) { }
                        GuildChannel any = null;
                        try { any = msg.getJDA().getChannelById(GuildChannel.class, id); } catch (Throwable ignore) { }
                        // If neither resolves, likely an external server channel (bot not joined)
                        if (same == null && any == null) {
                            isExternal = true;
                        }
                    } catch (Throwable ignore) { }
                    if (isExternal) {
                        display = "#" + ChannelName.ANOTHER_GUILD;
                    } else {
                        display = "#" + (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
                    }
                }
            } catch (Throwable t) {
                display = "#" + (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
            }
            String placeholder = D2H_INLINE_C_PREFIX + placeholderNonce + "_" + (idx++) + "}}";
            String html = "<span class=\"mention-channel\">" + htmlEscape(display) + "</span>";
            inlineHtmlMap.put(placeholder, html);
            m.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        m.appendTail(sb);
        String out = sb.toString();

        // 2) Additionally, replace channel URL patterns: https://discord.com/channels/{guildId}/{channelId}
        Pattern pUrl = Pattern.compile("https?://(?:(?:canary|ptb)\\.)?discord(?:app)?\\.com/channels/([0-9@me]+)/([0-9]+)(?!/)");
        Matcher mUrl = pUrl.matcher(out);
        StringBuilder sb2 = new StringBuilder();
        while (mUrl.find()) {
            String guildIdStr = mUrl.group(1);
            String channelIdStr = mUrl.group(2);
            String display = null;
            try {
                GuildChannel chAny = null;
                try { chAny = msg.getJDA().getChannelById(GuildChannel.class, channelIdStr); } catch (Throwable ignore) { }
                if (chAny != null) {
                    display = formatChannelDisplay(chAny, msg);
                } else {
                    // Could not resolve channel; use guildIdStr to differentiate external vs deleted
                    boolean external = false;
                    try {
                        if (guildIdStr != null && !"@me".equals(guildIdStr)) {
                            Guild g = msg.getJDA().getGuildById(guildIdStr);
                            if (g == null) {
                                external = true;
                            } else if (g.getIdLong() != msg.getGuild().getIdLong()) {
                                external = false; // known other guild but channel missing -> deleted
                            }
                        }
                    } catch (Throwable ignore) { }
                    if (external) {
                        display = "#" + ChannelName.ANOTHER_GUILD;
                    } else {
                        display = "#" + (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
                    }
                }
            } catch (Throwable ignore) {
                display = "#" + (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
            }
            String placeholder = D2H_INLINE_C_PREFIX + placeholderNonce + "_URL_" + (idx++) + "}}";
            String html = "<span class=\"mention-channel\">" + htmlEscape(display) + "</span>";
            inlineHtmlMap.put(placeholder, html);
            mUrl.appendReplacement(sb2, Matcher.quoteReplacement(placeholder));
        }
        mUrl.appendTail(sb2);
        return sb2.toString();
    }
    private String formatChannelDisplay(GuildChannel chAny, Message msg) {
        // Builds display including leading '#', handling threads and cross-guild.
        try {
            boolean sameGuild = chAny.getGuild().getIdLong() == msg.getGuild().getIdLong();
            String chName = null;
            try { chName = chAny.getName(); } catch (Throwable ignore) { }
            String threadSuffix = null;
            try {
                if (chAny.getType().isThread()) {
                    ThreadChannel tc = (ThreadChannel) chAny;
                    String parentName = null;
                    try { parentName = tc.getParentChannel().getName(); } catch (Throwable ignore) { }
                    if (parentName == null || parentName.isBlank()) { 
                        parentName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; 
                    }
                    String threadName = (chName == null || chName.isBlank()) ? (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED) : chName;
                    threadSuffix = parentName + ">" + threadName;
                }
            } catch (Throwable ignore) { }
            if (!sameGuild) {
                String gName = null;
                try { gName = chAny.getGuild().getName(); } catch (Throwable ignore) { }
                if (gName == null || gName.isBlank()) { gName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                String body = (threadSuffix != null) ? threadSuffix : ((chName == null || chName.isBlank()) ? (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED) : chName);
                return "#" + gName + ">" + body;
            } else {
                if (threadSuffix != null) {
                    return "#" + threadSuffix;
                } else {
                    if (chName == null || chName.isBlank()) { chName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                    return "#" + chName;
                }
            }
        } catch (Throwable t) {
            return "#" + (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
        }
    }

    private String replaceDiscordMessageLinksWithPlaceholders(Message msg, String text) {
        // Match discord message URLs: https://discord.com/channels/{guildId}/{channelId}/{messageId}
        Pattern p = Pattern.compile("https?://(?:(?:canary|ptb)\\.)?discord(?:app)?\\.com/channels/([0-9@me]+)/([0-9]+)/([0-9]+)");
        Matcher m = p.matcher(text);
        int idx = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String guildIdStr = m.group(1);
            String channelIdStr = m.group(2);
            String messageIdStr = m.group(3);
            String placeholder = D2H_MSG_PLACEHOLDER_PREFIX + placeholderNonce + "_" + (idx++) + "}}";

            // Build display elements
            String chDisplay = "";
            String authorDisplay = "";
            String timeDisplay = ""; // yyyy/MM/dd HH:mm
            String contentPreview = "";
            try {
                GuildMessageChannel ch = null;
                boolean externalChannel = false;
                try {
                    ch = msg.getJDA().getChannelById(GuildMessageChannel.class, channelIdStr);
                } catch (Throwable ignore) { }
                if (ch != null) {
                    boolean sameGuild = (ch.getGuild().getIdLong() == msg.getGuild().getIdLong());
                    // Thread-aware formatting
                    String threadSuffix = null;
                    if (ch.getType().isThread()) {
                        ThreadChannel tc = (ThreadChannel) ch;
                        String parentName = tc.getParentChannel().getName();
                        if (parentName.isBlank()) { 
                            parentName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; 
                        }
                        String threadName = ch.getName().isBlank() ? (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED) : ch.getName();
                        threadSuffix = parentName + ">" + threadName;
                    }
                    if (!sameGuild) {
                        String guildName = ch.getGuild().getName();
                        if (guildName.isBlank()) { guildName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                        String body = (threadSuffix != null) ? threadSuffix : (ch.getName());
                        if (body.isBlank()) { body = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                        chDisplay = guildName + ">" + body;
                    } else {
                        if (threadSuffix != null) {
                            chDisplay = threadSuffix;
                        } else {
                            String name = ch.getName();
                            if (name.isBlank()) { name = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                            chDisplay = name;
                        }
                    }
                } else {
                    // Could not resolve channel via JDA cache.
                    try {
                        if (guildIdStr != null && !"@me".equals(guildIdStr)) {
                            Guild g = msg.getJDA().getGuildById(guildIdStr);
                            if (g == null) {
                                externalChannel = true; // bot not in that guild
                            } else if (g.getIdLong() != msg.getGuild().getIdLong()) {
                                // Different guild present but channel missing -> treat as deleted in that guild
                                externalChannel = false;
                            }
                        }
                    } catch (Throwable ignore) { }
                    if (externalChannel) {
                        // External server and not resolvable: show an explicit external label with channelId hint
                        chDisplay = ChannelName.ANOTHER_GUILD + htmlEscape(channelIdStr);
                    } else {
                        chDisplay = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED;
                    }
                }
                Message target = null;
                try {
                    if (ch != null) {
                        target = ch.retrieveMessageById(messageIdStr).complete();
                    }
                } catch (Throwable ignore) { }
                if (target != null) {
                    try {
                        Member mbr = target.getMember();
                        if (mbr != null) {
                            authorDisplay = mbr.getEffectiveName();
                        } else {
                            authorDisplay = target.getAuthor().getName();
                        }
                    } catch (Throwable ignore) { }
                    if (authorDisplay.isBlank()) {
                        authorDisplay = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED;
                    }
                    try {
                        Date d = Date.from(target.getTimeCreated().toInstant());
                        String full = DateTimeUtil.time().format(d); // yyyy/MM/dd HH:mm:ss
                        timeDisplay = (full.length() >= 16) ? full.substring(0, 16) : full; // drop :ss
                    } catch (Throwable ignore) { }
                    try {
                        String raw = extractContentIncludingEmbeds(target);
                        contentPreview = raw.length() > 50 ? raw.substring(0, 50)+"..." : raw;
                    } catch (Throwable ignore) { }
                } else {
                    // target == null
                    authorDisplay = externalChannel ? AbstName.UNKNOWN : (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED);
                    timeDisplay = "";
                }
            } catch (Throwable ignore) { }

            String textDisplay = "#" + chDisplay + "\uD83D\uDCAC@" + authorDisplay + (timeDisplay.isEmpty() ? "" :("("+timeDisplay+")"));
            String attrPreview = htmlEscape(contentPreview);
            String attrTitle = attrPreview; // use the same text for native tooltip
            String spanHtml = "<span class=\"msg-link\" data-content=\"" + attrPreview + "\" title=\"" + attrTitle + "\">" + htmlEscape(textDisplay) + "</span>";

            msgLinkHtmlMap.put(placeholder, spanHtml);
            m.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String buildForwardedBlockquoteHtml(Guild current, MessageReference forwarded) {
        Message refMessage = null;
        MessageChannelUnion channel = forwarded.getChannel();
        if (channel != null) {
            refMessage = channel.retrieveMessageById(forwarded.getMessageId()).complete();
        }

        //debug
        if (refMessage != null) {
            System.out.println(formatLinkAuditLine(refMessage));
        }
        
        try {
            // Build origin displays similar to buildMsgLinkSpanFor but using snapshot APIs
            String chDisplay = "";
            String timeDisplay = "";
            try {
                MessageChannelUnion chAny = forwarded.getChannel();
                boolean sameGuild = true;
                try {
                    Guild g2 = forwarded.getGuild();
                    sameGuild = (current.getIdLong() == g2.getIdLong());
                } catch (Throwable ignore) { }
                String threadSuffix = null;
                try {
                    if (chAny.getType().isThread()) {
                        ThreadChannel tc = (ThreadChannel) chAny;
                        String parentName = tc.getParentChannel().getName();
                        if (parentName.isBlank()) { parentName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                        String threadName = (chAny.getName().isBlank()) ? (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED) : chAny.getName();
                        threadSuffix = parentName + ">" + threadName;
                    }
                } catch (Throwable ignore) { }
                if (!sameGuild) {
                    String guildName = null;
                    try { guildName = forwarded.getGuild().getName(); } catch (Throwable ignore) { }
                    if (guildName == null || guildName.isBlank()) {
                        guildName = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; 
                    }
                    String body = (threadSuffix != null) ? threadSuffix : (chAny != null ? chAny.getName() : (AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED));
                    if (body.isBlank()) { body = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                    chDisplay = guildName + ">" + body;
                } else {
                    if (threadSuffix != null) {
                        chDisplay = threadSuffix;
                    } else {
                        String name = null;
                        try { name = chAny != null ? chAny.getName() : null; } catch (Throwable ignore) { }
                        if (name == null || name.isBlank()) { name = AbstName.EMPTY_NAME + AbstName.SUFFIX_DELETED; }
                        chDisplay = name;
                    }
                }
                try {
                    Date d = refMessage != null ? Date.from(refMessage.getTimeCreated().toInstant()) : new Date();
                    String full = DateTimeUtil.time().format(d);
                    timeDisplay = (full.length() >= 16) ? full.substring(0, 16) : full;
                } catch (Throwable ignore) { }
            } catch (Throwable ignore) { }
            String origin = "#" + chDisplay + "\uD83D\uDCAC" + (timeDisplay.isEmpty() ? "" : ("(" + timeDisplay + ")"));
            String contentRaw = refMessage != null ? refMessage.getContentRaw() : null;
            String bodyProcessed = preprocessArchiveText(refMessage, contentRaw);
            String bodyHtml = toHtmlWithLinks(bodyProcessed);
            return "<blockquote class=\"forwarded\">"
                   + bodyHtml
                   + "<cite>" + origin + "</cite>"
                   + "</blockquote>";
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
