package pro.eng.yui.oss.d2h.db.dao;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.OptinTimestamp;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.mapper.OptoutMapper;
import pro.eng.yui.oss.d2h.db.model.Optout;

import java.util.List;
import java.util.Objects;

@Service
public class OptoutDAO {

    private final OptoutMapper mapper;

    @Autowired
    public OptoutDAO(OptoutMapper mapper) {
        this.mapper = mapper;
    }

    public void insert(Optout record) {
        Objects.requireNonNull(record.getUserId(), "userId is required");
        Objects.requireNonNull(record.getGuildId(), "guildId is required");
        mapper.insert(record);
    }

    public void update(Optout record) {
        Objects.requireNonNull(record.getUserId(), "userId is required");
        Objects.requireNonNull(record.getGuildId(), "guildId is required");
        mapper.update(record);
    }

    public void optin(UserId userId, GuildId guildId, @Nullable ChannelId channelId, @Nullable OptinTimestamp ts){
        Optout key = new Optout();
        key.setUserId(Objects.requireNonNull(userId));
        key.setGuildId(Objects.requireNonNull(guildId));
        key.setChannelId(channelId); // nullable for guild-wide
        key.setOptinTimestamp(ts); // nullable -> CURRENT_TIMESTAMP in SQL
        mapper.optin(key);
    }

    public Optout selectOne(UserId userId, GuildId guildId, ChannelId channelId) {
        Optout key = new Optout();
        key.setUserId(Objects.requireNonNull(userId));
        key.setGuildId(Objects.requireNonNull(guildId));
        key.setChannelId(channelId);
        Optout res = mapper.selectOne(key);
        if(res == null){
            throw new DbRecordNotFoundException("optout not found");
        }
        return res;
    }

    public List<Optout> selectAllByUserGuild(UserId userId, GuildId guildId){
        Optout key = new Optout();
        key.setUserId(Objects.requireNonNull(userId));
        key.setGuildId(Objects.requireNonNull(guildId));
        return mapper.selectAllByUserGuild(key);
    }
}
