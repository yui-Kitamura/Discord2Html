package pro.eng.yui.oss.d2h.db.mapper;

import org.apache.ibatis.annotations.Mapper;
import pro.eng.yui.oss.d2h.db.model.Channels;

import java.util.List;

@Mapper
public interface ChannelsMapper {
    
    List<Channels> selectChannelArchiveDo(Channels channels);
    
    void register(Channels newRecord);
    
    void update(Channels param);
    
    Channels selectOne(Channels key);
    
}
