package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

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

    private long channelId;
    public long getChannelId() { return channelId; }

    private boolean thread;
    public boolean isThread() { return thread; }

    private String parentChannelName;
    public String getParentChannelName() { return parentChannelName; }

    private Long parentChannelId;
    public Long getParentChannelId() { return parentChannelId; }
    
    public ChannelInfo(GuildMessageChannel ch){
        this.name = ch.getName();
        this.guildId = ch.getGuild().getIdLong();
        this.channelId = ch.getIdLong();
        this.thread = ch.getType() != null && ch.getType().isThread();
        if (this.thread) {
            try {
                ThreadChannel tc = (ThreadChannel) ch;
                if (tc.getParentMessageChannel() != null) {
                    this.parentChannelName = tc.getParentMessageChannel().getName();
                    this.parentChannelId = tc.getParentMessageChannel().getIdLong();
                }
            } catch (ClassCastException ignore) {
                // fallback: leave parent fields null
            }
        }
    }
}
