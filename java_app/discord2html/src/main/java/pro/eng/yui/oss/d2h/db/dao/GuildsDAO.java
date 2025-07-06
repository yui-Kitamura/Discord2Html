package pro.eng.yui.oss.d2h.db.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.exception.DbRecordNotFoundException;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.mapper.GuildsMapper;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.Objects;

@Service
public class GuildsDAO {
    
    private final GuildsMapper mapper;
    
    @Autowired
    public GuildsDAO(GuildsMapper guildsMapper){
        this.mapper = guildsMapper;
    }
    
    public Guilds selectGuildInfo(GuildId guildId){
        Guilds param = new Guilds();
        try {
            param.setGuildId(Objects.requireNonNull(guildId));
        }catch(NullPointerException npe) {
            throw new IllegalArgumentException(npe);
        }
        Guilds res = mapper.selectOne(param);
        if(res == null) {
            throw new DbRecordNotFoundException("not found with " + guildId);
        }
        return res;
    }
    
    public boolean exists(GuildId id){
        try {
            Guilds info = selectGuildInfo(id);
            return id.equals(info.getGuildId()); //念のため
        }catch(IllegalArgumentException | DbRecordNotFoundException err) {
            return false;
        }
    }
    
    public void upsertGuildInfo(Guilds newRecord){
        if(exists(newRecord.getGuildId())) {
            mapper.update(newRecord);
        }else{
            mapper.register(newRecord);
        }
    }
   
}
