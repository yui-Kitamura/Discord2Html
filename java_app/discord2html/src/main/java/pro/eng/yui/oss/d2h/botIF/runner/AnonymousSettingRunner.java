package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnonymousSettingRunner implements IRunner {
    
    public AnonymousSettingRunner(){
        //nothing to do
    }
    
    public void run(Guild guild, List<OptionMapping> command){
        //TODO implement
    }

    @Override
    public String afterRunMessage() {
        return "this guild has new anonymous user setting";
    }
}
