package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.consts.StringConsts;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.Permission;

@Component
public class DiscordBot extends ListenerAdapter {

    private final Secrets secrets;

    @Autowired
    public DiscordBot(Secrets secrets) {
        this.secrets = secrets;
    }

    @PostConstruct
    public void initialize() {
        JDA jda = JDABuilder.create(
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
        
        joinEvent.getGuild().createRole()
                .setName(StringConsts.ADMIN_ROLE).setColor(Color.GRAY)
                .queue();
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
            event.getChannel().sendMessage("yes, I'm here.")
                .setMessageReference(msg).mentionRepliedUser(false)
                .queue();
        }

        Member author = event.getMember();
        if(author != null) {
            for (Role r : author.getRoles()) {
                if(r.getName().equals(StringConsts.ADMIN_ROLE)) {
                    sendMessagePrivate(event);
                    break;
                }
            }
        }
        
        event.getChannel().sendMessage("Call D2H with slash commands if needed!")
                .queue();
    }
        
    private void sendMessagePrivate(MessageReceivedEvent messageEvent){
        StringBuilder chListWithBr = new StringBuilder();
        for (Channel ch : getArchivableChannelList(messageEvent.getGuild())) {
            chListWithBr.append(ch.getName()).append("\n");
        }
        final String guildName = messageEvent.getGuild().getName();

        messageEvent.getAuthor().openPrivateChannel()
                .flatMap(channel -> channel.sendMessage("Hello. You have D2H bot admin role." +
                                "So you can use this bots commands to make archive.")
                        .flatMap(message -> 
                                channel.sendMessage("These channels in "+guildName+" are enabled to use admin commands." +
                                        "```\n" + chListWithBr + "```"
                                )
                        )
                )
                .queue(null, error ->
                        messageEvent.getChannel()
                                .sendMessage("Failed to send private message")
                                .queue()
                );
    }
    
    private List<Channel> getArchivableChannelList(Guild guild){
        List<Channel> result = new ArrayList<>();
        
        List<Role> adminRole = guild.getRolesByName(StringConsts.ADMIN_ROLE, false);

        if (!adminRole.isEmpty()) {
            IPermissionHolder holder = adminRole.get(0);
            for (GuildChannel channel : guild.getChannels()) {
                if (holder.hasPermission(channel, Permission.VIEW_CHANNEL)) {
                    result.add(channel);
                }
            }
        }
        return result;
    }
}
