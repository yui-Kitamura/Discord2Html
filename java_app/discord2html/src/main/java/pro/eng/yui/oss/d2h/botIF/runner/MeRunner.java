package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBotUtils;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.List;

@Component
public class MeRunner implements IRunner {

    private final UsersDAO usersDao;
    private final DiscordBotUtils discordBotUtils;

    @Autowired
    public MeRunner(UsersDAO users, DiscordBotUtils discordBotUtils){
        this.usersDao = users;
        this.discordBotUtils = discordBotUtils;
    }

    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.ANY;
    }
    
    public void run(Member member, List<OptionMapping> options){
        UserAnon newValue = UserAnon.get(get(options, "anonymous").getAsString());
        runSetAnonymous(member, newValue);
    }
    
    @Override
    public MessageEmbed afterRunMessage(){
        return discordBotUtils.buildStatusEmbed(SUCCESS, "Your configuration has updated successfully");
    }
    
    @Override
    public boolean shouldDeferEphemeral(){
        return true;
    }
    
    private void runSetAnonymous(Member member, UserAnon newValue){
        final UserId userId = new UserId(member.getUser());
        final GuildId guildId = new GuildId(member.getGuild());
        Users latestInfo = new Users(member);
        usersDao.upsertUserInfo(latestInfo);
        usersDao.updateAnonStats(guildId, userId, newValue);
    }
}
