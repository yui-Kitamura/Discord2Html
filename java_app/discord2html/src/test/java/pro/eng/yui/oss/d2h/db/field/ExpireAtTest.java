package pro.eng.yui.oss.d2h.db.field;

import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

class ExpireAtTest {

    @Test
    void isNotExpired() {
        Calendar later = Calendar.getInstance();
        later.add(Calendar.DATE, 1);
        
        ExpireAt ex = new ExpireAt(later);
        
        assertFalse(ex.isExpired());
    }
    
    @Test
    void isAlreadyExpired(){
        Calendar before = Calendar.getInstance();
        before.add(Calendar.DATE, -1);
        
        ExpireAt ex = new ExpireAt(before);
        
        assertTrue(ex.isExpired());
    }
}