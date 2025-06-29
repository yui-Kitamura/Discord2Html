package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.IgnoreAnon;
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
    
    public Users select(UserId keyId){
        Users res = mapper.findById(keyId);
        if(res == null) {
            throw new DbRecordNotFoundException(keyId.toString());
        }
        return res;
    }
    
    public Users insert(Users newRecord){
        Users insertParam = new Users();
        try {
            insertParam.setUserId(Objects.requireNonNull(newRecord.getUserId()));
            insertParam.setUserName(Objects.requireNonNull(newRecord.getUserName()));
            insertParam.setNickname(newRecord.getNickname());
            insertParam.setAvatar(newRecord.getAvatar());
            insertParam.setIgnoreAnon(newRecord.getIgnoreAnon());
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        
        mapper.insert(insertParam);
        return select(insertParam.getUserId());
    }
    
    /** ignore_anon 以外の更新 */
    public Users update(UserId key, Users newData){
        Users usersKey = new Users();
        Users updateParam = new Users();
        try {
            usersKey.setUserId(Objects.requireNonNull(key));
            
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
        return select(key);
    }
    
    public IgnoreAnon updateIgnoreAnon(UserId userId, IgnoreAnon newValue){
        Users current = select(userId);
        if(current.getIgnoreAnon().equals(newValue)) {
            return current.getIgnoreAnon();
        }
        
        if(newValue.isTrue()) {
            mapper.setIgnoreAnon(userId);
        }else {
            mapper.setAsAnon(userId);
        }
        return newValue;
    }
    
}
