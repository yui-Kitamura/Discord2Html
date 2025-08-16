package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
import pro.eng.yui.oss.d2h.db.field.AnonStats;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.db.model.Users;
import pro.eng.yui.oss.d2h.github.GitHubService;
import pro.eng.yui.oss.d2h.html.ChannelInfo;
import pro.eng.yui.oss.d2h.html.FileGenerator;
import pro.eng.yui.oss.d2h.html.MessageInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

@Component
public class RunArchiveRunner implements IRunner {
    
    private final ApplicationConfig config;
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    private final UsersDAO usersDao;
    private final AnonStatsDAO anonStatsDao;
    private final DiscordJdaProvider jda;
    private final FileGenerator fileGenerator;
    private final GitHubService gitHubService;
    private final List<Path> generatedFiles = new ArrayList<>();

    @Autowired
    public RunArchiveRunner(
            ApplicationConfig c,
            GuildsDAO g, ChannelsDAO ch, UsersDAO u, AnonStatsDAO a,
            DiscordJdaProvider j, FileGenerator fileGenerator, GitHubService gitHubService
    ){
        this.config = c;
        this.guildDao = g;
        this.channelDao = ch;
        this.usersDao = u;
        this.anonStatsDao = a;
        this.jda = j;
        this.fileGenerator = fileGenerator;
        this.gitHubService = gitHubService;
    }
    
    public void run(Member member, List<OptionMapping> options){
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);
        
        // Clear any previously generated files
        generatedFiles.clear();

