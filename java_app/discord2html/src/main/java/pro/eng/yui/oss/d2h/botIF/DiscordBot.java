package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;

import java.util.EnumSet;

@Component
public class DiscordBot  {

    private final Secrets secrets;
    private final DiscordJdaProvider jda;
    
    private final DiscordBotListener botEventListener;
    private final DiscordBotCommandListener botCommandListener;

    public static final Activity idle = Activity.customStatus("stand by for log");
    public static final Activity working = Activity.customStatus("working");
    
    @Autowired
    public DiscordBot(Secrets secrets, DiscordJdaProvider provider, 
                      DiscordBotListener eventListener,
                      DiscordBotCommandListener commandListener) {
        this.secrets = secrets;
        this.jda = provider;
        this.botEventListener = eventListener;
        this.botCommandListener = commandListener;
    }

    @PostConstruct
    public void initialize() {
        shutdownJdaIfNeeded();
        try {
            jda.setJda(JDABuilder.create(
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
                .setActivity(idle)
                .addEventListeners(botEventListener, botCommandListener)
                .build()
            );
            jda.getJda().awaitReady();
            
            updateCommands();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void shutdownJdaIfNeeded(){
        if(jda.getJda() != null) {
            jda.getJda().shutdownNow();
        }
    }

    private void updateCommands(){
        SlashCommandData d2hCommand = Commands.slash(
                DiscordBotCommandListener.commands.get(0),
                "operation D2H bot"      
        );
        d2hCommand.addSubcommands(DiscordBotCommandListener.D2H_SUB_COMMANDS);
        
        jda.getJda().updateCommands().addCommands(d2hCommand).queue();
    }

}
