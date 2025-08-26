package pro.eng.yui.oss.d2h.db.field;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;

public abstract class AbstTimestamp {
    
    private final Timestamp timestamp;
    public Timestamp getValue(){
        return timestamp;
    }

    public AbstTimestamp(Date date){
        this.timestamp = new Timestamp(date.getTime());
    }
    public AbstTimestamp(Timestamp ts){
        this.timestamp = ts;
    }
    public AbstTimestamp(Calendar c){
        this.timestamp = new Timestamp(c.getTimeInMillis());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + timestamp.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstTimestamp other = (AbstTimestamp) obj;
        if(!this.timestamp.equals(other.timestamp)){ return false; }
        return true;
    }

    @Override
    public String toString() {
        return DateTimeUtil.mill().format(timestamp);
    }



}
