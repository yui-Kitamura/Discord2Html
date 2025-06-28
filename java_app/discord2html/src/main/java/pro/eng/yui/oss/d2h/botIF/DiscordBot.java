package pro.eng.yui.oss.d2h.botIF;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
    
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent joinEvent){
        joinEvent.getJDA().getPresence().setStatus(OnlineStatus.IDLE);
        joinEvent.getJDA().getPresence().setActivity(Activity.playing("Standby for log"));
        
        if(getH2dAdminRole(joinEvent.getGuild()) == null) {
            joinEvent.getGuild().createRole()
                    .setName(StringConsts.ADMIN_ROLE).setColor(Color.GRAY)
                    .setMentionable(false)
                    .queue();
        }
        Role role = getH2dAdminRole(joinEvent.getGuild());
        if(role != null) {
            List<GuildChannel> ch = joinEvent.getGuild().getChannels();
            for (GuildChannel gc : ch) {
                gc.getPermissionContainer()
                        .upsertPermissionOverride(role)
                        .deny(Permission.VIEW_CHANNEL)
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

            Member author = event.getMember();
            if (author != null) {
                for (Role r : author.getRoles()) {
                    if (r.getName().equals(StringConsts.ADMIN_ROLE)) {
                        sendMessagePrivate(event);
                        break;
                    }
                }
            }

            event.getChannel().sendMessage("Call D2H with slash commands if needed!")
                    .queue();
        }
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
                if (channel instanceof Category) { continue; }
                if (holder.hasPermission(channel, Permission.VIEW_CHANNEL)) {
                    result.add(channel);
                }
            }
        }
        return result;
    }
    
    private Role getH2dAdminRole(Guild guild){
        List<Role> adminRole = guild.getRolesByName(StringConsts.ADMIN_ROLE, false);
        if(adminRole.isEmpty()){ return null; }
        return adminRole.get(0);
    }
}
