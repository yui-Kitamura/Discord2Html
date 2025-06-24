package pro.eng.yui.oss.d2h.db.field;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class ExpireAt extends AbstTimestamp {
    
    public ExpireAt(long expireIn){
        this(new Date(new Date().toInstant().plusSeconds(expireIn).toEpochMilli()));
    }
    public ExpireAt(IssuedAt issued, long expireIn){
        this(new Date(issued.getValue().toInstant().plusSeconds(expireIn).toEpochMilli()));
    }
    
    public ExpireAt(Date date){
        super(date);
    }
    public ExpireAt(Timestamp ts){
        super(ts);
    }
    public ExpireAt(Calendar c){
        super(c);
    }
    
    public boolean isExpired(){
        return getValue().getTime() < System.currentTimeMillis();
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

}
