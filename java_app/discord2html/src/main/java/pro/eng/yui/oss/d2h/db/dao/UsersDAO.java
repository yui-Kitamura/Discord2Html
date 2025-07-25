package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.mapper.UsersMapper;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class UsersDAO {
    
    private final UsersMapper mapper;
    
    @Autowired
    public UsersDAO(UsersMapper usersMapper){
        this.mapper = usersMapper;
    }
    
    public Users select(GuildId joinedTo, UserId keyId){
        Users param = new Users();
        param.setGuildId(joinedTo);
        param.setUserId(keyId);
        Users res = mapper.findById(param);
        if(res == null) {
            throw new DbRecordNotFoundException(keyId.toString());
        }
        return res;
    }
    
    public boolean exists(GuildId joinedTo, UserId keyId){
        try {
            select(joinedTo, keyId);
            return true;
        }catch (DbRecordNotFoundException nfe) {
            return false;
        }
    }
    
    public Users upsertUserInfo(Users newInfo){
        if(exists(newInfo.getGuildId(), newInfo.getUserId())) {
            update(newInfo.getGuildId(), newInfo.getUserId(), newInfo);
        }else {
            insert(newInfo);
        }
        return select(newInfo.getGuildId(), newInfo.getUserId());
    }
    
    public Users insert(Users newRecord){
        Users insertParam = new Users();
        try {
            insertParam.setUserId(Objects.requireNonNull(newRecord.getUserId()));
            insertParam.setGuildId(Objects.requireNonNull(newRecord.getGuildId()));
            insertParam.setUserName(Objects.requireNonNull(newRecord.getUserName()));
            insertParam.setNickname(newRecord.getNickname());
            insertParam.setAvatar(newRecord.getAvatar());
            if(newRecord.getAnonStats() != null) {
                insertParam.setAnonStats(newRecord.getAnonStats());
            }
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        
        mapper.insert(insertParam);
        return select(insertParam.getGuildId(), insertParam.getUserId());
    }
    
    /** ignore_anon 以外の更新 */
    public Users update(GuildId joinedTo, UserId key, Users newData){
        Users usersKey = new Users();
        Users updateParam = new Users();
        try {
            usersKey.setUserId(Objects.requireNonNull(key));
            usersKey.setGuildId(Objects.requireNonNull(joinedTo));
            
            if(newData.getUserName() != null) {
                updateParam.setUserName(newData.getUserName());
            }
            if(newData.getAvatar() != null) {
                updateParam.setAvatar(newData.getAvatar());
            }
            if(newData.getNickname() != null) {
                updateParam.setNickname(newData.getNickname());
            }
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }

        Map<String, Users> keyVal = new HashMap<>();
        keyVal.put("key", usersKey);
        keyVal.put("param", updateParam);
        mapper.update(keyVal);
        return select(usersKey.getGuildId(), usersKey.getUserId());
    }
    
    public UserAnon updateAnonStats(GuildId joinedTo, UserId userId, UserAnon newValue){
        Users current = select(joinedTo, userId);
        if(newValue.equals(current.getAnonStats().get())) {
            return current.getAnonStats().get();
        }
        
        if(newValue.isAnon()) {
            mapper.setAsAnon(current);
        }else {
            mapper.setAsOpen(current);
        }
        return newValue;
    }
    
}
