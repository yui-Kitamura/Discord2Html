package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import org.springframework.stereotype.Component;

@Component
public class HelpRunner implements IRunner {
    
    public HelpRunner(){
        // nothing to do
    }
    
    public void run(Member member, boolean isAdmin){
        //TODO send private message for help
        
        
    }

    @Override
    public String afterRunMessage() {
        return "bot sent you help guid to DM";
    }
}
