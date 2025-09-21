package pro.eng.yui.oss.d2h.db.field;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * 再同意(Opt-in)時刻
 */
public class OptinTimestamp extends AbstTimestamp {

    public OptinTimestamp(Date date) {
        super(date);
    }
    public OptinTimestamp(Timestamp ts) {
        super(ts);
    }
    public OptinTimestamp(Calendar c) {
        super(c);
    }
}
