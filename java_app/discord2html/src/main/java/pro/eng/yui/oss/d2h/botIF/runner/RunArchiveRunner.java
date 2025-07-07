package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBot;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.Calendar;
import java.util.List;

@Component
public class RunArchiveRunner implements IRunner {
    
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    private final DiscordJdaProvider jda;
    
    @Autowired
    public RunArchiveRunner(GuildsDAO g, ChannelsDAO ch, DiscordJdaProvider j){
        this.guildDao = g;
        this.channelDao = ch;
        this.jda = j;
    }
    
    public void run(Member member, List<OptionMapping> options){
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);

        boolean isTargetChannelMarked = false;
        for(OptionMapping om : options) {
            if("target".equals(om.getName())) {
                String inputName = om.getAsString();
                List<TextChannel> channels =  member.getGuild().getTextChannelsByName(inputName, true);
                for(GuildMessageChannel channel : channels){
                    isTargetChannelMarked = true;
                    run(channel);
                }
                List<VoiceChannel> voiceChannels = member.getGuild().getVoiceChannelsByName(inputName, true);
                for(GuildMessageChannel v : voiceChannels) {
                    isTargetChannelMarked = true;
                    run(v);
                }
            }
        }
        if(isTargetChannelMarked == false) {
            for(GuildMessageChannel c : member.getGuild().getTextChannels()) {
                run(c);
            }
            for(GuildMessageChannel v : member.getGuild().getVoiceChannels()) {
                run(v);
            }
        }

        member.getJDA().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
    }
    
    public void run(){
        final int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        List<Guilds> allGuilds = guildDao.selectAll();
        for(Guilds guilds : allGuilds) {
            for(RunsOn on : guilds.getRunsOn()) {
                if(on.getValue() == now) {
                    List<Channels> chs = channelDao.selectAllInGuild(guilds.getGuildId());
                    for(Channels ch : chs) {
                        run(jda.getJda().getGuildById(ch.getGuidId().getValue()).getChannelById(GuildMessageChannel.class, ch.getChannelId().getValue()));
                    }
                }
            }
        }
    }

    private void run(GuildMessageChannel channel){
        
        //validate
        List<Channels> activate = channelDao.selectChannelArchiveDo(new GuildId(channel.getGuild()));
        boolean isActivatedChannel = false;
        ChannelId targetChannelId = new ChannelId(channel);
        for(Channels c : activate) {
            if(c.getChannelId().equals(targetChannelId)) {
                isActivatedChannel = true;
                break;
            }
        }
        if(isActivatedChannel == false) {
            return;
        }
        
        channel.sendMessage("This channel is archive target. Start >>>").queue();
        
        //TODO implement archive
        
        try {
            Thread.sleep(1234);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        channel.sendMessage("archive created. task end <<<").queue();
    }

    @Override
    public String afterRunMessage() {
        return "bot completed making archive";
    }
}
