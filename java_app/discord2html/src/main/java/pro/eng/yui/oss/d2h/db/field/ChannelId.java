package pro.eng.yui.oss.d2h.db.field;

public class ChannelId extends AbstIds{
    
    public ChannelId(long value){
        super(value);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean s = super.equals(obj);
        if(s) {
            if(!(this.getClass().equals(obj.getClass()))){ return false; }
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
