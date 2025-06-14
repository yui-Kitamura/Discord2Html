package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;

import java.util.EnumSet;

@Component
public class DiscordBot extends ListenerAdapter implements ApplicationRunner {

    private final Secrets secrets;

    @Autowired
    public DiscordBot(Secrets secrets) {
        this.secrets = secrets;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize(args.getSourceArgs());
    }

    public void initialize(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DiscordBot.class, args);
        DiscordBot bot = context.getBean(DiscordBot.class);
        JDA jda = JDABuilder.create(
                    bot.secrets.getDiscordAuth(),
                        EnumSet.of(
                                GatewayIntent.GUILD_PRESENCES,
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT
                        )
                )
                .setStatus(OnlineStatus.IDLE)
                .addEventListeners(bot)
                .build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        final String msgBody = msg.getContentRaw();
        
        if(msgBody.contains("D2H")) {
            event.getChannel().sendMessage("yes, I`m here.");
        }
    }
}
