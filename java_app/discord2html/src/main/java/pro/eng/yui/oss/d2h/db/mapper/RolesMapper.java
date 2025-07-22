package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.model.Roles;

import java.util.List;

@Mapper
public interface RolesMapper {

    Roles findById(Roles key);
    
    List<Roles> findWithGuild(Roles key);

    void register(Roles insertParam);
    
    // not required //void update(Roles keyValue);
    
    void setAsOpen(Roles key);
    void setAsAnon(Roles key);
}
