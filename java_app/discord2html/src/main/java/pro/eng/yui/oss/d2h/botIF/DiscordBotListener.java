package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.github.GitUtil;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscordBotListener extends ListenerAdapter {
    
    private final DiscordBotUtils bot;
    private final Set<String> notifiedOnce; // key: guildId:userId
    private final GitUtil gitUtil;

    @Autowired
    public DiscordBotListener(DiscordBotUtils bot, GitUtil gitUtil){
        this.bot = bot;
        this.notifiedOnce = ConcurrentHashMap.newKeySet();
        this.gitUtil = gitUtil;
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
    }

    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event){
        List<Role> adminRole = event.getGuild().getRolesByName(StringConsts.ADMIN_ROLE, false);
        if (adminRole.size() > 2 && event.getRole().getName().equalsIgnoreCase(StringConsts.ADMIN_ROLE)) {
            event.getRole().delete().reason("duplicate").queue();
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        bot.registerUserInfoToDB(event.getMember());
        try {
            final String msg = event.getMember().getAsMention() + " " +
                    "【お知らせ】このサーバーでは、投稿内容やリアクションがアーカイブとして記録・公開される場合があります。" +
                    "詳しくは、[アーカイブ運用ポリシー]("+ gitUtil.getPagesUrlSafe() +"/tos.html)をご確認ください。\n" +
                    "発言内容のアーカイブ記録に同意しない場合はコマンド `/d2h optout`をご利用ください";
            if (event.getMember().getGuild().getSystemChannel() != null) {
                event.getMember().getGuild().getSystemChannel()
                        .sendMessage(msg)
                        .queue();
            } else if (event.getMember().getDefaultChannel() != null) {
                event.getMember().getDefaultChannel().asTextChannel()
                        .sendMessage(msg)
                        .queue();
            } else if (event.getMember().getGuild().getDefaultChannel() != null) {
                event.getMember().getGuild().getDefaultChannel().asTextChannel()
                        .sendMessage(msg)
                        .queue();
            }
        }catch(Throwable e){ e.printStackTrace(); }
    }

}
