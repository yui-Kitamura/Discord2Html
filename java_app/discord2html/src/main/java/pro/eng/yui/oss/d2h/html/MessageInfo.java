package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.entities.messages.MessagePoll;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import org.jetbrains.annotations.Contract;
import pro.eng.yui.oss.d2h.db.field.*;
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
        return applyInlineAndMsgLinkReplacements(html);
    }

    /**
     * Apply replacements for prepared placeholders from both link maps to the given html/text.
     * This method MUST be used wherever we convert message text to HTML so that no {{D2H_* placeholders leak.
     */
    private String applyInlineAndMsgLinkReplacements(String html) {
        if (html == null || html.isEmpty()) { return ""; }
        if (!msgLinkHtmlMap.isEmpty()) {
            for (Map.Entry<String, String> e : msgLinkHtmlMap.entrySet()) {
                html = html.replace(e.getKey(), e.getValue());
            }
        }
        if (!inlineHtmlMap.isEmpty()) {
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
                EmojiUnion emoji = r.getEmoji();
                String name = emoji.getName();
                int count = r.getCount();
                boolean isCustom = false;
                String id;
                if (emoji.getType() == Emoji.Type.CUSTOM) {
                    isCustom = true;
                }
                if (isCustom) {
                    CustomEmoji customEmoji = emoji.asCustom();
                    id = customEmoji.getId();
                    String ext = customEmoji.isAnimated() ? EmojiInfo.EXT_GIF : EmojiInfo.EXT_PNG;
                    String url = ("https://cdn.discordapp.com/emojis/" + id + "." + ext);
                    String localPath = ("archives/emoji/emoji_" + id + "_" + DateTimeUtil.date8().format(new Date()) + "." + ext);
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

    // Pinned flag
    private final boolean pinned;
    public boolean isPinned() { return this.pinned; }

    // Poll (投票)
    private final String pollQuestion; // display text for question (escaped)
    private final String pollAnswersHtml; // concatenated <li>...</li> items (already HTML)
    private final String pollStartTimeText; // e.g., "yyyy/MM/dd HH:mm:ss 投票開始：" (only for result messages)
    private final String pollEndTimeText;   // e.g., "yyyy/MM/dd HH:mm:ss 投票締切："
    private final List<EmojiInfo> pollEmojis; // custom emojis used in poll options
    public String getPollQuestion() { return this.pollQuestion; }
    public String getPollAnswersHtml() { return this.pollAnswersHtml; }
    public String getPollStartTimeText() { return this.pollStartTimeText; }
    public String getPollEndTimeText() { return this.pollEndTimeText; }
    public List<EmojiInfo> getPollEmojis() { return this.pollEmojis == null ? List.of() : this.pollEmojis; }

    public static class EmojiInfo {
        private final String id;
        private final String name;
        private final boolean animated;

        public static final String EXT_GIF = "gif";
        public static final String EXT_PNG = "png";
        
        public EmojiInfo(String id, String name, boolean animated) {
            this.id = id;
            this.name = name;
            this.animated = animated;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public boolean isAnimated() { return animated; }
    }
    
    public enum ForwardMask {
        ARCHIVE, OPTOUT, UNKNOWN;
        public boolean doMask() {
            return this == OPTOUT || this == UNKNOWN;
        }
    }

    /** コンストラクタ */
    public MessageInfo(Message msg) {
        this(msg, new Users(msg.getAuthor(), msg.getGuild()), null, false, ForwardMask.UNKNOWN);
    }
    
    public MessageInfo(Message msg, Users authorInfo, String anonymizeScopeKey, boolean maskContent, ForwardMask maskForwarded){
        this.msgLinkHtmlMap = new HashMap<>();
        this.inlineHtmlMap = new HashMap<>();
        this.createdTimestamp = DateTimeUtil.time().format(Date.from(msg.getTimeCreated().toInstant()));
        this.userInfo = authorInfo;
        this.anonymizeScopeKey = anonymizeScopeKey;
        this.messageUserInfo = (anonymizeScopeKey == null)
                ? AnonymizationUtil.anonymizeUser(authorInfo)
                : AnonymizationUtil.anonymizeUser(authorInfo, anonymizeScopeKey);
        // Determine main content (mask if opted-out)
        this.contentRaw = maskContent ? "***（非公開希望ユーザーの発言）***" : extractContentIncludingEmbeds(msg);
        this.contentProcessed = preprocessArchiveText(msg, this.contentRaw);
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        this.pinned = msg.isPinned();
        String colorHex = null;
        try {
            Member m = msg.getMember();
            if (m != null) {
                Color c = m.getColor();
                if (c != null) {
                    colorHex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
                }
            }
        } catch (Throwable ignore) { ignore.printStackTrace(); }
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
            if (ref != null && ref.getType() == MessageReference.MessageReferenceType.FORWARD) {
                tmpForwardedHtml = buildForwardedBlockquoteHtml(msg.getGuild(), msg, maskForwarded);
                tmpForwarded = (tmpForwardedHtml != null);
            }
        } catch (Throwable t) { t.printStackTrace(); }
        this.forwarded = tmpForwarded;
        this.forwardedHtml = tmpForwardedHtml;

        // Build poll parts if present
        String tmpQuestion = null;
        String tmpAnswers = null;
        String tmpStart = null;
        String tmpEnd = null;
        List<EmojiInfo> tmpPollEmojis = null;
        try {
            PollParts pp = buildPollParts(msg);
            if (pp != null) {
                tmpQuestion = pp.question;
                tmpAnswers = pp.answersHtml;
                tmpStart = pp.startTimeText;
                tmpEnd = pp.endTimeText;
                tmpPollEmojis = pp.emojis;
            }
        } catch (Throwable ignore) { }
        this.pollQuestion = (tmpQuestion != null && !tmpQuestion.isBlank()) ? tmpQuestion : null;
        this.pollAnswersHtml = (tmpAnswers != null && !tmpAnswers.isBlank()) ? tmpAnswers : null;
        this.pollStartTimeText = tmpStart;
        this.pollEndTimeText = tmpEnd;
        this.pollEmojis = (tmpPollEmojis == null) ? List.of() : List.copyOf(tmpPollEmojis);
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
        if(msg.getType() == MessageType.POLL_RESULT) {
            return null;
        }
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
            boolean maskMention = false;
            try {
                try {
                    UserId uid = new UserId(Long.parseUnsignedLong(id));
                    GuildId gid = new GuildId(msg.getGuild());
                    ChannelId cid = new ChannelId(msg.getChannel());
                    // Mask when opted-out OR anonymous per current guild/role settings
                    maskMention = FileGenerateUtil.isUserOptedOut(uid, gid, cid)
                            || FileGenerateUtil.isUserAnonymous(uid, msg.getGuild());
                } catch (Throwable ignore) { /* best-effort */ }
                if (!maskMention) {
                    Member member = msg.getGuild().getMemberById(id);
                    if (member != null) {
                        name = member.getEffectiveName();
                    } else if (msg.getJDA().getUserById(id) != null) {
                        name = msg.getJDA().getUserById(id).getName();
                    }
                }
            } catch (Throwable ignore) { }
            if (maskMention) {
                name = UserName.ANON;
            }
            if (name == null || name.isBlank()) {
                name = UserName.EMPTY_NAME + UserName.SUFFIX_DELETED;
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
    
    private static class PollParts {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PollParts.class);
        /** HTMLエスケープ済み 質問テキスト */
        final String question;
        /** <code>&lt;li&gt;</code>エレメント群 */
        final String answersHtml;
        /** yyyy/MM/dd HH:mm:ss 投票開始： (結果メッセージ時のみ) */
        final String startTimeText;
        /** yyyy/MM/dd HH:mm:ss 投票締切： */
        final String endTimeText;
        /** Poll内で使用されているカスタム絵文字の一覧 */
        final List<EmojiInfo> emojis;
        PollParts(String question, String answersHtml, String startTimeText, String endTimeText, List<EmojiInfo> emojis) {
            this.question = question;
            this.answersHtml = answersHtml;
            this.startTimeText = startTimeText;
            this.endTimeText = endTimeText;
            this.emojis = (emojis == null) ? List.of() : List.copyOf(emojis);
        }
    }

    private PollParts buildPollParts(Message msg) {
        final boolean isResult = (msg.getType() == MessageType.POLL_RESULT);
        try {
            if(isResult) {
                try {
                    MessageReference ref = msg.getMessageReference();
                    Message original = msg.getJDA().getChannelById(GuildMessageChannel.class, ref.getChannelIdLong())
                            .retrieveMessageById(ref.getMessageIdLong()).complete();
                    if (original != null) {
                        msg = original; //元メッセージの参照
                    }
                } catch (Exception ignore) { }
            }

            MessagePoll poll = msg.getPoll();
            if (poll == null){ return null; }

            // Question text
            String question = null;
            try {
                MessagePoll.Question q = poll.getQuestion();
                question = q.getText();
            } catch (Throwable ignore) { }
            if (question == null){ question = ""; }
            final String escapedQuestion = htmlEscape(question);

            // Answers
            List<MessagePoll.Answer> answers = poll.getAnswers();
            if (answers.isEmpty()){ return null; }

            // Determine if finalized and collect votes
            boolean finalized =  poll.isFinalizedVotes();

            int totalVotes = 0;
            for(MessagePoll.Answer ans : poll.getAnswers()) {
                totalVotes += ans.getVotes();
            }
            
            StringBuilder li = new StringBuilder();
            List<EmojiInfo> emojiList = new ArrayList<>();
            for (MessagePoll.Answer ans : answers) {
                String emojiStr = "";
                try {
                    if (ans.getEmoji() != null) {
                        EmojiUnion eu = ans.getEmoji();
                        emojiStr = eu.getFormatted();
                        if (eu.getType() == Emoji.Type.CUSTOM) {
                            CustomEmoji ce = eu.asCustom();
                            emojiList.add(new EmojiInfo(ce.getId(), ce.getName(), ce.isAnimated()));
                        }
                    }
                } catch (Throwable ignore) { }
                final String answerText = emojiStr + ans.getText();
                li.append("<li class=\"poll-item\">");
                if (finalized) {
                    int v = ans.getVotes();
                    double pct = (totalVotes > 0) ? Math.round(v * 10000f / totalVotes) / 100f : 0f;
                    li.append("<div class=\"poll-result\">");
                    li.append("<span class=\"poll-label\">")
                            .append(toHtmlWithLinks(answerText))
                            .append("</span>");
                    li.append("<span class=\"poll-bar\"><span style=\"width:").append(pct).append("%\"></span></span>");
                    li.append("<span class=\"poll-count\"><span class=\"num\">").append(v).append("</span><span class=\"unit\">票</span></span>");
                    li.append("<span class=\"poll-percent\"><span class=\"num\">").append(String.format("%.2f", pct)).append("</span><span class=\"unit\">%</span></span>");
                    li.append("</div>");
                    
                } else {
                    li.append("<span class=\"poll-label\">").append(toHtmlWithLinks(answerText)).append("</span>");
                }
                li.append("</li>");
            }
            
            String startText = "投票開始：";
            try {
                if(isResult) {
                    startText += DateTimeUtil.time().format(Date.from(msg.getTimeCreated().toInstant()));
                }else {
                    startText = null;
                }
            }catch(Throwable ignore){ }
            String endText = "投票締切：";
            try {
                endText += DateTimeUtil.time().format(Date.from(poll.getTimeExpiresAt().toInstant()));
            }catch(Throwable ignore){ }

            return new PollParts(escapedQuestion, li.toString(), startText, endText, emojiList);
        } catch (Throwable t) {
            return null;
        }
    }

    private String buildForwardedBlockquoteHtml(Guild current, Message message, ForwardMask mask) {
        List<MessageSnapshot> forwarded = message.getMessageSnapshots();
        if (forwarded.isEmpty()) {
            return null;
        }

        StringBuilder html = new StringBuilder();
        for (MessageSnapshot snapshot : forwarded) {
            String messageHtml = buildForwardedMessageHtml(current, message, snapshot, mask);
            if (messageHtml != null) {
                html.append(messageHtml);
            }
        }
        return (html.isEmpty() == false) ? html.toString() : null;
    }
    
    private String buildForwardedMessageHtml(Guild current, Message forwarded, MessageSnapshot snapshot, ForwardMask mask) {
        if (snapshot == null) {
            return null;
        }

        try {
            MessageReference msgRef = forwarded.getMessageReference();
            Message sourceMsg = null;
            try {
                sourceMsg = forwarded.getJDA().getChannelById(GuildMessageChannel.class, msgRef.getChannelIdLong())
                        .retrieveMessageById(msgRef.getMessageIdLong()).complete();
            }catch(NullPointerException ignore){ }

            String chDisplay;
            String timeDisplay = "";
            try {
                MessageChannelUnion chAny = msgRef.getChannel(); // if null then catch and to be UNKNOWN
                boolean sameGuild = true;
                try {
                    Guild g2 = sourceMsg.getGuild();
                    sameGuild = (current != null && (current.getIdLong() == g2.getIdLong()));
                } catch (Throwable ignore) { }
                String threadSuffix = null;
                try {
                    if (chAny.getType().isThread()) {
                        ThreadChannel tc = (ThreadChannel) chAny;
                        String parentName = tc.getParentChannel().getName();
                        if (parentName.isBlank()) { parentName = ChannelName.UNKNOWN; }
                        String threadName = (chAny.getName().isBlank()) ? ChannelName.UNKNOWN : chAny.getName();
                        threadSuffix = parentName + ">" + threadName;
                    }
                } catch (Throwable ignore) { }
                if (sameGuild) {
                    if (threadSuffix != null) {
                        chDisplay = threadSuffix;
                    } else {
                        String name = "";
                        try { name = chAny.getName(); } catch (Throwable ignore) { }
                        if (name.isBlank()) { name = ChannelName.UNKNOWN; }
                        chDisplay = name;
                    }
                } else {
                    String guildName = "";
                    try {
                        guildName = forwarded.getGuild().getName();
                    } catch (Throwable ignore) { }
                    if (guildName.isBlank()) { guildName = GuildName.UNKNOWN; }
                    String chName = (threadSuffix != null) ? threadSuffix : chAny.getName();
                    if (chName.isBlank()) { chName = ChannelName.UNKNOWN; }
                    chDisplay = guildName + ">" + chName;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                chDisplay = ChannelName.UNKNOWN;
            }
            try {
                Date d = Date.from(sourceMsg.getTimeCreated().toInstant());
                if (mask.doMask()) {
                    timeDisplay = DateTimeUtil.dateOnly().format(d);
                } else {
                    // For normal forwarded messages (non-opt-out), show full timestamp including seconds
                    timeDisplay = DateTimeUtil.time().format(d);
                }
            } catch (NullPointerException ignore) { }

            String origin = "#" + chDisplay + "\uD83D\uDCAC" + (timeDisplay.isEmpty() ? "" : ("(" + timeDisplay + ")"));

            String bodyHtml;
            switch (mask) {
                case ARCHIVE -> {
                    bodyHtml = toHtmlWithLinks(preprocessArchiveText(sourceMsg, snapshot.getContentRaw()));
                    bodyHtml = applyInlineAndMsgLinkReplacements(bodyHtml);
                }
                case UNKNOWN -> {
                    //サーバーのユーザでないとオプトアウトできないため非公開
                    bodyHtml = "<div class=\"content\" optout\">***（外部サーバーの発言）***</div>";
                }
                case OPTOUT -> {
                    bodyHtml = "<div class=\"content optout\">***（非公開希望ユーザーの発言）***</div>";
                }
                default -> bodyHtml = "";
            }

            return bodyHtml + "<cite>" + origin + "</cite>";
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
