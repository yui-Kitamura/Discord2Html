package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.model.Optout;

import java.util.List;

@Mapper
public interface OptoutMapper {

    void insert(Optout record);

    void update(Optout record);

    void optin(Optout key);

    Optout selectOne(Optout key);

    List<Optout> selectAllByUserGuild(Optout key);

    Integer countEffectiveOptout(Optout key);
}
