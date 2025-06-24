package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Users;

@Mapper
public interface UsersMapper {
    
    Users findById(@Param("user_id")UserId userId);

    void insert(Users insertParam);
}
