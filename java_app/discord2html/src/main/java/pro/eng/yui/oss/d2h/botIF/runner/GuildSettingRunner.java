package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.i.MessageKeys;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.Lang;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.List;

@Component
public class GuildSettingRunner implements IRunner {

    private final GuildsDAO guildsDao;

    @Autowired
    public GuildSettingRunner(GuildsDAO guildsDao) {
        this.guildsDao = guildsDao;
    }

    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options) {
        return RequiredPermissionType.D2H_ADMIN;
    }

    public void run(@NotNull Guild guild, List<OptionMapping> options) {
        String langStr = null;
        for (OptionMapping op : options) {
            if ("lang".equals(op.getName())) {
                langStr = op.getAsString();
            }
        }

        if (langStr != null) {
            Guilds g = new Guilds();
            g.setGuildId(new GuildId(guild));
            g.setLang(new Lang(langStr));
            guildsDao.upsertGuildInfo(g);
        }
    }

    @Override
    public MessageSeed afterRunMessage() {
        return new MessageSeed(SUCCESS, MessageKeys.RUNNER_GUILD_SUCCESS);
    }
}
