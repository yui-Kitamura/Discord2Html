package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.DiscordOauthTokenDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

@Service
public class OAuthService {
    
    private final DiscordOauthTokenDAO discordDao;
    private final DiscordApiClient discordApi;
    
    @Autowired
    public OAuthService(DiscordOauthTokenDAO dao, DiscordApiClient api){
        this.discordDao = dao;
        this.discordApi = api;
    }
    
    public ResponseToken callApiGetAccessTokenByCode(String code){
        return discordApi.exchangeCodeForToken(code);
    }
    
    public void registerOrUpdateNewToken(UserId userId, ResponseToken tokenInfo){
        DiscordOauthToken newRecord = new DiscordOauthToken(userId, tokenInfo);
        try {
            discordDao.selectOne(userId);
        }catch(DbRecordNotFoundException nfe) {
            discordDao.insert(newRecord);
            return;
        }
        {
            discordDao.update(userId, newRecord);
            return;
        }
    }
    
    public UserId getUserIdByToken(AccessToken accessToken){
        return discordApi.fetchUserId(accessToken);
    }
}
