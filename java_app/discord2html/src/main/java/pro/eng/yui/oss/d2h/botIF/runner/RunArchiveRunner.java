package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
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
import java.util.TimeZone;

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
            boolean isTargetChannelMarked = false;
            for (OptionMapping om : options) {
                if ("target".equals(om.getName())) {
                    // CHANNEL型データ対応
                    GuildChannelUnion selected = om.getAsChannel();
                    if (selected != null) {
                        if (selected.getType().isThread()) {
                            // threadの直接指定をNGにする
                            isTargetChannelMarked = true; // prevent full-guild run on invalid target
                            try {
                                List<Channels> loggingChannels = channelDao.selectChannelArchiveDo(new GuildId(member.getGuild()));
                                for (Channels sendTo : loggingChannels) {
                                    jda.getJda().getGuildById(sendTo.getGuidId().getValue())
                                            .getChannelById(GuildMessageChannel.class, sendTo.getChannelId().getValue())
                                            .sendMessage("targetオプションはチャンネルのみ指定できます。スレッドは直接指定できません。親チャンネルを指定してください。")
                                            .queue();
                                }
                            } catch (Exception ignore) { /* ignore */ }
                            continue;
                        }
                        if (selected.getType().isMessage()) {
                            GuildMessageChannel gmc = selected.asGuildMessageChannel();
                            if (gmc != null) {
                                isTargetChannelMarked = true;
                                run(gmc, false);
                                runActiveThreadsUnder(gmc, false);
                                continue; // processed target via channel option
                            }
                        }
                    }
                }
            }
            if (isTargetChannelMarked == false) {
                for (GuildMessageChannel c : member.getGuild().getTextChannels()) {
                    run(c, false);
                    runActiveThreadsUnder(c, false);
                }
                for (GuildMessageChannel v : member.getGuild().getVoiceChannels()) {
                    run(v, false);
                }
            }
            
            // Push all generated files at once
            if (config.getPushToGitHub() && !generatedFiles.isEmpty()) {
                try {
                    gitHubService.pushHtmlFilesToGitHub(generatedFiles);
                } catch (Exception e) {
                    List<Channels> loggingChannels = channelDao.selectChannelArchiveDo(new GuildId(member.getGuild()));
                    System.err.println("Failed to push HTML files to GitHub: " + e.getMessage());
                    e.printStackTrace();
                    for(Channels sendTo : loggingChannels) {
                        jda.getJda().getGuildById(sendTo.getGuidId().getValue())
                                .getChannelById(GuildMessageChannel.class, sendTo.getChannelId().getValue())
                                .sendMessage("Failed to push archives to GitHub: " + e.getMessage())
                                .queue();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        member.getJDA().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
    }
    
    public void run(){
        final int now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).get(Calendar.HOUR_OF_DAY);
        
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
                        run(parent, true);
                        // Also run for active threads under this parent channel
                        runActiveThreadsUnder(parent, true);
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
     * 処理本体
     * @param scheduled true: 定期実行, false: 手動実行
     */
    private void run(final GuildMessageChannel channel, final boolean scheduled){
        //validate
        Channels targetChInfo = null;
        boolean isThread = false;
        try {
            isThread = channel.getType() != null && channel.getType().isThread();
        } catch (Exception ignore) {
            // keep default false
        }
        if (scheduled) {
            // scheduled execution: ensure non-thread channels are MONITOR in DB
            if (channel.getType().isThread() == false) {
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
                System.out.println(channel + " is not a target");
                return;
            }
        }

        // Load guild settings for onRunMessage and onRunURL
        Guilds guildSettings = guildDao.selectGuildInfo(new GuildId(channel.getGuild()));
        OnRunMessageMode msgMode = guildSettings.getOnRunMessage().get();
        
        if ((!isThread) && (msgMode.isStart() || msgMode.isBoth())) {
            channel.sendMessage("This channel is archive target. Start >>>").queue();
        }

        // Determine begin/end using guild scheduled hours from DB (JST)
        Calendar nowJst = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        Calendar endDate;
        Calendar beginDate;
        if (scheduled && nowJst.get(Calendar.HOUR_OF_DAY) == 0) {
            // At 00:00 on day n, capture full previous day (d-1 00:00:00 to d-1 23:59:59.999)
            endDate = (Calendar) nowJst.clone();
            endDate.set(Calendar.MINUTE, 0);
            endDate.set(Calendar.SECOND, 0);
            endDate.set(Calendar.MILLISECOND, 0);

            beginDate = (Calendar) endDate.clone();
            
            beginDate.add(Calendar.DAY_OF_MONTH, -1);
            endDate.add(Calendar.MILLISECOND, -1);

        } else {
            // Manual run: always archive from today's 00:00 to now (JST)
            endDate = nowJst;
            beginDate = (Calendar) nowJst.clone();
            beginDate.set(Calendar.HOUR_OF_DAY, 0);
            beginDate.set(Calendar.MINUTE, 0);
            beginDate.set(Calendar.SECOND, 0);
            beginDate.set(Calendar.MILLISECOND, 0);
        }

        // Retrieve messages differently for normal channels vs threads
        List<MessageInfo> messages;
        Calendar beginForOutput = (Calendar) beginDate.clone();
        if (isThread && channel instanceof ThreadChannel) {
            messages = getMessagesForThread((ThreadChannel) channel, endDate);
            // adjust begin date to earliest message for display if available
            if (!messages.isEmpty()) {
                try {
                    Date first = DateTimeUtil.time().parse(messages.get(0).getCreatedTimestamp());
                    Calendar cal = Calendar.getInstance(DateTimeUtil.JST);
                    cal.setTime(first);
                    beginForOutput = cal;
                } catch (Exception ignore) { /* leave beginForOutput as is */ }
            }
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
            // Include thread index at archives/threads/<channel>/index.html
            Path threadIndex = Path.of(config.getOutputPath(), "archives", "threads", channel.getName(), "index.html");
            if (Files.exists(threadIndex) && !generatedFiles.contains(threadIndex)) {
                generatedFiles.add(threadIndex);
            }
            // If current channel is a thread, also ensure the parent thread index is included
            try {
                if (channel.getType() != null && channel.getType().isThread()) {
                    ThreadChannel tc = (ThreadChannel) channel;
                    if (tc.getParentMessageChannel() != null) {
                        String parentId = tc.getParentMessageChannel().getId();
                        Path parentIndex = Path.of(config.getOutputPath(), "archives", parentId, "threads", "index.html");
                        if (Files.exists(parentIndex) && !generatedFiles.contains(parentIndex)) {
                            generatedFiles.add(parentIndex);
                        }
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

        if ((!isThread) && (msgMode.isEnd() || msgMode.isBoth())) {
            String endMsg = "archive created. task end <<<";
            if (guildSettings.getOnRunUrl().get().isShare()) {
                try {
                    endMsg += "\n" + buildChannelArchiveUrl(channel, DateTimeUtil.formatDate8(endDate));
                } catch (Exception ignore) { /* ignore URL build failures */ }
            }
            channel.sendMessage(endMsg).queue();
        }
    }

    private void runActiveThreadsUnder(GuildMessageChannel parent, boolean scheduled) {
        try {
            if (parent == null) { return; }
            if (parent.getType().isThread()) {
                return; // threads don't have sub-threads; and avoid reentrancy
            }
            TextChannel text = jda.getJda().getChannelById(TextChannel.class, parent.getIdLong());
            if (text == null) { return; }
            List<ThreadChannel> threads = text.getThreadChannels();
            for (ThreadChannel t : threads) {
                if (!t.isArchived()) {
                    run(t, scheduled);
                }
            }
        } catch (Throwable ignore) {
            // best-effort; skip on any error
        }
    }

    // メッセージ取得（通常のメッセージチャンネル向け）
    private List<MessageInfo> getMessagesForMessageChannel(GuildMessageChannel channel, Calendar beginDate, Calendar endDate) {
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();
        var history = channel.getHistory();
        GuildId guildId = new GuildId(channel.getGuild());
        Guilds guildInfo = guildDao.selectGuildInfo(guildId);
        int anonCycle = guildInfo.getAnonCycle().getValue();
        if (anonCycle < 1 || 24 < anonCycle) {
            anonCycle = 24;
        }
        final int finalAnonCycle = anonCycle;
        boolean more = true;
        while (more) {
            var batch = history.retrievePast(100).complete();
            if (batch == null || batch.isEmpty()) {
                break;
            }
            var oldest = batch.get(batch.size() - 1);
            var oldestInstant = oldest.getTimeCreated().toInstant();
            final Instant beginInstant = beginDate.toInstant();
            final Instant endInstant = endDate.toInstant();
            batch.stream()
                    .filter(msg -> msg.getTimeCreated().toInstant().isAfter(beginInstant)
                            && msg.getTimeCreated().toInstant().isBefore(endInstant))
                    .forEach(msg -> {
                        Users author = null;
                        if (msg.getMember() != null) {
                            author = new Users(msg.getMember());
                            UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
                            author.setAnonStats(new AnonStats(anonStatus));
                        } else {
                            if (msg.getAuthor() != null) {
                                author = new Users(msg.getAuthor(), channel.getGuild());
                                UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
                                author.setAnonStats(new AnonStats(anonStatus));
                            }
                        }
                        if (author != null) {
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
                        }
                    });
            if (!oldestInstant.isAfter(beginInstant)) {
                more = false;
            }
        }
        return messages;
    }

    // メッセージ取得（スレッド向け：過去日も含め全期間を1ファイルに統一）
    private List<MessageInfo> getMessagesForThread(ThreadChannel thread, Calendar endDate) {
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();
        var history = thread.getHistory();
        GuildId guildId = new GuildId(thread.getGuild());
        Guilds guildInfo = guildDao.selectGuildInfo(guildId);
        int anonCycle = guildInfo.getAnonCycle().getValue();
        if (anonCycle < 1 || 24 < anonCycle) {
            anonCycle = 24;
        }
        final int finalAnonCycle = anonCycle;
        while (true) {
            var batch = history.retrievePast(100).complete();
            if (batch == null || batch.isEmpty()) {
                break;
            }
            // Snapshot end instant for stable inclusive-end filtering
            final java.time.Instant endInstant = endDate.toInstant();
            batch.stream()
                    .filter(msg -> !msg.getTimeCreated().toInstant().isAfter(endInstant))
                    .forEach(msg -> {
                        Users author = null;
                        if (msg.getMember() != null) {
                            author = new Users(msg.getMember());
                            UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
                            author.setAnonStats(new AnonStats(anonStatus));
                        } else {
                            if (msg.getAuthor() != null) {
                                author = new Users(msg.getAuthor(), thread.getGuild());
                                UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
                                author.setAnonStats(new AnonStats(anonStatus));
                            }
                        }
                        if (author != null) {
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
                        }
                    });
            // do not break early; continue until all past messages exhausted
        }
        return messages;
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
        String base;
        if (config.getPushToGitHub()) {
            base = "bot completed making archive and pushing all files to GitHub repository";
        } else {
            base = "bot completed making archive";
        }
        StringBuilder sb = new StringBuilder(base);
        if (lastRunNotes.size() > 0) {
            sb.append("\n");
            for (String n : lastRunNotes) {
                sb.append(n).append("\n");
            }
            sb.setLength(sb.length() - 1); // remove the last "\n"
        }
        return sb.toString();
    }
}
