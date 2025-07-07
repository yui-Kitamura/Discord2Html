package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.Status;

import java.util.List;

@Component
public class ArchiveConfigRunner implements IRunner {
    
    private final ChannelsDAO channelDao;
    
    @Autowired
    public ArchiveConfigRunner(ChannelsDAO c){
        this.channelDao = c;
    }
    
    public void run(Member member, List<OptionMapping> options){
        //TODO set archive settings with new value
        
        ChannelId targetCh = null;
        Status newMode = null;
        for(OptionMapping op : options) {
            if("channel".equals(op.getName())) {
                 targetCh = new ChannelId(op.getAsChannel());
                 continue;
            }
            if("mode".equals(op.getName())) {
                newMode = new Status(op.getAsString());
            }
        }
        
        if(targetCh != null && newMode != null) {
            channelDao.updateChannelStatus(targetCh, newMode);
        }else {
            throw new IllegalArgumentException("required parameter is missed");
        }
    }

    @Override
    public String afterRunMessage() {
        return "archive setting has changed";
    }
}
