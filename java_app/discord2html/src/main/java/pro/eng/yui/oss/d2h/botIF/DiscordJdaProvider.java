package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

@Component
public class DiscordJdaProvider {
    private JDA jda;
    public JDA getJda() {
        return jda;
    }
    /* pkg-prv */ void setJda(JDA newValue){
        this.jda = newValue;
    }
}
