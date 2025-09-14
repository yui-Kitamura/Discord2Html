package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import pro.eng.yui.oss.d2h.db.field.ChannelId;

/**
 * Templateファイルで使用するチャンネル情報
 */
public class ChannelInfo {
    
    private String name;
    public void setName(String newValue){
        this.name = newValue;
    }
    public String getName(){
        return name;
    }

    private long guildId;
    public long getGuildId() {
        return guildId;
    }

    private ChannelId channelId;
    public ChannelId getChannelId() { return channelId; }

    private boolean thread;
    public boolean isThread() { return thread; }

    private String parentChannelName;
    public String getParentChannelName() { return parentChannelName; }

    private ChannelId parentChannelId;
    public ChannelId getParentChannelId() { return parentChannelId; }
    
    public ChannelInfo(GuildChannel ch){
        this.name = ch.getName();
        this.guildId = ch.getGuild().getIdLong();
        this.channelId = new ChannelId(ch);
        this.thread = ch.getType().isThread();
        if (this.thread) {
            try {
                ThreadChannel tc = (ThreadChannel) ch;
                this.parentChannelName = tc.getParentChannel().getName();
                this.parentChannelId = new ChannelId(tc.getParentChannel());
            } catch (ClassCastException ignore) {
                // fallback: leave parent fields null
            }
        }
    }
}
