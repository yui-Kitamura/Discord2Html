package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.AnonCycle;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import pro.eng.yui.oss.d2h.botIF.i.MessageKeys;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;

import java.util.List;

@Component
public class AnonymousSettingRunner implements IRunner {
    
    private final GuildsDAO guildDao;

    @Autowired
    public AnonymousSettingRunner(GuildsDAO g){
        this.guildDao = g;
    }
    
    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.D2H_ADMIN;
    }
    
    public void run(Guild guild, List<OptionMapping> command){
        boolean isConfigAnonymousCycle = false;

        for(OptionMapping op : command) {
            if("menu".equals(op.getName())) {
                if("cycle".equals(op.getAsString())) {
                    if (isConfigAnonymousCycle) {
                        continue;
                    }
                    isConfigAnonymousCycle = true;
                }
            }
            if("cycle".equals(op.getName())){
                if(isConfigAnonymousCycle) {
                    runCycle(guild, new AnonCycle(op.getAsInt()));
                }
            }
        }
    }

    @Override
    public MessageSeed afterRunMessage() {
        return new MessageSeed(INFO, MessageKeys.RUNNER_ANONYMOUS_SUCCESS);
    }
    
    private void runCycle(Guild guild, AnonCycle newValue) {
        Guilds current = guildDao.selectGuildInfo(new GuildId(guild));
        current.setAnonCycle(newValue);
        guildDao.upsertGuildInfo(current);
    }
}
