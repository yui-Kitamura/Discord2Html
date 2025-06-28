package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
    private final DiscordBotCommandListener botCommandListener;

    @Autowired
    public DiscordBot(Secrets secrets,
                      DiscordBotListener eventListener,
                      DiscordBotCommandListener commandListener) {
        this.secrets = secrets;
        this.botEventListener = eventListener;
        this.botCommandListener = commandListener;
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
                .addEventListeners(botEventListener, botCommandListener)
                .build();
            jda.awaitReady();
            
            updateCommands();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void shutdownJdaIfNeeded(){
        if(jda != null) {
            jda.shutdownNow();
        }
    }

    private void updateCommands(){
        SlashCommandData d2hCommand = Commands.slash(
                DiscordBotCommandListener.commands.get(0),
                "operation D2H bot"      
        );
        d2hCommand.addSubcommands(DiscordBotCommandListener.D2H_SUB_COMMANDS);
        
        jda.updateCommands().addCommands(d2hCommand).queue();
    }

}
