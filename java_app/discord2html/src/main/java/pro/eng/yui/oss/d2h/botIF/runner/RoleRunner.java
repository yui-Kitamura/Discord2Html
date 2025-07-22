package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    
    @Autowired
    public RoleRunner(RolesDAO rolesDao){
        this.roleDao = rolesDao;
    }
    
    public void run(Member member, List<OptionMapping> options){
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
    public String afterRunMessage() {
        return "role setting has changed";
    }
}
