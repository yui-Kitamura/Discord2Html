package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBotUtils;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.List;

/**
 * サーバーの自動アーカイブ実行周期（h）を設定するRunner（起算は 0:00）
 * Options:
 *  - cycle (optional): 0-23 の整数。未指定の場合は現在の設定を応答。
 */
@Component
public class AutoArchiveScheduleRunner implements IRunner {

    private final GuildsDAO guildsDAO;
    private final DiscordBotUtils discordBotUtils;

    private MessageEmbed lastRunsOnListMessage;

    @Autowired
    public AutoArchiveScheduleRunner(GuildsDAO guildsDAO, DiscordBotUtils discordBotUtils) {
        this.guildsDAO = guildsDAO;
        this.discordBotUtils = discordBotUtils;
        this.lastRunsOnListMessage = discordBotUtils.buildStatusEmbed(INFO, "auto archive schedule has changed");
    }

    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.D2H_ADMIN;
    }
    
    public void run(Guild guild, List<OptionMapping> options) {
        Integer cycle = null;
        for (OptionMapping op : options) {
            if ("cycle".equals(op.getName())) {
                cycle = op.getAsInt();
            }
        }
        Guilds current = guildsDAO.selectGuildInfo(new GuildId(guild));

        if (cycle == null) {
            // 変更なし: 現在の設定を返答
            lastRunsOnListMessage = discordBotUtils.buildStatusEmbed(
                    INFO, "current scheduled hours are: " + buildRunsOnListString(current.getRunsOnList()));
            return;
        }
        if (cycle < 0 || 23 < cycle) {
            throw new IllegalArgumentException("cycle must be between 0 and 23");
        }
        // 更新して保存
        current.setRunsOn(new RunsOn(cycle));
        guildsDAO.upsertGuildInfo(current);

        // 最新のスケジュールを作成
        lastRunsOnListMessage = discordBotUtils.buildStatusEmbed(SUCCESS,
                "new scheduled hours are: " + buildRunsOnListString(current.getRunsOnList()));
    }

    private String buildRunsOnListString(List<RunsOn> runsList) {
        if (runsList == null || runsList.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < runsList.size(); i++) {
            sb.append(runsList.get(i).toString());
            if (i < runsList.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public MessageEmbed afterRunMessage() {
        return lastRunsOnListMessage;
    }
}
