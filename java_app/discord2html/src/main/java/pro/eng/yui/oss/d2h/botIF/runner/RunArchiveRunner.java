package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import pro.eng.yui.oss.d2h.botIF.DiscordBotUtils;
import pro.eng.yui.oss.d2h.html.FileGenerateUtil;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBot;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.github.GitHubService;
import pro.eng.yui.oss.d2h.github.GitConfig;
import pro.eng.yui.oss.d2h.html.ChannelInfo;
import pro.eng.yui.oss.d2h.html.FileGenerateService;
import pro.eng.yui.oss.d2h.html.MessageInfo;
import pro.eng.yui.oss.d2h.consts.OnRunMessageMode;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.io.IOException;

@Component
public class RunArchiveRunner implements IRunner {
    // notes for the latest manual command run (validation/errors)
    private final List<String> lastRunNotes = new ArrayList<>();
    
    private final ApplicationConfig config;
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    private final DiscordJdaProvider jda;
    private final FileGenerateService fileGenerator;
    private final FileGenerateUtil fileUtil;
    private final GitHubService gitHubService;
    private final GitConfig gitConfig;
    private final List<Path> generatedFiles = new ArrayList<>();
    private final DiscordBotUtils discordBotUtils;
    // private final IndexGenerator indexGenerator;

