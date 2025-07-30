package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.channel.Channel;

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
    
    public ChannelInfo(Channel ch){
        this.name = ch.getName();
    }
}
