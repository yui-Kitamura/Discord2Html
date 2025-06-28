package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;

import java.util.EnumSet;

@Component
public class DiscordBot  {

    private final Secrets secrets;
    private JDA jda;
    
    private final DiscordBotListener botEventListener;

    @Autowired
    public DiscordBot(Secrets secrets, DiscordBotListener eventListener) {
        this.secrets = secrets;
        this.botEventListener = eventListener;
    }

    @PostConstruct
    public void initialize() {
        shutdownJdaIfNeeded();
        try {
            jda = JDABuilder.create(
                        secrets.getDiscordBotToken(),
                        EnumSet.of(
                                GatewayIntent.GUILD_PRESENCES,
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MESSAGE_REACTIONS
                        )
                )
                .setStatus(OnlineStatus.IDLE)
                .addEventListeners(botEventListener)
                .build();
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void shutdownJdaIfNeeded(){
        if(jda != null) {
            jda.shutdownNow();
        }
    }

}
