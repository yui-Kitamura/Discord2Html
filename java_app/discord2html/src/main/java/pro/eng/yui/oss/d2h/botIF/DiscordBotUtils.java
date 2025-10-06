package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class DiscordBotUtils {
    
    private final GuildsDAO guildsDao;
    private final ChannelsDAO channelsDao;
    private final UsersDAO usersDao;
    
    @Autowired
    public DiscordBotUtils(GuildsDAO g, ChannelsDAO c, UsersDAO u){
        this.guildsDao = g;
        this.channelsDao = c;
        this.usersDao = u;
    }

    public MessageEmbed buildStatusEmbed(Color frameColor, String message) {
        return new EmbedBuilder()
                .setDescription(message)
                .setColor(frameColor)
                .build();
    }

    /* pkg-prv */ void upsertGuildInfoToDB(Guild guild){
        Guilds newRecord = new Guilds();
        newRecord.setGuildId(new GuildId(guild));
        newRecord.setGuildName(new GuildName(guild));
        guildsDao.upsertGuildInfo(newRecord);
    }
    
    /* pkg-prv */ void upsertGuildChannelToDB(GuildMessageChannel ch){
        upsertGuildInfoToDB(ch.getGuild());
        Channels newRecord = new Channels(ch);
        channelsDao.upsertChannelInfo(newRecord);
    }

    /* pkg-prv */ void upsertGuildChannelToDB(Guild guild){
        for(GuildMessageChannel channel : guild.getTextChannels()) {
            upsertGuildChannelToDB(channel);
        }
        for(GuildMessageChannel channel : guild.getVoiceChannels()) {
            upsertGuildChannelToDB(channel);
        }
    }
    
    /* pkg-prv */ void registerUserInfoToDB(Member member){
        upsertGuildInfoToDB(member.getGuild());
        Users newRecord = new Users(member);
        usersDao.upsertUserInfo(newRecord);
    }

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
        
        List<Channels> activeDbCh = channelsDao.selectChannelArchiveDo(new GuildId(guild));
        for(Channels ch : activeDbCh) {
            result.add(guild.getGuildChannelById(ch.getChannelId().getValue()));
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
        if(member == null) { return false; }
        return member.getRoles().contains(getD2hAdminRole(member.getGuild()));
    }
}
