package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

import java.util.List;
import java.util.Map;

@Mapper
public interface DiscordOauthTokenMapper {

    void insertToken(DiscordOauthToken token);

    void updateToken(Map<String, Object> params);

    DiscordOauthToken findByUserId(UserId userId);

    void deleteToken(UserId userId);
    
    List<DiscordOauthToken> select();

}
