package pro.eng.yui.oss.d2h.botIF.runner;

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
    private final DiscordBot bot;
    
    public RunArchiveRunner(GuildsDAO g, ChannelsDAO ch, DiscordBot bot){
        this.guildDao = g;
        this.channelDao = ch;
        this.bot = bot;
    }
    
    public void run(Member member, List<OptionMapping> options){
        //TODO run make archive file
        
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE, DiscordBot.working);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
                        run(ch);
                    }
                }
            }
        }
    }
    
    private void run(Channels target){
        Guild guildIn = bot.jda().getGuildById(target.getGuidId().getValue());
        if(guildIn == null){
            System.err.println("Guild not found: "+ target.getGuidId());    
            return;
        }
        GuildMessageChannel channel = guildIn.getChannelById(GuildMessageChannel.class, target.getChannelId().getValue());
        if(channel == null) {
            System.err.println("Channel not found: " + target.getChannelId());
            return;
        }
        
        channel.sendMessage("This channel is archive target. Start >>>").queue();
        
        //TODO implement archive
        
        channel.sendMessage("archive created. task end <<<").queue();
    }

    @Override
    public String afterRunMessage() {
        return "bot completed making archive";
    }
}
