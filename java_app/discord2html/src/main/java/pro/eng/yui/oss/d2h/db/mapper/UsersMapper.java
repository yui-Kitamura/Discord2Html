package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.Map;

@Mapper
public interface UsersMapper {
    
    Users findById(Users key);

    void insert(Users insertParam);
    
    void update(Map<String, Users> keyValue);
    
    void setIgnoreAnon(Users key);
    void setAsAnon(Users key);
}
