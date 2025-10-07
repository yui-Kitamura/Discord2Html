package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBotUtils;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.RolesDAO;
import pro.eng.yui.oss.d2h.db.field.AnonStats;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RoleId;
import pro.eng.yui.oss.d2h.db.model.Roles;

import java.util.List;

@Component
public class RoleRunner implements IRunner {
    
    private final RolesDAO roleDao;
    private final DiscordBotUtils discordBotUtils;

    @Autowired
    public RoleRunner(RolesDAO rolesDao, DiscordBotUtils discordBotUtils){
        this.roleDao = rolesDao;
        this.discordBotUtils = discordBotUtils;
    }
    
    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.D2H_ADMIN;
    }
    
    public void run(List<OptionMapping> options){
        Role targetRole = get(options, "role").getAsRole();
        UserAnon newValue = UserAnon.get(get(options, "anonymous").getAsString());
        
        RoleId roleId = new RoleId(targetRole.getIdLong());
        try {
            roleDao.selectRole(roleId);
        }catch(DbRecordNotFoundException notFound) {
            // 未登録のroleについてはregister
            Roles param = new Roles();
            param.setRoleId(roleId);
            param.setGuildId(new GuildId(targetRole.getGuild().getIdLong()));
            roleDao.upsertRoleInfo(param);
        }
        
        roleDao.setRoleAnonStats(roleId, new AnonStats(newValue));
    }

    @Override
    public MessageEmbed afterRunMessage() {
        return discordBotUtils.buildStatusEmbed(SUCCESS, "role setting has changed");
    }
}
