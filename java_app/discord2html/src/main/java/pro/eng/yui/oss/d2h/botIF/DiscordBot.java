package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.DiscordOauthTokenDAO;
import pro.eng.yui.oss.d2h.db.field.AccessToken;

import java.util.EnumSet;

@Component
public class DiscordBot extends ListenerAdapter {

    private final DiscordOauthTokenDAO discordDao;

    @Autowired
    public DiscordBot(DiscordOauthTokenDAO discordDao) {
        this.discordDao = discordDao;
    }

    @PostConstruct
    public void initialize() {
        AccessToken token;
        try {
            token = discordDao.select().getAccessToken();
        }catch(DbRecordNotFoundException e) {
            token = new AccessToken("");
        }
        JDA jda = JDABuilder.create(
                        token.getValue(),
                        EnumSet.of(
                                GatewayIntent.GUILD_PRESENCES,
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MESSAGE_REACTIONS
                        )
                )
                .setStatus(OnlineStatus.IDLE)
                .addEventListeners(this)
                .build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void refreshToken(){
        initialize();
    }
    
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent joinEvent){
        joinEvent.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
        joinEvent.getJDA().getPresence().setActivity(Activity.playing("Standby for log"));
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
