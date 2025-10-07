package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.DiscordBotUtils;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Channels;
import pro.eng.yui.oss.d2h.db.model.Guilds;

import java.util.*;

@Component
public class ArchiveConfigRunner implements IRunner {
    
    private final ChannelsDAO channelDao;
    private final GuildsDAO guildsDao;
    private final DiscordBotUtils discordBotUtils;

    private String afterMessage = "";
    
    @Autowired
    public ArchiveConfigRunner(ChannelsDAO c, GuildsDAO g, DiscordBotUtils discordBotUtils){
        this.channelDao = c;
        this.guildsDao = g;
        this.discordBotUtils = discordBotUtils;
    }
    
    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        if(options.isEmpty()) {
            return RequiredPermissionType.ANY;
        }
        return RequiredPermissionType.D2H_ADMIN;
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
                guildId = opChannelGuildId(options);
            } catch (Exception ignore) { /* best effort */ }
        }
        if (guildId == null) {
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
        
        // 3) build response message
        afterMessage = buildArchiveTargetListMessage(guild);
    }

    /** list channels archived for this guild grouped by category in Discord order */
    private String buildArchiveTargetListMessage(Guild guild) {
        if (guild == null) {
            return "archive targets: (guild not resolved)";
        }
        GuildId guildId = new GuildId(guild);
        List<Channels> targetRecords = new ArrayList<>();
        try {
            targetRecords = channelDao.selectChannelArchiveDo(guildId);
        } catch (Exception ignore) { /* ignore */ }
        if (targetRecords == null || targetRecords.isEmpty()) {
            return "archive targets: (none)";
        }
        Set<ChannelId> targetChannels = new HashSet<>();
        for (Channels ch : targetRecords) {
            targetChannels.add(ch.getChannelId());
        }

        // Build groups by category id in Discord order
        Map<CategoryId, List<ChannelId>> groups = new LinkedHashMap<>();
        Map<CategoryId, CategoryName> categoryNames = new LinkedHashMap<>();

        List<GuildChannel> all = guild.getChannels(); // Discord order
        for (GuildChannel ch : all) {
            if (ch instanceof ThreadChannel) { continue; }
            if (ch instanceof Category cat) {
                // Ensure category key exists (even if no archived channels under it yet)
                CategoryId catId = new CategoryId(cat);
                groups.putIfAbsent(catId, new ArrayList<>());
                categoryNames.putIfAbsent(catId, new CategoryName(cat));
                continue;
            }
            ChannelId id = new ChannelId(ch);
            CategoryId catId = CategoryId.NO_CATEGORY;
            CategoryName catName = CategoryName.EMPTY;
            if (ch instanceof ICategorizableChannel cc && cc.getParentCategory() != null) {
                catId = new CategoryId(cc.getParentCategory());
                catName = new CategoryName(cc.getParentCategory());
            }
            groups.putIfAbsent(catId, new ArrayList<>());
            categoryNames.putIfAbsent(catId, catName);
            groups.get(catId).add(id);
        }
        // ギルドに現存しないチャンネル（remaining）は無視する

        // Remove non-target channels and empty groups
        List<CategoryId> emptyKeys = new ArrayList<>();
        for (Map.Entry<CategoryId, List<ChannelId>> e : groups.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                emptyKeys.add(e.getKey());
                continue;
            }
            e.getValue().removeIf(ch -> !targetChannels.contains(ch));
            if (e.getValue().isEmpty()) {
                emptyKeys.add(e.getKey());
            }
        }
        for (CategoryId k : emptyKeys) {
            groups.remove(k);
            categoryNames.remove(k);
        }

        if (groups.isEmpty()) {
            return "archive targets: (none)";
        }

        // Render output
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<CategoryId, List<ChannelId>> e : groups.entrySet()) {
            CategoryId catId = e.getKey();
            CategoryName cat = categoryNames.getOrDefault(catId, CategoryName.EMPTY);
            List<ChannelId> chIds = e.getValue();
            sb.append(cat.getValue()).append("\n");
            for (int i = 0; i < chIds.size(); i++) {
                ChannelId cid = chIds.get(i);
                boolean last = (i == chIds.size() - 1);
                sb.append(last ? "└ " : "├ ")
                  .append("<#").append(Objects.toString(cid.getValue())).append(">")
                  .append("\n");
            }
        }
        if (sb.isEmpty() == false) {
            sb.setLength(sb.length() - 1); // trim the last "\n"
        }
        return sb.toString();
    }

    private GuildId opChannelGuildId(List<OptionMapping> options) {
        OptionMapping channelOption = get(options, "channel");
        if(channelOption == null) {
            return null;
        }
        return new GuildId(channelOption.getAsChannel().getGuild());
    }

    @Override
    public MessageEmbed afterRunMessage() {
        if (afterMessage == null || afterMessage.isEmpty()) {
            afterMessage = "archive setting has changed";
        }
        return discordBotUtils.buildStatusEmbed(INFO, afterMessage);
    }
}
