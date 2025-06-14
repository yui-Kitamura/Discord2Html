package pro.eng.yui.oss.d2h.db.field;

import pro.eng.yui.oss.d2h.consts.ChannelStatus;

public class Status extends AbstVarChar {
    
    public static int LIMIT = 50;
    
    public Status(String value){
        super(value, LIMIT);
        if(ChannelStatus.validName(value) == false) {
            throw new IllegalArgumentException(value);
        }
    }
    public Status(ChannelStatus status){
        super(status.name(), LIMIT);
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
