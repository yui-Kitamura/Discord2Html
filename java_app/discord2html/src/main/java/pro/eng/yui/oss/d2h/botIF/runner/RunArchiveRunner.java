package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBot;

import java.util.List;

@Component
public class RunArchiveRunner implements IRunner {
    
    public RunArchiveRunner(){
        // nothing to do
    }
    
    public void run(Member member, List<OptionMapping> options){
        //TODO run make archive file
        
        member.getJDA().getPresence().setPresence(OnlineStatus.ONLINE,  Activity.playing("working"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        member.getJDA().getPresence().setPresence(OnlineStatus.IDLE, Activity.playing(DiscordBot.idleMessage));
    }

    @Override
    public String afterRunMessage() {
        return "";
    }
}
