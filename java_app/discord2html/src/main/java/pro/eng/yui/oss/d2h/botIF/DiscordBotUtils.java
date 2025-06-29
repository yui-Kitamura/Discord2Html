package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.consts.StringConsts;

import java.util.ArrayList;
import java.util.List;

@Component
public class DiscordBotUtils {

    /* pkg-prv */ void sendMessagePrivate(MessageReceivedEvent messageEvent){
        StringBuilder chListWithBr = new StringBuilder();
        for (Channel ch : getArchivableChannelList(messageEvent.getGuild())) {
            chListWithBr.append(ch.getName()).append("\n");
        }
        final String guildName = messageEvent.getGuild().getName();

        messageEvent.getAuthor().openPrivateChannel()
                .flatMap(channel -> 
                        channel.sendMessage("Hello. You have D2H bot admin role." +
                                "So you can use this bots commands to make archive.")
                        .flatMap(message ->
                                channel.sendMessage("These channels in "+guildName+
                                        " are enabled to use admin commands." +
                                        "```\n" + chListWithBr + "```"
                                )
                        )
                )
                .queue();
    }

    /* pkg-prv */ List<Channel> getAdminTaggedChannelList(Guild guild){
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
        Role adminRole = getD2hAdminRole(channel.getGuild());
        if(adminRole == null){ return false; }
        return getArchivableChannelList(channel.getGuild()).contains(channel);
    }

    /* pkg-prv */ Role getD2hAdminRole(Guild guild){
        List<Role> adminRole = guild.getRolesByName(StringConsts.ADMIN_ROLE, false);
        if(adminRole.isEmpty()){ return null; }
        return adminRole.get(0);
    }
    
    /* pkg-prv */ boolean isD2hAdmin(Member member){
        return member.getRoles().contains(getD2hAdminRole(member.getGuild()));
    }
}
