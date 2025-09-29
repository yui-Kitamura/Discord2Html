package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.OnRunMessage;
import pro.eng.yui.oss.d2h.db.field.OnRunUrl;
import pro.eng.yui.oss.d2h.db.field.Status;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.List;

@Component
public class ArchiveConfigRunner implements IRunner {
    
    private final ChannelsDAO channelDao;
    private final GuildsDAO guildsDao;
    
    private String afterMessage = "";
    
    @Autowired
    public ArchiveConfigRunner(ChannelsDAO c, GuildsDAO g){
        this.channelDao = c;
        this.guildsDao = g;
    }
    
    public void run(@NotNull Guild guild, List<OptionMapping> options){
        afterMessage = "";
        GuildChannel targetCh = null;
        Status newMode = null;
        String onRunMessageStr = null;
        String onRunUrlStr = null;
        for(OptionMapping op : options) {
            if("channel".equals(op.getName())) {
                 targetCh = op.getAsChannel();
                 continue;
            }
            if("mode".equals(op.getName())) {
                newMode = new Status(op.getAsString());
                continue;
            }
            if("onrunmessage".equals(op.getName())) {
                onRunMessageStr = op.getAsString();
                continue;
            }
            if("onrunurl".equals(op.getName())) {
                onRunUrlStr = op.getAsString();
                continue;
            }
        }

        // Resolve guildId: prefer channel's guild
        GuildId guildId = null;
        if (targetCh != null) {
            try {
                guildId = new GuildId(opChannelGuildId(options));
            } catch (Exception ignore) { /* best effort */ }
        }
        if (guildId == null && guild != null) {
            guildId = new GuildId(guild.getIdLong());
        }

        boolean hasChannel = (targetCh != null);
        boolean hasMode = (newMode != null);
        if (hasChannel ^ hasMode) { // xor
            throw new IllegalArgumentException("channel and mode must be specified together");
        }

        // 1) update channel mode when both provided
        if (hasChannel && hasMode) {
            ChannelId targetChId = new ChannelId(targetCh);
            if (!channelDao.exists(targetChId)) {
                Channels chRec = new Channels(targetCh);
                channelDao.upsertChannelInfo(chRec);
            }
            channelDao.updateChannelStatus(targetChId, newMode);
        }
        
        // 2) persist guild-level settings if provided
        if (onRunMessageStr != null || onRunUrlStr != null) {
            if (guildId == null) {
                throw new IllegalArgumentException("failed to resolve guildId");
            }
            Guilds g = new Guilds();
            g.setGuildId(guildId);
            if (onRunMessageStr != null) {
                g.setOnRunMessage(new OnRunMessage(onRunMessageStr));
            }
            if (onRunUrlStr != null) {
                g.setOnRunUrl(new OnRunUrl(onRunUrlStr));
            }
            guildsDao.upsertGuildInfo(g);
        }
    }

    private long opChannelGuildId(List<OptionMapping> options) {
        for (OptionMapping op : options) {
            if ("channel".equals(op.getName())) {
                return op.getAsChannel().getGuild().getIdLong();
            }
        }
        throw new IllegalArgumentException("channel option is missed");
    }

    @Override
    public String afterRunMessage() {
        if (afterMessage == null || afterMessage.isEmpty()) {
            return "archive setting has changed";
        }
        return afterMessage;
    }
}
