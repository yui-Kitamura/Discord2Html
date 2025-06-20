package pro.eng.yui.oss.d2h.db.field;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class UpdatedAt extends AbstTimestamp {
    
    public UpdatedAt(Date date){
        super(date);
    }
    public UpdatedAt(Timestamp ts){
        super(ts);
    }
    public UpdatedAt(Calendar c){
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