        try {
            boolean isTargetChannelMarked = false;
            for (OptionMapping om : options) {
                if ("target".equals(om.getName())) {
                    // CHANNEL型データ対応
                    try {
                        GuildChannelUnion selected = om.getAsChannel();
                        if (selected != null && selected.getType().isMessage()) {
                            GuildMessageChannel gmc = selected.asGuildMessageChannel();
                            if (gmc != null) {
                                isTargetChannelMarked = true;
                                run(gmc, false);
                                continue; // processed target via channel option
                            }
                        }
                    } catch (Exception unexpected) {
                        throw new RuntimeException(unexpected); 
                    }
                }
            }
            if (isTargetChannelMarked == false) {
                for (GuildMessageChannel c : member.getGuild().getTextChannels()) {
                    run(c, false);
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
        final int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        // Clear any previously generated files
        generatedFiles.clear();
        
        List<Guilds> allGuilds = guildDao.selectAll();
        for(Guilds guilds : allGuilds) {
            for(RunsOn on : guilds.getRunsOn()) {
                if(on.getValue() == now) {
                    List<Channels> chs = channelDao.selectAllInGuild(guilds.getGuildId());
                    for(Channels ch : chs) {
                        run(jda.getJda().getGuildById(ch.getGuidId().getValue()).getChannelById(GuildMessageChannel.class, ch.getChannelId().getValue()), true);
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
    private void run(GuildMessageChannel channel, boolean scheduled){
        //validate
        Channels targetChInfo = null;
        List<Channels> activate = channelDao.selectChannelArchiveDo(new GuildId(channel.getGuild()));
        ChannelId targetChannelId = new ChannelId(channel);
        for(Channels c : activate) {
            if(c.getChannelId().equals(targetChannelId)) {
                targetChInfo = c;
                break;
            }
        }
        if(targetChInfo == null) {
            System.out.println(channel + " is not a target");
            return;
        }

        channel.sendMessage("This channel is archive target. Start >>>").queue();

        // Determine begin/end using guild scheduled hours from DB
        Calendar endDate = Calendar.getInstance();
        Calendar beginDate = getPreviousScheduledTime(endDate, new GuildId(channel.getGuild()));

        // Collect messages after beginDate handling more than 100 via pagination
        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();
        var history = channel.getHistory();
        boolean more = true;
        while (more) {
            var batch = history.retrievePast(100).complete();
            if (batch == null || batch.isEmpty()) {
                break;
            }
            // If the oldest message in this batch is before beginDate, we can stop after filtering
            var oldest = batch.get(batch.size() - 1);
            var oldestInstant = oldest.getTimeCreated().toInstant();
            // Filter only messages within window and collect authors/anon
            batch.stream()
                    .filter(msg -> msg.getTimeCreated().toInstant().isAfter(beginDate.toInstant())
                            && msg.getTimeCreated().toInstant().isBefore(endDate.toInstant()))
                    .forEach(msg -> {
                        Users author = null;
                        if (msg.getMember() != null) {
                            author = new Users(msg.getMember());
                            // Determine anonymization based on member roles/settings
                            UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
                            author.setAnonStats(new AnonStats(anonStatus));
                        } else {
                            // Webhook/Bot or system-like messages (no Member)
                            if (msg.getAuthor() != null) {
                                author = new Users(msg.getAuthor(), channel.getGuild());
                                // For bots/webhooks, force OPEN (not anonymized)
                                UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
                                author.setAnonStats(new AnonStats(anonStatus));
                            }
                        }
                        if (author != null) {
                            if (!marked.contains(author)) {
                                usersDao.upsertUserInfo(author);
                                marked.add(author);
                            }
                            messages.add(new MessageInfo(msg, author));
                        }
                    });
            if (!oldestInstant.isAfter(beginDate.toInstant())) {
                // we have reached messages at/before beginDate; stop paging
                more = false;
            }
        }
        // sort chronologically (createdTimestamp format is lexicographically sortable)
        messages.sort(Comparator.comparing(MessageInfo::getCreatedTimestamp));

        Path generatedFile = fileGenerator.generate(new ChannelInfo(channel), messages, beginDate, endDate, 1);
        generatedFiles.add(generatedFile);
        // Also include the top index.html updated by FileGenerator as a push target (deduplicated)
        Path indexPath = Path.of(config.getOutputPath(), "index.html");
        if (!generatedFiles.contains(indexPath)) {
            generatedFiles.add(indexPath);
        }
        // Also include the per-channel archives/<channel>.html updated by FileGenerator (deduplicated)
        Path channelArchivePath = Path.of(config.getOutputPath(), "archives", channel.getName() + ".html");
        if (!generatedFiles.contains(channelArchivePath)) {
            generatedFiles.add(channelArchivePath);
        }

        // Log only when scheduled to mark last run time
        if (scheduled) {
            try {
                channelDao.logChannelStatus(new ChannelId(channel));
            } catch (Exception ignore) {
                // logging failure should not break archive generation
            }
        }

        channel.sendMessage("archive created. task end <<<").queue();
    }

    private Calendar getPreviousScheduledTime(Calendar now, GuildId guildId) {
        Calendar endCopy = (Calendar) now.clone();
        List<RunsOn> runs = guildDao.getRunsOn(guildId);
        if (runs == null || runs.isEmpty()) {
            // fallback: if no schedule found
            Calendar begin = (Calendar) now.clone();
            begin.add(Calendar.HOUR_OF_DAY, -24);
            return begin;
        }
        int currentHour = endCopy.get(Calendar.HOUR_OF_DAY);
        RunsOn prev = null;
        for (RunsOn r : runs) {
            if (r.getValue() < currentHour) {
                prev = r;
            }
        }
        Calendar begin = (Calendar) now.clone();
        if (prev != null) {
            begin.set(Calendar.MINUTE, 0);
            begin.set(Calendar.SECOND, 0);
            begin.set(Calendar.MILLISECOND, 0);
            begin.set(Calendar.HOUR_OF_DAY, prev.getValue());
        } else {
            // previous is yesterday's last run hour
            RunsOn last = runs.get(runs.size() - 1);
            begin.add(Calendar.DAY_OF_MONTH, -1);
            begin.set(Calendar.MINUTE, 0);
            begin.set(Calendar.SECOND, 0);
            begin.set(Calendar.MILLISECOND, 0);
            begin.set(Calendar.HOUR_OF_DAY, last.getValue());
        }
        return begin;
    }

    @Override
    public String afterRunMessage() {
        if (config.getPushToGitHub()) {
            return "bot completed making archive and pushing all files to GitHub repository";
        } else {
            return "bot completed making archive";
        }
    }
}
