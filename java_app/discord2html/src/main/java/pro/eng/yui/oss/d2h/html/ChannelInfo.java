package pro.eng.yui.oss.d2h.html;

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
    
    public ChannelInfo(GuildMessageChannel ch){
        this.name = ch.getName();
        this.guildId = ch.getGuild().getIdLong();
    }
}
