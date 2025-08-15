package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
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
                    String inputName = om.getAsString();
                    List<TextChannel> channels = member.getGuild().getTextChannelsByName(inputName, true);
                    for (GuildMessageChannel channel : channels) {
                        isTargetChannelMarked = true;
                        run(channel);
                    }
                    List<VoiceChannel> voiceChannels = member.getGuild().getVoiceChannelsByName(inputName, true);
                    for (GuildMessageChannel v : voiceChannels) {
                        isTargetChannelMarked = true;
                        run(v);
                    }
                }
            }
            if (isTargetChannelMarked == false) {
                for (GuildMessageChannel c : member.getGuild().getTextChannels()) {
                    run(c);
                }
                for (GuildMessageChannel v : member.getGuild().getVoiceChannels()) {
                    run(v);
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
                        run(jda.getJda().getGuildById(ch.getGuidId().getValue()).getChannelById(GuildMessageChannel.class, ch.getChannelId().getValue()));
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
     */
    private void run(GuildMessageChannel channel){
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
            return;
        }

        channel.sendMessage("This channel is archive target. Start >>>").queue();

        Calendar beginDate = Calendar.getInstance();
        beginDate.add(Calendar.HOUR_OF_DAY, -3); //FIXME 3h固定で仮生成
        Calendar endDate = Calendar.getInstance();

        List<MessageInfo> messages = new ArrayList<>();
        List<Users> marked = new ArrayList<>();
        channel.getHistory().retrievePast(100)
                .complete()
                .stream()
                .filter(msg -> {
                    return msg.getTimeCreated().toInstant().isAfter(beginDate.toInstant())
                            && msg.getTimeCreated().toInstant().isBefore(endDate.toInstant());
                })
                .sorted(Comparator.comparing(msg -> { return msg.getTimeCreated(); }))
                .forEach(msg -> {
                    if(msg.getMember() != null) {
                        Users author = new Users(msg.getMember());
                        if (marked.contains(author) == false) {
                            usersDao.upsertUserInfo(author);
                            marked.add(author);
                        }

                        UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
                        if (author.getAnonStats() == null) {
                            author.setAnonStats(new AnonStats(anonStatus));
                        }
                        
                        messages.add(new MessageInfo(msg, author));
                    }
                });

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

        channel.sendMessage("archive created. task end <<<").queue();
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
