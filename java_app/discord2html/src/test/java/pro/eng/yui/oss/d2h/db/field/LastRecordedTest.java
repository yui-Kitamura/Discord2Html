package pro.eng.yui.oss.d2h.db.field;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class LastRecordedTest {

    @Test
    void testEquals() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JUNE, 14, 15, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        Timestamp ts = new Timestamp(cal.getTimeInMillis());
        Date date = new Date(cal.getTimeInMillis());

        LastRecorded lr1 = new LastRecorded(ts);
        LastRecorded lr2 = new LastRecorded(date);
        LastRecorded lr3 = new LastRecorded(cal);

        assertEquals(lr1, lr2);
        assertEquals(lr2, lr3);
        assertEquals(lr1, lr3);

        Calendar diffCal = Calendar.getInstance();
        diffCal.setTimeInMillis(cal.getTimeInMillis() + 1000);
        LastRecorded lr4 = new LastRecorded(diffCal);

        assertNotEquals(lr4, lr1);
        assertNotEquals(null, lr1);
        assertNotEquals("not a LastRecorded object", lr1);
    }

    @Test
    void testToString() {
        Calendar cal = Calendar.getInstance();

        // Morning time test
        cal.set(2025, Calendar.JUNE, 14, 9, 5, 45);
        cal.set(Calendar.MILLISECOND, 123);
        LastRecorded morning = new LastRecorded(cal);
        assertEquals("25/06/14 09:05:45.123", morning.toString());

        // Afternoon time test
        cal.set(Calendar.HOUR_OF_DAY, 15);
        cal.set(Calendar.MINUTE, 30);
        LastRecorded afternoon = new LastRecorded(cal);
        assertEquals("25/06/14 15:30:45.123", afternoon.toString());
    }
}