package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArchiveConfigRunner implements IRunner {
    
    public ArchiveConfigRunner(){
        // nothing to do
    }
    
    public void run(Member member, List<OptionMapping> options){
        //TODO set archive settings with new value
        
    }

    @Override
    public String afterRunMessage() {
        return "archive setting has changed";
    }
}