    @Autowired
    public RunArchiveRunner(
            ApplicationConfig c,
            GuildsDAO g, ChannelsDAO ch,
            DiscordJdaProvider j, FileGenerateService fileGenerator, FileGenerateUtil fileUtil,
            GitHubService gitHubService, GitConfig gitConfig, DiscordBotUtils discordBotUtils){
        this.config = c;
        this.guildDao = g;
        this.channelDao = ch;
        this.jda = j;
        this.fileGenerator = fileGenerator;
        this.fileUtil = fileUtil;
        this.gitHubService = gitHubService;
        this.gitConfig = gitConfig;
        this.discordBotUtils = discordBotUtils;
    }

    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.D2H_ADMIN;
    }
    
    public void run(GuildId target, List<OptionMapping> options){
        jda.getJda().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);
        
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
            List<GuildChannel> targets = new ArrayList<>();
            if (targetUnion != null) {
                if (targetUnion.getType().isThread()) {
                    // Reject threads as direct targets
                    lastRunNotes.add("[ERROR] targetオプションはチャンネルのみ指定できます。スレッドは直接指定できません。親チャンネルを指定してください。");
                    return; // スレッド直接指定エラー
                }
                targets.add(targetUnion.asStandardGuildChannel());
            }
            if (targets.isEmpty()) {
                // 指定されていない場合、Guild内のMONITOR全対象チャンネルを取得
                List<Channels> chs = channelDao.selectChannelArchiveDo(target);
                for (Channels ch : chs) {
                    GuildChannel gc = jda.getJda().getGuildById(ch.getGuidId().getValue())
                            .getChannelById(GuildChannel.class, ch.getChannelId().getValue());
                    if (gc != null && !gc.getType().isThread()) {
                        targets.add(gc);
                    }
                }
            }

            // Generate for each target channel using the public day-specific interface
            for (GuildChannel gc : targets) {
                try {
                    runForSpecificDay(gc, day);
                } catch (Exception e) {
                    String msg = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getName();
                    lastRunNotes.add("[ERROR] チャンネル " + gc.getName() + " の日付指定アーカイブ生成に失敗: " + msg);
                }
            }

            // Generate help page only once per run
            try {
                fileGenerator.regenerateHelpPage(target);
                Path help = config.getOutputPath().resolve("help.html");
                if (!generatedFiles.contains(help) && Files.exists(help)) {
                    generatedFiles.add(help);
                }
            } catch (IOException ignore) { }

            // Push all generated daily files
            if (config.getPushToGitHub() && !generatedFiles.isEmpty()) {
                try {
                    gitHubService.pushHtmlFilesToGitHub(generatedFiles);
                } catch (Exception e) {
                    lastRunNotes.add("[ERROR] GitHubへのプッシュに失敗しました: " + e.getMessage());
                }
            } else if (config.getPushToGitHub() && generatedFiles.isEmpty()) {
                lastRunNotes.add("[INFO] 変更されたアーカイブがありませんでした。GitHubへのプッシュはスキップしました。");
            }
        } catch(Exception e) {
            e.printStackTrace();
            lastRunNotes.add("[ERROR:予期しない例外が発生] " + e.getMessage());
        }

        jda.getJda().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
    }
    
    /** バッチ実行用I/F */
    public void run(){
        try {
            jda.getJda().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);
        } catch (Exception ignore) {
            // 本体処理に影響を与えないように
        }
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
                        GuildChannel parent =
                                jda.getJda().getGuildById(ch.getGuidId().getValue())
                                        .getChannelById(GuildChannel.class, ch.getChannelId().getValue());
                        if (parent != null) {
                            if(parent instanceof IThreadContainer container) {
                                runActiveThreadsUnder(container, beginDate, endDate, true);
                            }
                            if(!(parent instanceof ForumChannel)) {
                                run(parent, beginDate, endDate, true);
                            }
                        }
                    }
                    
                    // Generate help page once per scheduled guild run
                    try {
                        fileGenerator.regenerateHelpPage(guilds.getGuildId());
                        Path help = config.getOutputPath().resolve("help.html");
                        if (!generatedFiles.contains(help) && Files.exists(help)) {
                            generatedFiles.add(help);
                        }
                    } catch (IOException ignore) { }
                    
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
        try {
            jda.getJda().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
        } catch (Exception ignore) {
            // 処理本体に影響させないため
        }
    }

    /**
     * run for a specific day (00:00 to 23:59:59.999 if past day, or now if today) in JST.
     */
    private void runForSpecificDay(GuildChannel channel, Calendar dayJst) {
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
        if(channel instanceof IThreadContainer container) {
            runActiveThreadsUnder(container, begin, end, false);
        }
        if(channel instanceof GuildMessageChannel msgCh) {
            run(msgCh, begin, end, false);
        }
    }


    /**
     * Core runner with explicit date range.
     */
    private void run(final GuildChannel channel, final Calendar beginDate, final Calendar endDate, final boolean scheduled) {
        //validate
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
                        break;
                    }
                }
                if (!monitored) {
                    System.out.println(channel + " is not MONITOR on scheduled run");
                    return;
                }
            }
        } else {
            Channels targetChInfo = null;
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
        
        if ((!isThread) && !isVoiceText(channel) && channel instanceof GuildMessageChannel msgCh && (msgMode.isStart() || msgMode.isBoth())) {
            msgCh.sendMessageEmbeds(discordBotUtils.buildStatusEmbed(INFO, "This channel is archive target. Start >>>")).queue();
        }

        // Retrieve messages differently for normal channels vs threads
        List<MessageInfo> messages = new ArrayList<>();
        Calendar beginForOutput = (Calendar) beginDate.clone();
        if (isThread && channel instanceof ThreadChannel tc) {
            messages = getMessagesForThread(tc, endDate);
        } else if (channel instanceof GuildMessageChannel msgCh){
            messages = getMessagesForMessageChannel(msgCh, beginDate, endDate);
        }
        // sort chronologically
        messages.sort(Comparator.comparing(MessageInfo::getCreatedTimestamp));
        
        Calendar endForOutput = (Calendar) endDate.clone();
        if (isThread && !messages.isEmpty()) {
            try {
                Date first = DateTimeUtil.time().parse(messages.get(0).getCreatedTimestamp());
                Calendar calBegin = Calendar.getInstance(DateTimeUtil.JST);
                calBegin.setTime(first);
                beginForOutput = calBegin;
            } catch (Exception ignore) { /* keep prior beginForOutput */ }
            try {
                Date last = DateTimeUtil.time().parse(messages.get(messages.size() - 1).getCreatedTimestamp());
                Calendar calEnd = Calendar.getInstance(DateTimeUtil.JST);
                calEnd.setTime(last);
                endForOutput = calEnd;
            } catch (Exception ignore) { /* keep prior endForOutput */ }
        }
        
        ChannelInfo chInfo = new ChannelInfo(channel);
        List<Path> outs = fileGenerator.generate(chInfo, messages, beginForOutput, endForOutput, 1);
        for (Path p : outs) {
            if (!generatedFiles.contains(p)) {
                generatedFiles.add(p);
            }
        }

        if (!isThread) {
            Path channelArchivePath = config.getOutputPath()
                    .resolve("archives").resolve(channel.getId() + ".html");
            if (Files.exists(channelArchivePath) && !generatedFiles.contains(channelArchivePath)) {
                generatedFiles.add(channelArchivePath);
            }
            // Include channel pinned list page archives/<channelId>/pin.html
            try {
                Path channelPinPath = config.getOutputPath()
                        .resolve("archives").resolve(channel.getId()).resolve("pin.html");
                if (Files.exists(channelPinPath) && !generatedFiles.contains(channelPinPath)) {
                    generatedFiles.add(channelPinPath);
                }
            } catch (Exception ignore) { /* best-effort */ }
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
                    Path dailyPath = config.getOutputPath()
                            .resolve("archives").resolve(date8).resolve(channel.getId() + ".html");
                    if (Files.exists(dailyPath) && !generatedFiles.contains(dailyPath)) {
                        generatedFiles.add(dailyPath);
                    }
                    dayIter.add(Calendar.DAY_OF_MONTH, 1);
                }
            } catch (Exception ignore) { /* best-effort */ }
        }
        // Include thread archive files under archives/<channelId>/threads/*.html
        try {
            Path threadsDir = config.getOutputPath()
                    .resolve("archives").resolve(channel.getId()).resolve("threads");
            if (Files.exists(threadsDir) && Files.isDirectory(threadsDir)) {
                // Include direct thread HTML files (t-<id>.html, index.html)
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(threadsDir, "*.html")) {
                    for (Path p : stream) {
                        if (!generatedFiles.contains(p)) {
                            generatedFiles.add(p);
                        }
                    }
                }
                // Explicitly include pin.html under each thread subdirectory: threads/t-<id>/pin.html
                try (DirectoryStream<Path> subdirs = Files.newDirectoryStream(threadsDir)) {
                    for (Path sub : subdirs) {
                        try {
                            if (Files.isDirectory(sub) && sub.getFileName().toString().startsWith("t-")) {
                                Path pin = sub.resolve("pin.html");
                                if (Files.exists(pin) && !generatedFiles.contains(pin)) {
                                    generatedFiles.add(pin);
                                }
                            }
                        } catch (Exception ignoreOne) { /* best-effort per entry */ }
                    }
                }
            }
            // Include thread index at archives/<parentChannelId>/threads/index.html (for parent channels)
            Path threadIndex = config.getOutputPath()
                    .resolve("archives").resolve(channel.getId()).resolve("threads")
                    .resolve("index.html");
            if (Files.exists(threadIndex) && !generatedFiles.contains(threadIndex)) {
                generatedFiles.add(threadIndex);
            }
            // If the current channel is a thread, also ensure the parent thread index and this thread's pin.html are included
            try {
                if (channel.getType().isThread() && channel instanceof ThreadChannel tc) {
                    String parentId = tc.getParentChannel().getId();
                    // parent thread index
                    Path parentIndex = config.getOutputPath()
                            .resolve("archives").resolve(parentId).resolve("threads")
                            .resolve("index.html");
                    if (Files.exists(parentIndex) && !generatedFiles.contains(parentIndex)) {
                        generatedFiles.add(parentIndex);
                    }
                    // this thread's pin.html at archives/<parentId>/threads/t-<threadId>/pin.html
                    Path thisThreadPin = config.getOutputPath()
                            .resolve("archives").resolve(parentId).resolve("threads")
                            .resolve("t-" + tc.getId()).resolve("pin.html");
                    if (Files.exists(thisThreadPin) && !generatedFiles.contains(thisThreadPin)) {
                        generatedFiles.add(thisThreadPin);
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

        if ((!isThread) && !isVoiceText(channel) && channel instanceof GuildMessageChannel msgCh && (msgMode.isEnd() || msgMode.isBoth())) {
            String endMsg = "archive created. task end <<<";
            if (guildSettings.getOnRunUrl().get().isShare()) {
                try {
                    Calendar urlCal = (Calendar) endDate.clone();
                    if (scheduled && urlCal.get(Calendar.HOUR_OF_DAY) == 0) {
                        urlCal.add(Calendar.DAY_OF_MONTH, -1);
                    }
                    endMsg += "\n" + buildChannelArchiveUrl(msgCh, DateTimeUtil.formatDate8(urlCal));
                } catch (Exception ignore) { /* ignore URL build failures */ }
            }
            msgCh.sendMessageEmbeds(discordBotUtils.buildStatusEmbed(SUCCESS, endMsg)).queue();
        }
    }
    private void runActiveThreadsUnder(IThreadContainer parent, Calendar beginDate, Calendar endDate, boolean scheduled) {
        try {
            if (parent == null) { return; }
            List<ThreadChannel> threads = new ArrayList<>(parent.getThreadChannels());
            threads.addAll(parent.retrieveArchivedPublicThreadChannels().complete());
            for (ThreadChannel t : threads) {
                run(t, beginDate, endDate, scheduled);
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
            // best-effort; skip on any error
        }
    }

    /** メッセージ取得（通常のメッセージチャンネル向け） */
    private List<MessageInfo> getMessagesForMessageChannel(GuildMessageChannel channel, Calendar beginDate, Calendar endDate) {
        return fileUtil.fetchMessagesForDaily(channel, beginDate, endDate);
    }

    /** メッセージ取得（スレッド向け：過去日も含め全期間を1ファイルに統一） */
    private List<MessageInfo> getMessagesForThread(ThreadChannel thread, Calendar endDate) {
        // スレッドの開始時刻を下限として取得
        Calendar begin = Calendar.getInstance(DateTimeUtil.JST);
        begin.setTime(Date.from(thread.getTimeCreated().toInstant()));
        return fileUtil.fetchMessagesForDaily(thread, begin, endDate);
    }

    private boolean isVoiceText(GuildChannel ch) {
        if (ch == null) { return false; }
        return (ch instanceof VoiceChannel || ch instanceof StageChannel);
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
    public MessageEmbed afterRunMessage() {
        if (lastRunNotes.size() == 0) {
            // success
            if (config.getPushToGitHub()) {
                return discordBotUtils.buildStatusEmbed(SUCCESS, "bot completed making archive and pushing all files to GitHub repository");
            } else {
                return discordBotUtils.buildStatusEmbed(SUCCESS, "bot completed making archive");
            }
        } else {
            // fail
            StringBuilder sb = new StringBuilder();
            for (String n : lastRunNotes) {
                sb.append(n).append("\n");
            }
            sb.setLength(sb.length() - 1); // remove the last "\n"
            return discordBotUtils.buildStatusEmbed(WARN, sb.toString());
        }
    }
}
