package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.List;

/**
 * サーバーの自動アーカイブ実行周期（h）を設定するRunner（起算は 0:00）
 * Options:
 *  - cycle (required): 0-23 の整数。
 */
@Component
public class AutoArchiveScheduleRunner implements IRunner {

    private final GuildsDAO guildsDAO;

    @Autowired
    public AutoArchiveScheduleRunner(GuildsDAO guildsDAO) {
        this.guildsDAO = guildsDAO;
    }

    public void run(Guild guild, List<OptionMapping> options) {
        Integer cycle = null;
        for (OptionMapping op : options) {
            if ("cycle".equals(op.getName())) {
                cycle = op.getAsInt();
            }
        }
        if (cycle == null) {
            throw new IllegalArgumentException("required parameter cycle is missed");
        }
        if (cycle < 0 || 23 < cycle) {
            throw new IllegalArgumentException("cycle must be between 0 and 23");
        }
        Guilds current = guildsDAO.selectGuildInfo(new GuildId(guild));
        current.setRunsOn(new RunsOn(cycle));
        guildsDAO.upsertGuildInfo(current);
    }

    @Override
    public String afterRunMessage() {
        return "auto archive schedule has changed";
    }
}
