package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.model.Guilds;

@Mapper
public interface GuildsMapper {

    void register(Guilds newRecord);

    void update(Guilds param);

    Guilds selectOne(Guilds key);
    
}
