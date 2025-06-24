package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.dao.DiscordOauthTokenDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;
import pro.eng.yui.oss.d2h.db.model.Users;

@Service
public class OAuthService {
    
    private final DiscordOauthTokenDAO discordDao;
    private final DiscordApiClient discordApi;
    private final UsersDAO usersDao;
    
    @Autowired
    public OAuthService(DiscordOauthTokenDAO dao, DiscordApiClient api,
                        UsersDAO usersDAO){
        this.discordDao = dao;
        this.discordApi = api;
        this.usersDao = usersDAO;
    }
    
    public ResponseToken callApiGetAccessTokenByCode(String code){
        return discordApi.exchangeCodeForToken(code);
    }
    
    public void registerOrUpdateNewToken(Users userInfo, ResponseToken tokenInfo){
        DiscordOauthToken newRecord = new DiscordOauthToken(userInfo.getUserId(), tokenInfo);
        try {
            try {
                usersDao.select(userInfo.getUserId());
            }catch(DbRecordNotFoundException nfe) {
                usersDao.insert(userInfo);
            }
            discordDao.selectOne(userInfo.getUserId());
        }catch(DbRecordNotFoundException nfe) {
            discordDao.insert(newRecord);
            return;
        }
        {
            discordDao.update(userInfo.getUserId(), newRecord);
            return;
        }
    }
    
    public Users getUserByToken(AccessToken accessToken){
        return discordApi.fetchUserByToken(accessToken);
    }
}
