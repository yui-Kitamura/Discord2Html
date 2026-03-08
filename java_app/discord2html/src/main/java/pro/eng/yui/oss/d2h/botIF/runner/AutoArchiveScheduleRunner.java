package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.RunsOn;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import pro.eng.yui.oss.d2h.botIF.i.MessageKey;
import pro.eng.yui.oss.d2h.botIF.i.MessageKeys;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;

import java.awt.*;
import java.util.List;

/**
 * サーバーの自動アーカイブ実行周期（h）を設定するRunner（起算は 0:00）
 * Options:
 *  - cycle (optional): 0-23 の整数。未指定の場合は現在の設定を応答。
 */
@Component
public class AutoArchiveScheduleRunner implements IRunner {

    private final GuildsDAO guildsDAO;

    private MessageKey lastMessageKey;
    private Object[] lastMessageArgs;
    private Color lastMessageColor;

    @Autowired
    public AutoArchiveScheduleRunner(GuildsDAO guildsDAO) {
        this.guildsDAO = guildsDAO;
        this.lastMessageKey = MessageKeys.RUNNER_AUTO_ARCHIVE_SUCCESS;
        this.lastMessageArgs = new Object[0];
        this.lastMessageColor = INFO;
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
            this.lastMessageColor = INFO;
            this.lastMessageKey = MessageKeys.RUNNER_AUTO_ARCHIVE_CURRENT;
            this.lastMessageArgs = new Object[]{ buildRunsOnListString(current.getRunsOnList()) };
            return;
        }
        if (cycle < 0 || 23 < cycle) {
            throw new IllegalArgumentException("cycle must be between 0 and 23");
        }
        // 更新して保存
        current.setRunsOn(new RunsOn(cycle));
        guildsDAO.upsertGuildInfo(current);

        // 最新のスケジュールを作成
        this.lastMessageColor = SUCCESS;
        this.lastMessageKey = MessageKeys.RUNNER_AUTO_ARCHIVE_NEW;
        this.lastMessageArgs = new Object[]{ buildRunsOnListString(current.getRunsOnList()) };
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
    public MessageSeed afterRunMessage() {
        return new MessageSeed(lastMessageColor, lastMessageKey, lastMessageArgs);
    }
}
