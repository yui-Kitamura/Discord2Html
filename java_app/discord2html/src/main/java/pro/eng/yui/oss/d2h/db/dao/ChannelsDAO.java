package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.mapper.ChannelsMapper;
import pro.eng.yui.oss.d2h.db.model.Channels;

import java.util.List;
import java.util.Objects;

@Service
public class ChannelsDAO {
    
    private final ChannelsMapper mapper;
    
    @Autowired
    public ChannelsDAO(ChannelsMapper channelsMapper){
        this.mapper = channelsMapper;
    }
    
    public List<Channels> selectChannelArchiveDo(GuildId guildId){
        Channels param = new Channels();
        try {
            param.setGuildId(Objects.requireNonNull(guildId));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        return mapper.selectChannelArchiveDo(param);
    }
    
    public Channels selectChannelInfo(ChannelId channelId){
        Channels param = new Channels();
        try {
            param.setChannelId(Objects.requireNonNull(channelId));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        Channels res = mapper.selectOne(param);
        if(res == null) {
            throw new DbRecordNotFoundException("not found with " + channelId);
        }
        return res;
    }
    
    public boolean exists(ChannelId id){
        try {
            Channels info = selectChannelInfo(id);
            return id.equals(info.getChannelId()); //念のため
        }catch(IllegalArgumentException | DbRecordNotFoundException err) {
            return false;
        }
    }
    
    public void upsertChannelInfo(Channels newRecord){
        if(exists(newRecord.getChannelId())) {
            mapper.update(newRecord);
        }else{
            mapper.register(newRecord);
        }
    }
    
    
}
