package pro.eng.yui.oss.d2h.botIF;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.DiscordOauthTokenDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

import java.util.Date;

@Service
public class OAuthService {
    
    private final DiscordOauthTokenDAO discordDao;
    private final DiscordApiClient discordApi;
    
    @Autowired
    public OAuthService(DiscordOauthTokenDAO dao, DiscordApiClient api){
        this.discordDao = dao;
        this.discordApi = api;
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
        
        UserId userId = discordApi.fetchUserId(tokenValue);

        try {
            discordDao.selectOne(userId);
        }catch(DbRecordNotFoundException nfe) {
            //insert
            DiscordOauthToken newRecord = new DiscordOauthToken();
            newRecord.setUserId(userId);
            newRecord.setAccessToken(tokenValue);
            newRecord.setRefreshToken(refreshToken);
            newRecord.setScope(scope);
            newRecord.setExpireAt(expire);
            discordDao.insert(newRecord);
            return;
        }
        {
            //update
            DiscordOauthToken newData = new DiscordOauthToken();
            //TODO input
            discordDao.update(userId, newData);
            return;
        }
    }
}
