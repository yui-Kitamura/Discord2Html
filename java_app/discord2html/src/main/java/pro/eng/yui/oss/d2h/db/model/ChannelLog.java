package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.IsChanged;
import pro.eng.yui.oss.d2h.db.field.Status;

import java.sql.Timestamp;
import java.util.Objects;

public class ChannelLog {
    
    /** DiscordチャンネルID */
    private ChannelId channel_id;
    public void setChannelId(ChannelId newValue){
        this.channel_id = newValue;
    }
    public ChannelId getChannelId(){
        return channel_id;
    }
    
    /** タイムスタンプ */
    private Timestamp time_stamp;
    public void setTimeStamp(Timestamp timestamp){
        this.time_stamp = timestamp;
    }
    public Timestamp getTimeStamp(){
        return time_stamp;
    }
    
    /** 設定変更Flg */
    private IsChanged is_changed;
    public void setIsChanged(IsChanged isChanged){
        this.is_changed = isChanged;
    }
    public IsChanged getIsChanged(){
        return is_changed;
    }    
            
    /** チャンネル記録設定 */
    private Status status;
    public void setStatus(Status newValue){
        this.status = newValue;
    }
    public Status getStatus() {
        return status;
    }

    public ChannelLog(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (channel_id == null ? 0 : channel_id.hashCode());
        hash = hash * 31 + (time_stamp == null ? 0 : time_stamp.hashCode());
        hash = hash * 31 + (is_changed == null ? 0 : is_changed.hashCode());
        hash = hash * 31 + (status == null ? 0 : status.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        ChannelLog other = (ChannelLog) obj;
        if(!Objects.equals(channel_id, other.channel_id)){
            return false;
        }
        if(!Objects.equals(time_stamp, other.time_stamp)){
            return false;
        }
        if(!Objects.equals(is_changed, other.is_changed)){
            return false;
        }
        if(!Objects.equals(status, other.status)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
