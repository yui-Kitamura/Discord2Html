package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.channel.Channel;

public class ChannelId extends AbstIds{
    
    public ChannelId(long value){
        super(value);
    }
    public ChannelId(Channel channel){
        super(channel.getIdLong());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean s = super.equals(obj);
        if(s) {
            //追加要素あればここで検証
            return true;
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
