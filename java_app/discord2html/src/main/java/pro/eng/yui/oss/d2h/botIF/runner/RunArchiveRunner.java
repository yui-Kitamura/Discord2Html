package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBot;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.db.dao.AnonStatsDAO;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.db.model.Users;
import pro.eng.yui.oss.d2h.github.GitHubService;
import pro.eng.yui.oss.d2h.github.GitConfig;
import pro.eng.yui.oss.d2h.html.ChannelInfo;
import pro.eng.yui.oss.d2h.html.FileGenerator;
import pro.eng.yui.oss.d2h.html.MessageInfo;
import pro.eng.yui.oss.d2h.consts.OnRunMessageMode;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RunArchiveRunner implements IRunner {
    // notes for the latest manual command run (validation/errors)
    private final List<String> lastRunNotes = new ArrayList<>();
    
    private final ApplicationConfig config;
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    private final UsersDAO usersDao;
    private final AnonStatsDAO anonStatsDao;
    private final DiscordJdaProvider jda;
    private final FileGenerator fileGenerator;
    private final GitHubService gitHubService;
    private final GitConfig gitConfig;
    private final List<Path> generatedFiles = new ArrayList<>();

    @Autowired
    public RunArchiveRunner(
            ApplicationConfig c,
            GuildsDAO g, ChannelsDAO ch, UsersDAO u, AnonStatsDAO a,
            DiscordJdaProvider j, FileGenerator fileGenerator,
            GitHubService gitHubService, GitConfig gitConfig
    ){
        this.config = c;
        this.guildDao = g;
        this.channelDao = ch;
        this.usersDao = u;
        this.anonStatsDao = a;
        this.jda = j;
        this.fileGenerator = fileGenerator;
        this.gitHubService = gitHubService;
        this.gitConfig = gitConfig;
    }
    
    public void run(Member member, List<OptionMapping> options){
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);
        
        // initialize
        lastRunNotes.clear();
        generatedFiles.clear();

        try {
            // Check for optional date parameter (yyyyMMdd, JST)
            String dateStr = null;
            GuildChannelUnion targetUnion = null;
            for (OptionMapping om : options) {
                if ("target".equals(om.getName())) {
                    try {
                        targetUnion = om.getAsChannel();
                    } catch (Exception ignore) { /* keep null */ }
                    continue;
                }
                if ("date".equals(om.getName())) {
                    try {
                        dateStr = om.getAsString();
                    } catch (Exception ignore) { /* keep null */ }
                    continue;
                }
            }
            if (dateStr == null || dateStr.isBlank()) {
                // Default missing date to today
                dateStr = DateTimeUtil.nowDate8();
            }

            // Validate date format using centralized util
            if (!DateTimeUtil.isValidDate8(dateStr)) {
                lastRunNotes.add("[ERROR] dateオプションは yyyyMMdd 形式で指定してください。例: 20250131");
                return; // ERROR中断
            }
            Calendar day;
            try {
                day = DateTimeUtil.toJstCalendarFromDate8(dateStr);
            } catch (Exception e) {
                lastRunNotes.add("[ERROR] dateオプションの解析に失敗しました: " + e.getMessage());
                return;
            }
            // Enforce lookback days limit (inclusive)
            Calendar today = Calendar.getInstance(DateTimeUtil.JST);
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long diffMs = today.getTimeInMillis() - day.getTimeInMillis();
            long diffDays = diffMs / (24L * 60L * 60L * 1000L);
            if (diffDays > config.getMaxLookbackDays()) {
                lastRunNotes.add("[ERROR] 遡れる最大日数は " + config.getMaxLookbackDays() + " 日です。指定日: " + dateStr);
                return;
            }

            // Decide targets using targetUnion
            List<GuildMessageChannel> targets = new ArrayList<>();
            if (targetUnion != null) {
                if (targetUnion.getType().isThread()) {
                    // Reject threads as direct targets
                    lastRunNotes.add("[ERROR] targetオプションはチャンネルのみ指定できます。スレッドは直接指定できません。親チャンネルを指定してください。");
                    return; // スレッド直接指定エラー
                }
                if (targetUnion.getType().isMessage()) {
                    targets.add(targetUnion.asGuildMessageChannel());
                }
            }
            if (targets.isEmpty()) {
                // 指定されていない場合、Guild内のMONITOR全対象チャンネルを取得
                List<Channels> chs = channelDao.selectChannelArchiveDo(new GuildId(member.getGuild()));
                for (Channels ch : chs) {
                    GuildMessageChannel gmc = jda.getJda().getGuildById(ch.getGuidId().getValue())
                            .getChannelById(GuildMessageChannel.class, ch.getChannelId().getValue());
                    if (gmc != null && !gmc.getType().isThread()) {
                        targets.add(gmc);
                    }
                }
            }

            // Generate for each target channel using the public day-specific interface
            for (GuildMessageChannel gmc : targets) {
                try {
                    runForSpecificDay(gmc, day);
                } catch (Exception e) {
                    lastRunNotes.add("[ERROR] チャンネル " + gmc.getName() + " の日付指定アーカイブ生成に失敗: " + e.getMessage());
                }
            }

            // Push all generated daily files
            if (config.getPushToGitHub() && !generatedFiles.isEmpty()) {
                try {
                    gitHubService.pushHtmlFilesToGitHub(generatedFiles);
                } catch (Exception e) {
                    lastRunNotes.add("[ERROR] GitHubへのプッシュに失敗しました: " + e.getMessage());
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            lastRunNotes.add("[ERROR:予期しない例外が発生] " + e.getMessage());
        }

        member.getJDA().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
    }
    
    public void run(){
        final Calendar beginDate = Calendar.getInstance(DateTimeUtil.JST);
        final Calendar endDate = (Calendar) beginDate.clone();
        final int now = endDate.get(Calendar.HOUR_OF_DAY);
        // Normalize beginDate to 00:00:00.000 of target day
        beginDate.set(Calendar.HOUR_OF_DAY, 0);
        beginDate.set(Calendar.MINUTE, 0);
        beginDate.set(Calendar.SECOND, 0);
        beginDate.set(Calendar.MILLISECOND, 0);
        // Special case: when scheduled at 0:00 JST, archive the previous full day
        if (now == 0) {
            // shift begin to previous day 00:00
            beginDate.add(Calendar.DAY_OF_MONTH, -1);
            // set endDate to previous day 23:59:59.999
            endDate.set(Calendar.YEAR, beginDate.get(Calendar.YEAR));
            endDate.set(Calendar.MONTH, beginDate.get(Calendar.MONTH));
            endDate.set(Calendar.DAY_OF_MONTH, beginDate.get(Calendar.DAY_OF_MONTH));
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
            endDate.set(Calendar.MILLISECOND, 999);
        }
        
        // Clear any previously generated files
        generatedFiles.clear();
        
        List<Guilds> allGuilds = guildDao.selectAll();
        for(Guilds guilds : allGuilds) {
            for(RunsOn on : guilds.getRunsOnList()) {
                if(on.getValue() == now) {
                    List<Channels> chs = channelDao.selectChannelArchiveDo(guilds.getGuildId());
                    for(Channels ch : chs) {
                        GuildMessageChannel parent = 
                                jda.getJda().getGuildById(ch.getGuidId().getValue())
                                .getChannelById(GuildMessageChannel.class, ch.getChannelId().getValue());
                        if (parent == null) {
                            continue;
                        }
                        // Run archive for parent channel
                        run(parent, beginDate, endDate, true);
                        // Also run for active threads under this parent channel
                        runActiveThreadsUnder(parent, beginDate, endDate,true);
                    }
                    
                    // Push all generated files for this guild at once
                    if (config.getPushToGitHub() && !generatedFiles.isEmpty()) {
                        try {
                            gitHubService.pushHtmlFilesToGitHub(generatedFiles);
                        } catch (Exception e) {
                            List<Channels> loggingChannels = channelDao.selectChannelArchiveDo(guilds.getGuildId());
                            System.err.println("Failed to push HTML files to GitHub: " + e.getMessage());
                            e.printStackTrace();
                            for(Channels sendTo : loggingChannels) {
                                jda.getJda().getGuildById(sendTo.getGuidId().getValue())
                                        .getChannelById(GuildMessageChannel.class, sendTo.getChannelId().getValue())
                                        .sendMessage("Failed to push archives to GitHub: " + e.getMessage())
                                        .queue();
                            }
                        }
                        // Clear files after pushing for this guild
                        generatedFiles.clear();
                    }
                }
            }
        }
    }

    /**
     * run for a specific day (00:00 to 23:59:59.999 if past day, or now if today) in JST.
     */
    private void runForSpecificDay(GuildMessageChannel channel, Calendar dayJst) {
        Calendar begin = (Calendar) dayJst.clone();
        begin.set(Calendar.HOUR_OF_DAY, 0);
        begin.set(Calendar.MINUTE, 0);
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) begin.clone();
        String target = DateTimeUtil.date8().format(begin.getTime());
        String today = DateTimeUtil.nowDate8();
        if (today.equals(target)) {
            end = Calendar.getInstance(DateTimeUtil.JST);
        } else {
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            end.set(Calendar.MILLISECOND, 999);
        }
        run(channel, begin, end, false);
        runActiveThreadsUnder(channel, begin, end, false);
    }

    /**
     * Core runner with explicit date range.
     */
    private void run(final GuildMessageChannel channel, final Calendar beginDate, final Calendar endDate, final boolean scheduled) {
        //validate
        Channels targetChInfo = null;
        boolean isThread = channel.getType().isThread();
        
        if (scheduled) {
            // scheduled execution: ensure non-thread channels are MONITOR in DB
            if (isThread == false) {
                List<Channels> activate = channelDao.selectChannelArchiveDo(new GuildId(channel.getGuild()));
                ChannelId targetChannelId = new ChannelId(channel);
                boolean monitored = false;
                for (Channels c : activate) {
                    if (c.getChannelId().equals(targetChannelId)) {
                        monitored = true;
                        targetChInfo = c;
                        break;
                    }
                }
                if (!monitored) {
                    System.out.println(channel + " is not MONITOR on scheduled run");
                    return;
                }
            }
        } else {
            List<Channels> activate = channelDao.selectChannelArchiveDo(new GuildId(channel.getGuild()));
            ChannelId targetChannelId = new ChannelId(channel);
            for(Channels c : activate) {
                if(c.getChannelId().equals(targetChannelId)) {
                    targetChInfo = c;
                    break;
                }
            }
            // Allow threads to be archived even if not registered in Channels
            if(targetChInfo == null && !isThread) {
                lastRunNotes.add("[INFO] targetで指定されたチャンネルはアーカイブ作成対象ではありません: " + "#" + channel.getName());
                System.out.println(channel + " is not a target");
                return;
            }
        }

        // Load guild settings for onRunMessage and onRunURL
        Guilds guildSettings = guildDao.selectGuildInfo(new GuildId(channel.getGuild()));
        OnRunMessageMode msgMode = guildSettings.getOnRunMessage().get();
        
        if ((!isThread) && !isVoiceText(channel) && (msgMode.isStart() || msgMode.isBoth())) {
            channel.sendMessage("This channel is archive target. Start >>>").queue();
        }

        // Retrieve messages differently for normal channels vs threads
        List<MessageInfo> messages;
        Calendar beginForOutput = (Calendar) beginDate.clone();
        if (isThread && channel instanceof ThreadChannel tc) {
            if (tc.isArchived() || tc.isLocked()) {
                long existingEnd = getExistingThreadPageEndMillis(tc);
                long targetEnd = endDate.getTimeInMillis();
                if (existingEnd >= targetEnd - 1) {
                    // Already up-to-date, skip regeneration
                    return;
                }
            }
            messages = getMessagesForThread(tc, endDate);
        } else {
            messages = getMessagesForMessageChannel(channel, beginDate, endDate);
        }
        // sort chronologically
        messages.sort(Comparator.comparing(MessageInfo::getCreatedTimestamp));
        
        // For threads: ensure beginForOutput reflects the earliest message timestamp (after sorting)
        if (isThread && !messages.isEmpty()) {
            try {
                Date first = DateTimeUtil.time().parse(messages.get(0).getCreatedTimestamp());
                Calendar cal = Calendar.getInstance(DateTimeUtil.JST);
                cal.setTime(first);
                beginForOutput = cal;
            } catch (Exception ignore) { /* keep prior beginForOutput */ }
        }
        
        Path generatedFile = fileGenerator.generate(new ChannelInfo(channel), messages, beginForOutput, (Calendar)endDate.clone(), 1);
        generatedFiles.add(generatedFile);
        // Also include the top index.html updated by FileGenerator as a push target (deduplicated)
        Path indexPath = Path.of(config.getOutputPath(), "index.html");
        if (!generatedFiles.contains(indexPath)) {
            generatedFiles.add(indexPath);
        }
        // Include help.html so it will be placed at gh_pages root
        Path helpPath = Path.of(config.getOutputPath(), "help.html");
        if (!generatedFiles.contains(helpPath)) {
            generatedFiles.add(helpPath);
        }
        // Also include the per-channel archives/<channelId>.html updated by FileGenerator (deduplicated)
        // NOTE: For thread channels, the file name is t-<id>.html under archives/<parent>/threads/, not <channelName>.html.
        // Therefore, only add archives/<channelName>.html for non-thread channels and if it exists.
        if (!isThread) {
            Path channelArchivePath = Path.of(config.getOutputPath(), "archives", channel.getId() + ".html");
            if (Files.exists(channelArchivePath) && !generatedFiles.contains(channelArchivePath)) {
                generatedFiles.add(channelArchivePath);
            }
            // Also include daily combined files for all affected dates between begin and end
            try {
                Calendar dayIter = (Calendar) beginDate.clone();
                // Normalize to 00:00:00.000 JST for safe date iteration
                dayIter.set(Calendar.MINUTE, 0);
                dayIter.set(Calendar.SECOND, 0);
                dayIter.set(Calendar.MILLISECOND, 0);
                dayIter.set(Calendar.HOUR_OF_DAY, 0);
                Calendar endDay = (Calendar) endDate.clone();
                // Iterate days inclusively from begin to end
                while (!dayIter.after(endDay)) {
                    String date8 = DateTimeUtil.date8().format(dayIter.getTime());
                    Path dailyPath = Path.of(config.getOutputPath(), "archives", date8, channel.getId() + ".html");
                    if (Files.exists(dailyPath) && !generatedFiles.contains(dailyPath)) {
                        generatedFiles.add(dailyPath);
                    }
                    dayIter.add(Calendar.DAY_OF_MONTH, 1);
                }
            } catch (Exception ignore) { /* best-effort */ }
        }
        // Include thread archive files under archives/<channelId>/threads/*.html
        try {
            Path threadsDir = Path.of(config.getOutputPath(), "archives", channel.getId(), "threads");
            if (Files.exists(threadsDir) && Files.isDirectory(threadsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(threadsDir, "*.html")) {
                    for (Path p : stream) {
                        if (!generatedFiles.contains(p)) {
                            generatedFiles.add(p);
                        }
                    }
                }
            }
            // Include thread index at archives/<parentChannelId>/threads/index.html (for parent channels)
            Path threadIndex = Path.of(config.getOutputPath(), "archives", channel.getId(), "threads", "index.html");
            if (Files.exists(threadIndex) && !generatedFiles.contains(threadIndex)) {
                generatedFiles.add(threadIndex);
            }
            // If the current channel is a thread, also ensure the parent thread index is included
            try {
                if (channel.getType().isThread() && channel instanceof ThreadChannel tc) {
                    String parentId = tc.getParentMessageChannel().getId();
                    Path parentIndex = Path.of(config.getOutputPath(), "archives", parentId, "threads", "index.html");
                    if (Files.exists(parentIndex) && !generatedFiles.contains(parentIndex)) {
                        generatedFiles.add(parentIndex);
                    }
                }
            } catch (Throwable ignore2) { /* ignore */ }
        } catch (Exception ignore) {
            // If scanning fails, skip silently
        }

        // Log only when scheduled to mark last run time
        if (scheduled) {
            try {
                channelDao.logChannelStatus(new ChannelId(channel));
            } catch (Exception ignore) {
                // logging failure should not break archive generation
            }
        }

        if ((!isThread) && !isVoiceText(channel) && (msgMode.isEnd() || msgMode.isBoth())) {
            String endMsg = "archive created. task end <<<";
            if (guildSettings.getOnRunUrl().get().isShare()) {
                try {
                    Calendar urlCal = (Calendar) endDate.clone();
                    if (scheduled && urlCal.get(Calendar.HOUR_OF_DAY) == 0) {
                        urlCal.add(Calendar.DAY_OF_MONTH, -1);
                    }
                    endMsg += "\n" + buildChannelArchiveUrl(channel, DateTimeUtil.formatDate8(urlCal));
                } catch (Exception ignore) { /* ignore URL build failures */ }
            }
            channel.sendMessage(endMsg).queue();
        }
    }
    private void runActiveThreadsUnder(GuildMessageChannel parent, Calendar beginDate, Calendar endDate, boolean scheduled) {
        try {
            if (parent == null) { return; }
            if (parent.getType().isThread()) {
                return; // threads don't have sub-threads; and avoid reentrancy
            }
            TextChannel text = jda.getJda().getChannelById(TextChannel.class, parent.getIdLong());
            if (text == null) { return; }
            List<ThreadChannel> threads = text.getThreadChannels();
            for (ThreadChannel t : threads) {
                run(t, beginDate, endDate, scheduled);
            }
        } catch (Throwable ignore) {
            // best-effort; skip on any error
        }
    }

    /** メッセージ取得（通常のメッセージチャンネル向け） */
    private List<MessageInfo> getMessagesForMessageChannel(GuildMessageChannel channel, Calendar beginDate, Calendar endDate) {
        Instant begin = beginDate.toInstant();
        Instant end = endDate.toInstant();
        return collectMessages(channel, begin, end);
    }

    /** メッセージ取得（スレッド向け：過去日も含め全期間を1ファイルに統一） */
    private List<MessageInfo> getMessagesForThread(ThreadChannel thread, Calendar endDate) {
        // No begin
        Instant end = endDate.toInstant();
        return collectMessages(thread, null, end);
    }

    /**
     * 汎用メッセージ取得メソッド
     * @param channel target channel (message channel or thread)
     * @param beginInstantOrNull null to disable lower bound; otherwise messages strictly after or at begin depending on exclusiveBegin
     */
    private List<MessageInfo> collectMessages(
            GuildMessageChannel channel,
            Instant beginInstantOrNull,
            Instant endInstant
    ) {
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();
        boolean breakEarlyByBegin = (beginInstantOrNull != null);
        
        GuildId guildId = new GuildId(channel.getGuild());
        Guilds guildInfo = guildDao.selectGuildInfo(guildId);
        int anonCycle = guildInfo.getAnonCycle().getValue();
        if (anonCycle < 1 || 24 < anonCycle) { anonCycle = 24; }
        final int finalAnonCycle = anonCycle;
        try {
            MessageHistory history = channel.getHistory();
            while (true) {
                List<Message> batch = history.retrievePast(100).complete();
                if (batch == null || batch.isEmpty()) { break; }
                Message oldest = batch.get(batch.size() - 1);
                Instant oldestInstant = oldest.getTimeCreated().toInstant();
                batch.stream()
                        .filter(msg -> {
                            boolean afterBeginOk = (breakEarlyByBegin == false) || msg.getTimeCreated().toInstant().isAfter(beginInstantOrNull);
                            boolean beforeEndOk = msg.getTimeCreated().toInstant().isBefore(endInstant);
                            return afterBeginOk && beforeEndOk;
                        })
                        .forEach(msg -> {
                            Users author;
                            if (msg.getMember() == null) {
                                // bot or non-member
                                author = new Users(msg.getAuthor(), channel.getGuild());
                                UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
                                author.setAnonStats(new AnonStats(anonStatus));
                            } else {
                                // member
                                author = new Users(msg.getMember());
                                UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
                                author.setAnonStats(new AnonStats(anonStatus));
                            }
                            if (!marked.contains(author)) {
                                usersDao.upsertUserInfo(author);
                                marked.add(author);
                            }
                            Date msgDate = Date.from(msg.getTimeCreated().toInstant());
                            Calendar calJst = Calendar.getInstance(DateTimeUtil.JST);
                            calJst.setTime(msgDate);
                            int hour = calJst.get(Calendar.HOUR_OF_DAY);
                            int cycleIndex = hour / finalAnonCycle;
                            String dateStr = DateTimeUtil.date8().format(msgDate);
                            String scopeKey = guildId.toString() + "-" + dateStr + "-c" + cycleIndex + "-n" + finalAnonCycle;
                            messages.add(new MessageInfo(msg, author, scopeKey));
                        });
                if (breakEarlyByBegin && oldestInstant.isBefore(beginInstantOrNull)) {
                    break;
                }
            }
        } catch (Throwable ignore) {
            // best-effort
        }
        return messages;
    }

    private boolean isVoiceText(GuildMessageChannel ch) {
        if (ch == null) { return false; }
        return (ch instanceof VoiceChannel || ch instanceof StageChannel);
    }

    private long getExistingThreadPageEndMillis(ThreadChannel tc) {
        try {
            String parentId = tc.getParentMessageChannel().getId();
            Path out = Path.of(config.getOutputPath(), "archives", parentId, "threads", "t-" + tc.getId() + ".html");
            if (!Files.exists(out)) { return 0L; }
            String html = Files.readString(out, StandardCharsets.UTF_8);
            // <meta name="d2h-thread-end-epoch" content="...">
            Pattern p = Pattern.compile("<meta\\s+[^>]*name=\\\"d2h-thread-end-epoch\\\"[^>]*content=\\\"(\\d+)\\\"", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) {
                try { return Long.parseLong(m.group(1)); } catch (Exception ignore) { /* fallback */ }
            }
        
        } catch (Exception ignore) { }
        return 0L;
    }

    private String buildChannelArchiveUrl(GuildMessageChannel channel, String date8) {
        if (gitConfig == null || gitConfig.getRepo() == null) {
            return "";
        }
        String owner = gitConfig.getRepo().getOwner();
        String name = gitConfig.getRepo().getName();
        if (owner == null || name == null) {
            return "";
        }
        String base = "https://" + owner + ".github.io/" + name + "/archives/";
        return base + date8 + "/" + channel.getId() + ".html";
    }

    @Override
    public String afterRunMessage() {
        if (lastRunNotes.size() == 0) {
            // success
            if (config.getPushToGitHub()) {
                return "bot completed making archive and pushing all files to GitHub repository";
            } else {
                return "bot completed making archive";
            }
        } else {
            // fail
            StringBuilder sb = new StringBuilder();
            for (String n : lastRunNotes) {
                sb.append(n).append("\n");
            }
            sb.setLength(sb.length() - 1); // remove the last "\n"
            return sb.toString();
        }
    }
}
