package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    
}
