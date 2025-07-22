package pro.eng.yui.oss.d2h.db.dao;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.mapper.RolesMapper;
import pro.eng.yui.oss.d2h.db.model.Roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class RolesDAO {
    
    private final RolesMapper mapper;
    
    @Autowired
    public RolesDAO(RolesMapper rolesMapper){
        this.mapper = rolesMapper;
    }
    
    public Roles selectRole(RoleId id){
        Roles param = new Roles();
        try {
            param.setRoleId(Objects.requireNonNull(id));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        Roles res = mapper.findById(param);
        if(res == null) {
            throw new DbRecordNotFoundException("not found with role_id " + id);
        }
        return res;
    }
    
    public List<Roles> selectRoleList(GuildId guildId){
        Roles param = new Roles();
        try {
            param.setGuildId(Objects.requireNonNull(guildId));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        List<Roles> resList = mapper.findWithGuild(param);
        if(resList == null) {
            return Collections.emptyList();
        }
        return resList;
    }
    
    public List<Roles> selectUserRole(Member member){
        List<Roles> userRoleList = new ArrayList<>();
        List<Role> being = member.getRoles();
        for(Role r : being) {
            userRoleList.add(selectRole(new RoleId(r.getIdLong())));
        }
        return userRoleList;
    }
    
    public UserAnon checkUserAnonStats(Member member){
        List<Roles> being = selectUserRole(member);
        boolean isAnon = true;
        for(Roles r : being) {
            if (r.getAnonStats().get().isOpen()) {
                isAnon = false;
                break;
            }
        }
        return UserAnon.get(isAnon);
    }
    
    public boolean exists(RoleId id){
        try {
            Roles info = selectRole(id);
            return id.equals(info.getRoleId()); //念のため
        }catch(IllegalArgumentException | DbRecordNotFoundException err) {
            return false;
        }
    }
    
    public void upsertRoleInfo(Roles newRecord){
        try {
            Objects.requireNonNull(newRecord.getRoleId());
            Objects.requireNonNull(newRecord.getGuildId());
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        if(exists(newRecord.getRoleId())) {
            //mapper.update(newRecord);
        }else{
            if(newRecord.getAnonStats() == null) {
                newRecord.setAnonStats(new AnonStats(UserAnon.ANONYMOUS));
            }
            mapper.register(newRecord);
        }
    }
    
    public void setRoleAnonStats(RoleId role, AnonStats newValue){
        Roles param = new Roles();
        try {
            param.setRoleId(Objects.requireNonNull(role));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        if(newValue.get().isAnon()) {
            mapper.setAsAnon(param);
        }else {
            mapper.setAsOpen(param);
        }
    }
    
}
