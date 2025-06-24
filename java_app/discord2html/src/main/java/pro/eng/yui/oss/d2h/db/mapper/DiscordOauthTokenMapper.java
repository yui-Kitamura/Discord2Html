package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

import java.util.List;

@Mapper
public interface DiscordOauthTokenMapper {

    void insertToken(@Param("token") DiscordOauthToken token);

    void updateToken(@Param("key") DiscordOauthToken key, @Param("token") DiscordOauthToken token);

    DiscordOauthToken findByUserId(@Param("userId") UserId userId);

    void deleteToken(@Param("userId") UserId userId);
    
    List<DiscordOauthToken> select();

}
