package pro.eng.yui.oss.d2h.consts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAnonTest {
    
    @Test
    void testIsAnon(){
        UserAnon anon = UserAnon.ANONYMOUS;
        UserAnon open = UserAnon.OPEN;
        assertAll(
                ()->assertTrue(anon.isAnon()),
                ()->assertFalse(open.isAnon())
        );
    }
    @Test
    void testIsOpen(){
        UserAnon anon = UserAnon.ANONYMOUS;
        UserAnon open = UserAnon.OPEN;
        assertAll(
                ()->assertFalse(anon.isAnon()),
                ()->assertTrue(open.isAnon())
        );
    }

}