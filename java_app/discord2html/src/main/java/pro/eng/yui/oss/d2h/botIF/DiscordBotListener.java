package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.channel.update.GenericChannelUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.consts.StringConsts;

import java.awt.*;
import java.util.List;

@Component
public class DiscordBotListener extends ListenerAdapter {
    
    private final DiscordBotUtils bot;
    
    @Autowired
    public DiscordBotListener(DiscordBotUtils bot){
        this.bot = bot;
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent joinEvent){
        joinEvent.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
        joinEvent.getJDA().getPresence().setActivity(Activity.playing("Standby for log"));

        if(bot.getD2hAdminRole(joinEvent.getGuild()) == null) {
            joinEvent.getGuild().createRole()
                    .setName(StringConsts.ADMIN_ROLE).setColor(Color.GRAY)
                    .setMentionable(false)
                    .queue();
        }
        Role role = bot.getD2hAdminRole(joinEvent.getGuild());
        if(role != null) {
            List<GuildChannel> ch = joinEvent.getGuild().getChannels();
            for (GuildChannel gc : ch) {
                gc.getPermissionContainer()
                        .upsertPermissionOverride(role)
                        .deny(Permission.MESSAGE_HISTORY)
                        .queue();
            }
        }
    }

    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event){
        List<Role> adminRole = event.getGuild().getRolesByName(StringConsts.ADMIN_ROLE, false);
        if (adminRole.size() > 2 && event.getRole().getName().equalsIgnoreCase(StringConsts.ADMIN_ROLE)) {
            event.getRole().delete().reason("duplicate").queue();
        }
    }

}
