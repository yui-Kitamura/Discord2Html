package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.mapper.DiscordOauthTokenMapper;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

import java.util.Objects;

@Service
public class DiscordOauthTokenDAO {

    private final DiscordOauthTokenMapper mapper;

    @Autowired
    public DiscordOauthTokenDAO(DiscordOauthTokenMapper mapper) {
        this.mapper = mapper;
    }

    public DiscordOauthToken insert(DiscordOauthToken newRecord) {
        DiscordOauthToken dbParam = new DiscordOauthToken();
        try {
            dbParam.setUserId(Objects.requireNonNull(newRecord.getUserId()));
            dbParam.setAccessToken(Objects.requireNonNull(newRecord.getAccessToken()));
            dbParam.setRefreshToken(Objects.requireNonNull(newRecord.getRefreshToken()));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        mapper.insertToken(dbParam);
        return mapper.findByUserId(dbParam.getUserId());
    }
    
    public DiscordOauthToken update(UserId key, DiscordOauthToken newRecord){
        DiscordOauthToken updateKey = new DiscordOauthToken();
        DiscordOauthToken updateParam = new DiscordOauthToken();
        try {
            updateKey.setUserId(Objects.requireNonNull(key));
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException("required update key was null", npe);
        }
        try {
            updateParam.setAccessToken(newRecord.getAccessToken());
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        
        //TODO mapper.update
        return mapper.findByUserId(updateKey.getUserId());
    }

}
