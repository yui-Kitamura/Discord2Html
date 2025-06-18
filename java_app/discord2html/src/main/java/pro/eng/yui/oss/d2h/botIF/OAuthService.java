package pro.eng.yui.oss.d2h.botIF;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.DiscordOauthTokenDAO;
import pro.eng.yui.oss.d2h.db.field.AccessToken;
import pro.eng.yui.oss.d2h.db.field.ExpireAt;
import pro.eng.yui.oss.d2h.db.field.RefreshToken;
import pro.eng.yui.oss.d2h.db.field.Scope;

import java.util.Date;

@Service
public class OAuthService {
    
    private final DiscordOauthTokenDAO discordDao;
    
    @Autowired
    public OAuthService(DiscordOauthTokenDAO dao){
        this.discordDao = dao;
    }
    
    public void registerOrUpdateNewToken(@NotNull OAuth2AuthorizedClient client){
        AccessToken tokenValue = null;
        RefreshToken refreshToken = null;
        Scope scope = null;
        ExpireAt expire = null;

        if (client.getAccessToken() != null) {
            tokenValue = new AccessToken(client.getAccessToken().getTokenValue());
        }
        if (client.getRefreshToken() != null) {
            refreshToken = new RefreshToken(client.getRefreshToken().getTokenValue());
        }
        scope = new Scope(client.getAccessToken().getScopes());
        if(client.getAccessToken().getExpiresAt() != null) {
            expire = new ExpireAt(Date.from(client.getAccessToken().getExpiresAt()));
        }

        try {
            discordDao.selectOne(null); //FIXME use uid
        }catch(DbRecordNotFoundException nfe) {

        }
        //TODO DB-insert or update
    }
}
