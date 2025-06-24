package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.mapper.DiscordOauthTokenMapper;
import pro.eng.yui.oss.d2h.db.model.DiscordOauthToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            dbParam.setExpiresAt(newRecord.getExpiresAt());
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
            updateParam.setExpiresAt(newRecord.getExpiresAt());
            if(newRecord.getRefreshToken() != null) {
                updateParam.setRefreshToken(newRecord.getRefreshToken());
            }
            if(newRecord.getScope() != null) {
                updateParam.setScope(newRecord.getScope());
            }
        }catch (NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }

        Map<String,Object> params = new HashMap<>();
        params.put("key", updateKey);
        params.put("token", updateParam);
        mapper.updateToken(params);
        return mapper.findByUserId(updateKey.getUserId());
    }
    
    public DiscordOauthToken select() throws DbRecordNotFoundException {
        List<DiscordOauthToken> data = mapper.select();
        if(data == null || data.isEmpty()) {
            throw new DbRecordNotFoundException("no record");
        }
        return data.get(0); //TODO 複数件取れる条件と対応を検討
    }

}
