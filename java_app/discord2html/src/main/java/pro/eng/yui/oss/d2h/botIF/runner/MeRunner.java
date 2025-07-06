package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.List;

@Component
public class MeRunner implements IRunner {

    private final UsersDAO usersDao;

    @Autowired
    public MeRunner(UsersDAO users){
        this.usersDao = users;
    }

    public void run(Member member, List<OptionMapping> command){
        boolean isConfigAnonymous = false;
        
        for(OptionMapping op : command) {
            if("anonymous".equals(op.getName())) {
                if(isConfigAnonymous){ continue; }
                isConfigAnonymous = true;
                runSetAnonymous(member, new IgnoreAnon(op.getAsBoolean()));
            }
        }
        
    }
    
    @Override
    public String afterRunMessage(){
        return "Your configuration has updated successfully";
    }
    
    private void runSetAnonymous(Member member, IgnoreAnon newValue){
        final UserId userId = new UserId(member.getUser());
        final GuildId guildId = new GuildId(member.getGuild());
        Users latestInfo = new Users();
        latestInfo.setUserId(userId);
        latestInfo.setGuildId(guildId);
        latestInfo.setUserName(new UserName(member.getUser()));
        latestInfo.setNickname(new Nickname(member));
        latestInfo.setAvatar(new Avatar(member.getUser()));
        usersDao.upsertUserInfo(latestInfo);
        usersDao.updateIgnoreAnon(guildId, userId, newValue);
    }
}
