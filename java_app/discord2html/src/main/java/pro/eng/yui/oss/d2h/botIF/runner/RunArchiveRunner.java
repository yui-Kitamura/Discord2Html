package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBot;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.Calendar;
import java.util.List;

@Component
public class RunArchiveRunner implements IRunner {
    
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    
    public RunArchiveRunner(GuildsDAO g, ChannelsDAO ch){
        this.guildDao = g;
        this.channelDao = ch;
    }
    
    public void run(Member member, List<OptionMapping> options){
        //TODO run make archive file
        
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);

        loop: for(OptionMapping om : options) {
            if("target".equals(om.getName())) {
                String channelName = om.getAsString();
                for(GuildMessageChannel channel : member.getGuild().getTextChannels()) {
                    if (channelName.equals(channel.getName())) {
                        run(channel);
                        break loop;
                    }
                }
            }
        }

        member.getJDA().getPresence().setPresence(OnlineStatus.IDLE, DiscordBot.idle);
    }
    
    public void run(){
        final int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        List<Guilds> allGuilds = guildDao.selectAll();
        loop: for(Guilds guilds : allGuilds) {
            for(RunsOn on : guilds.getRunsOn()) {
                if(on.getValue() == now) {
                    List<Channels> chs = channelDao.selectAllInGuild(guilds.getGuildId());
                    for(Channels ch : chs) {
                        run((GuildMessageChannel) ch);
                        break loop;
                    }
                }
            }
        }
    }
    
    private void run(JDA jda, Channels target) {
        Guild guildIn = jda.getGuildById(target.getGuidId().getValue());
        if (guildIn == null) {
            System.err.println("Guild not found: " + target.getGuidId());
            return;
        }
        GuildMessageChannel channel = guildIn.getChannelById(GuildMessageChannel.class, target.getChannelId().getValue());
        if (channel == null) {
            System.err.println("Channel not found: " + target.getChannelId());
            return;
        }
        
        run(channel);
    }
    
    private void run(GuildMessageChannel channel){
        
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
