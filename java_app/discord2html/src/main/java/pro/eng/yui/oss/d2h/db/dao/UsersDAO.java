package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.mapper.UsersMapper;
import pro.eng.yui.oss.d2h.db.model.Users;

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
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        
        mapper.insert(insertParam);
        return select(insertParam.getUserId());
    }
}
