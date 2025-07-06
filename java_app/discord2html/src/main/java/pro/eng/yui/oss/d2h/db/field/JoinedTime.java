package pro.eng.yui.oss.d2h.db.field;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class JoinedTime extends AbstTimestamp {
    
    public JoinedTime(Date date){
        super(date);
    }
    public JoinedTime(Timestamp ts){
        super(ts);
    }
    public JoinedTime(Calendar c){
        super(c);
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
