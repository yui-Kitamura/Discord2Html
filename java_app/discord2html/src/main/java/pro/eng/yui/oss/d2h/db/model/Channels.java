package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.ChannelName;

import java.util.Objects;

public class Channels {
    
    /** DiscordチャンネルID */
    private ChannelId channel_id;
    public void setChannelId(ChannelId newValue){
        this.channel_id = newValue;
    }
    public ChannelId getChannelId(){
        return channel_id;
    }
    
    /** Discordチャンネル名 */
    private ChannelName channel_name;
    public void setChannelName(ChannelName newValue){
        this.channel_name = newValue;
    }
    public ChannelName getChannelName() {
        return channel_name;
    }

    public Channels(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (channel_id == null ? 0 : channel_id.hashCode());
        hash = hash * 31 + (channel_name == null ? 0 : channel_name.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        Channels other = (Channels) obj;
        if(!Objects.equals(channel_id, other.channel_id)){
            return false;
        }
        if(!Objects.equals(channel_name, other.channel_name)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
