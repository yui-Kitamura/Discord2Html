package pro.eng.yui.oss.d2h.db.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelIdTest {

    @Test
    void testEquals() {
        ChannelId c1 = new ChannelId(12345L);
        ChannelId c2 = new ChannelId(54321L);
        assertNotEquals(c1, c2);
    }
    
    @Test
    void testEquals2(){
        ChannelId c1 = new ChannelId(12345L);
        ChannelId c2 = new ChannelId(12345L);
        assertEquals(c1, c2);
    }

    @Test
    void testEqualsWithDifferentType() {
        ChannelId channelId = new ChannelId(12345L);
        UserId userId = new UserId(12345L);
        assertNotEquals(channelId, userId);
    }
}