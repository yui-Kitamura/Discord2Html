package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        return mapper.findById(keyId);
    }
    
    public Users insert(Users newRecord){
        Users insertParam = new Users();
        try {
            insertParam.setUserId(Objects.requireNonNull(newRecord.getUserId()));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        
        mapper.insert(insertParam);
        return select(insertParam.getUserId());
    }
}
