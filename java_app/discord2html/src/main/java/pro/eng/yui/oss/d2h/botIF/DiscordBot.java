package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.consts.StringConsts;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.Permission;

@Component
public class DiscordBot  {

    private final Secrets secrets;
    private JDA jda;

    @Autowired
    public DiscordBot(Secrets secrets) {
        this.secrets = secrets;
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
                .addEventListeners(this)
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

    /* pkg-prv */ void sendMessagePrivate(MessageReceivedEvent messageEvent){
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

    /* pkg-prv */ List<Channel> getArchivableChannelList(Guild guild){
        List<Channel> result = new ArrayList<>();
        
        List<Role> adminRole = guild.getRolesByName(StringConsts.ADMIN_ROLE, false);

        if (!adminRole.isEmpty()) {
            IPermissionHolder holder = adminRole.get(0);
            for (GuildChannel channel : guild.getChannels()) {
                if (channel instanceof Category) { continue; }
                if (holder.hasPermission(channel, Permission.MESSAGE_HISTORY)) {
                    result.add(channel);
                }
            }
        }
        return result;
    }

    /* pkg-prv */ boolean isArchivableChannel(GuildChannel channel){
        Role adminRole = getH2dAdminRole(channel.getGuild());
        if(adminRole == null){ return false; }
        return getArchivableChannelList(channel.getGuild()).contains(channel);
    }

    /* pkg-prv */ Role getH2dAdminRole(Guild guild){
        List<Role> adminRole = guild.getRolesByName(StringConsts.ADMIN_ROLE, false);
        if(adminRole.isEmpty()){ return null; }
        return adminRole.get(0);
    }
}
