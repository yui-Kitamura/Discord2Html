package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
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
            dbParam.setTokenType(newRecord.getTokenType());
            dbParam.setScope(newRecord.getScope());
            dbParam.setExpireAt(newRecord.getExpireAt());
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        mapper.insertToken(dbParam);
        return mapper.findByUserId(dbParam.getUserId());
    }
    
    public DiscordOauthToken update(UserId key, DiscordOauthToken newRecord) throws DbRecordNotFoundException {
        DiscordOauthToken updateKey = new DiscordOauthToken();
        DiscordOauthToken updateParam = new DiscordOauthToken();
        try {
            updateKey.setUserId(Objects.requireNonNull(key));
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException("required update key was null", npe);
        }
        try {
            updateParam.setAccessToken(newRecord.getAccessToken());
            updateParam.setExpireAt(newRecord.getExpireAt());
            if(newRecord.getRefreshToken() != null) {
                updateParam.setRefreshToken(newRecord.getRefreshToken());
            }
            if(newRecord.getScope() != null) {
                updateParam.setScope(newRecord.getScope());
            }
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }

        mapper.updateToken(updateKey, updateParam);
        return mapper.findByUserId(updateKey.getUserId());
    }
    
    public DiscordOauthToken selectOne(UserId keyId) throws DbRecordNotFoundException {
        //TODO implement
        return null;
    }

}
