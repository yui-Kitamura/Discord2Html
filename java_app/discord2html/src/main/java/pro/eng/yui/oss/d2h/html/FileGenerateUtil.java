package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.Contract;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.dao.AnonStatsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.dao.OptoutDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.db.model.Users;
import pro.eng.yui.oss.d2h.github.GitConfig;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileGenerateUtil {

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

    private final GuildsDAO guildsDao;
    private final UsersDAO usersDao;
    private final GitConfig gitConfig;
    private final DiscordJdaProvider jdaProvider;
    private static OptoutDAO staticOptoutDao;
    private static AnonStatsDAO staticAnonStatsDao;
    
    public FileGenerateUtil(
            GuildsDAO guildsDAO, AnonStatsDAO anonStatsDAO, UsersDAO usersDAO, OptoutDAO optoutDao,
            DiscordJdaProvider jdaProvider,
            GitConfig gitConfig) {
        this.guildsDao = guildsDAO;
        this.usersDao = usersDAO;
        this.jdaProvider = jdaProvider;
        this.gitConfig = gitConfig;
        FileGenerateUtil.staticOptoutDao = optoutDao;
        FileGenerateUtil.staticAnonStatsDao = anonStatsDAO;
    }

    public static boolean isUserOptedOut(UserId userId, GuildId guildId, ChannelId channelId) {
        try {
            return staticOptoutDao.isOptedOut(userId, guildId, channelId);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return true;
        }
    }

    /**
     * Check if the target user should be displayed anonymously in the given guild context
     * using the same rule as message authors (role and user settings; bots are OPEN).
     * Defaults to true (anonymous) on any error to avoid leaking names.
     */
    public static boolean isUserAnonymous(UserId userId, Guild guild) {
        try {
            if (guild == null || userId == null) { return true; }
            Member member = null;
            try { member = guild.getMemberById(userId.getValue()); } catch (Throwable ignore) { }
            if (member == null) { return true; }
            return staticAnonStatsDao.extractAnonStats(member).isAnon();
        } catch (Throwable ignore) {
            return true;
        }
    }

    /**
     * Write content to a file only if it changed. Creates the file when missing.
     */
    public void writeIfChanged(Path target, String newContent) {
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

    @Contract(" -> !null")
    public String repoBase() {
        try {
            String name = gitConfig.getRepo().getName();
            if (name == null) { return ""; }
            name = name.trim();
            while (name.startsWith("/")) { name = name.substring(1); }
            while (name.endsWith("/")) { name = name.substring(0, name.length() - 1); }
            return name;
        } catch (Exception ignore) {
            return "";
        }
    }

    @Contract(" -> !null")
    public String repoBaseWithPrefix() {
        return "/" + repoBase();
    }
    
    @Contract("_ -> !null")
    public String normalizeHref(String href) {
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

    @Contract("null -> null")
    public String resolveGuildIconUrl(GuildId guildId) {
        if (guildId == null) { return null; }
        try {
            Guild guild = jdaProvider.getJda().getGuildById(guildId.getValue());
            if (guild != null && guild.getIconUrl() != null && !guild.getIconUrl().isEmpty()) {
                return guild.getIconUrl();
            }
        } catch (Exception ignore) { }
        return null;
    }
    
    public void archiveCustomEmojis(Path outputPath, List<MessageInfo> messages) throws IOException {
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
                    String ext = animated ? MessageInfo.EmojiInfo.EXT_GIF : MessageInfo.EmojiInfo.EXT_PNG;
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
                        saveCustomEmoji(emojiDir, id, name, ext, today, bytes);
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
                        saveCustomEmoji(emojiDir, id, name, ext, today, bytes);
                    }
                }
            } catch (Throwable ignore) {
                // best-effort
            }

            // --- 3) From poll answers (custom emojis) ---
            try {
                List<MessageInfo.EmojiInfo> pe = mi.getPollEmojis();
                if (pe != null) {
                    for (MessageInfo.EmojiInfo ei : pe) {
                        if (ei == null) { continue; }
                        String id = ei.getId();
                        String name = ei.getName();
                        String ext = ei.isAnimated() ? MessageInfo.EmojiInfo.EXT_GIF : MessageInfo.EmojiInfo.EXT_PNG;
                        if (id == null || id.isEmpty()) { continue; }
                        String key = id + "." + ext;
                        if (processed.contains(key)) { continue; }
                        processed.add(key);
                        String url = "https://cdn.discordapp.com/emojis/" + id + "." + ext;
                        byte[] bytes;
                        try (var in = new URL(url).openStream()) {
                            bytes = in.readAllBytes();
                        } catch (IOException ioe) {
                            // skip this emoji if fetch fails
                            continue;
                        }
                        saveCustomEmoji(emojiDir, id, name, ext, today, bytes);
                    }
                }
            } catch (Throwable ignore) {
                // best-effort
            }
        }
    }

    /** カスタム絵文字を物理パスに保存する */
    private void saveCustomEmoji(Path emojiDir, String id, String name, String ext, String today, byte[] bytes) {
        if (emojiDir == null || id == null || ext == null || today == null || bytes == null) { return; }
        String safeName = getSafeEmojiName(name);
        Path todayPath = emojiDir.resolve("emoji_" + id + "_" + safeName + "_" + today + "." + ext);
        try {
            if (Files.exists(todayPath)) {
                Files.write(todayPath, bytes); // overwrite within same day
            } else {
                boolean sameFound = false;
                try {
                    for (Path p : Files.newDirectoryStream(emojiDir, "emoji_" + id + "_*." + ext)) {
                        if (p.getFileName().toString().equals(todayPath.getFileName().toString())) { continue; }
                        try {
                            byte[] past = Files.readAllBytes(p);
                            if (Arrays.equals(past, bytes)) { sameFound = true; break; }
                        } catch (IOException ignore) { }
                    }
                } catch (IOException ignore) { }
                if (!sameFound) {
                    Files.write(todayPath, bytes);
                }
            }
        } catch (IOException ioe) {
            // ignore single emoji failure
        }
        // Also write stable id-based copy
        try {
            Path idStable = emojiDir.resolve(id + "." + ext);
            Files.write(idStable, bytes);
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * カスタム絵文字<img>タグを生成する。フォールバック順は:
     * 1) アーカイブ日のスナップショット (emoji_{id}_{safeName}_{date}.{ext})
     * 2) 最新ID固定パス ({id}.{ext})
     * 3) Discord CDN
     */
    // 依存性注入がない処理のためstaticで提供
    public static String buildEmojiImgHtml(String name, String id, boolean animated) {
        String ext = animated ? MessageInfo.EmojiInfo.EXT_GIF : MessageInfo.EmojiInfo.EXT_PNG;
        String date = DateTimeUtil.date8().format(new Date());
        String safe = getSafeEmojiName(name);
        String first = "/Discord2Html/archives/emoji/emoji_" + id + "_" + safe + "_" + date + "." + ext;
        String second = "/Discord2Html/archives/emoji/" + id + "." + ext;
        String cdn = "https://cdn.discordapp.com/emojis/" + id + "." + ext;
        // 二段階 onerror: 1) 日付付き → 2) id固定 → 3) CDN
        return "<img class='emoji' src='" + first + "' alt='" + (name == null ? "" : name) + "' " +
               "onerror=\"this.onerror=function(){this.onerror=null;this.src='" + cdn + "';};this.src='" + second + "';\" />";
    }

    private static String getSafeEmojiName(String name) {
        String safe = (name == null || name.isEmpty()) ? "emoji" : name;
        return safe.replaceAll("[^A-Za-z0-9_-]", "_");
    }


    /**
     * Discordのタイムスタンプ記法の文字列を人間可読な文字列に変換します
     *
     * @param tag Discordのタイムスタンプ文字列
     * @return 変換された日時文字列。変換に失敗した場合は入力された文字列をそのまま返します。
     */
    public static String convertUnixTime(String tag) {
        if (tag == null || tag.isEmpty()) {
            return tag;
        }
        try {
            Matcher m = DateTimeUtil.DISCORD_TIME_PATTERN.matcher(tag);
            if (!m.matches()) {
                return tag;
            }
            return convertUnixTime(DateTimeUtil.getFromUnix(m.group(1)), m.group(2));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return tag;
        }
    }

    /**
     * カレンダーオブジェクトを指定されたフォーマットの日時文字列に変換します。
     *
     * @param time   変換対象のカレンダーオブジェクト
     * @param format 変換フォーマット (d/D: 日付のみ, t/T: 時刻のみ, f/F/R: 完全な日時)
     * @return フォーマットされた日時文字列
     */
    public static String convertUnixTime(final Calendar time, final String format) {
        final Date timestamp = time.getTime();
        return switch (format) {
            case "d", "D" -> DateTimeUtil.dateOnly().format(timestamp);
            case "t", "T" -> DateTimeUtil.time().format(timestamp);
            case "f", "F", "R" -> DateTimeUtil.full().format(timestamp);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * 指定されたチャンネルから特定期間のメッセージを取得します。
     *
     * @param channel   対象のDiscordチャンネル
     * @param beginDate 取得開始日時
     * @param endDate   取得終了日時
     * @return 取得されたメッセージ情報のリスト
     */
    public List<MessageInfo> fetchMessagesForDaily(GuildMessageChannel channel, Calendar beginDate, Calendar endDate) {
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();

        try {
            Guilds guildInfo = guildsDao.selectGuildInfo(new GuildId(channel.getGuild()));
            int anonCycle = guildInfo != null && guildInfo.getAnonCycle() != null ? guildInfo.getAnonCycle().getValue() : 24;
            if (anonCycle < 1 || 24 < anonCycle) {
                anonCycle = 24;
            }
            final int finalAnonCycle = anonCycle;
            
            MessageHistory history = channel.getHistory();
            GuildId guildId = new GuildId(channel.getGuild());
            final Instant beginInstant = beginDate.toInstant();
            final Instant endInstant = endDate.toInstant();
            boolean more = true;
            while (more) {
                var batch = history.retrievePast(100).complete();
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                var oldest = batch.get(batch.size() - 1);
                var oldestInstant = oldest.getTimeCreated().toInstant();
                batch.stream()
                        .filter(msg -> !msg.getTimeCreated().toInstant().isBefore(beginInstant)
                                && !msg.getTimeCreated().toInstant().isAfter(endInstant))
                        .forEach(msg -> {
                            MessageInfo mi = buildMessageInfo(msg, guildId, finalAnonCycle, marked);
                            if (mi != null) { messages.add(mi); }
                        });
                if (!oldestInstant.isAfter(beginInstant)) {
                    more = false;
                }
            }
        } catch (Throwable ignore) {
            // best-effort: return what we have
        }
        return messages;
    }


    /**
     * 単一のDiscordメッセージからMessageInfo オブジェクトを構築します。
     *
     * @param msg       Discordメッセージ
     * @param guildId   サーバーID
     * @param anonCycle 匿名化サイクル時間（時間単位）
     * @param marked    処理済みユーザーリスト
     * @return 構築されたMessageInfoオブジェクト。失敗時はnull
     */
    private MessageInfo buildMessageInfo(Message msg, GuildId guildId, final int anonCycle, List<Users> marked) {
        try {
            Users author = Users.get(msg, staticAnonStatsDao, staticOptoutDao);
            try {
                if (!marked.contains(author)) {
                    usersDao.upsertUserInfo(author);
                    marked.add(author);
                }
            } catch (Exception ignore) { /* ignore DB issues */ }
            Date msgDate = Date.from(msg.getTimeCreated().toInstant());
            Calendar calJst = Calendar.getInstance(DateTimeUtil.JST);
            calJst.setTime(msgDate);
            int hour = calJst.get(Calendar.HOUR_OF_DAY);
            int cycleIndex = hour / anonCycle;
            String dateStr = DateTimeUtil.date8().format(msgDate);
            String scopeKey = guildId.toString() + "-" + dateStr + "-c" + cycleIndex + "-n" + anonCycle + "-m" + msg.getId();

            // forwarded block should be shown
            MessageInfo.ForwardMask maskForward = MessageInfo.ForwardMask.UNKNOWN;
            try {
                MessageReference ref = msg.getMessageReference();
                if (ref != null && ref.getType() == MessageReference.MessageReferenceType.FORWARD) {
                    // 転送メッセージは、同サーバー内で元ユーザがオプトアウトしてない場合のみ表示
                    if (author.isOptedOut()) {
                        maskForward = MessageInfo.ForwardMask.OPTOUT;
                    }else {
                        try {
                            GuildMessageChannel refCh = msg.getJDA().getChannelById(GuildMessageChannel.class, ref.getChannelIdLong());
                            Message src = refCh.retrieveMessageById(ref.getMessageIdLong()).complete();
                            boolean sameGuild = (src.getGuild().getIdLong() == msg.getGuild().getIdLong());
                            if (sameGuild) {
                                boolean sourceOptout = staticOptoutDao.isOptedOut(new UserId(src.getAuthor()), new GuildId(msg.getGuild()), new ChannelId(ref.getChannelIdLong()));
                                if (sourceOptout) {
                                    maskForward = MessageInfo.ForwardMask.OPTOUT;
                                }else{
                                    maskForward = MessageInfo.ForwardMask.ARCHIVE;
                                } 
                            }
                        } catch (NullPointerException ignore) { /* best-effort */ }
                    }
                }
            } catch (Throwable ignore) { /* best-effort */ }

            return new MessageInfo(msg, author, scopeKey, author.isOptedOut(), maskForward);
        } catch (Throwable ignore) {
            return null;
        }
    }

}
