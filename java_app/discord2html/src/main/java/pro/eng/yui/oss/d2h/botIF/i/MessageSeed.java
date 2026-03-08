package pro.eng.yui.oss.d2h.botIF.i;

import java.awt.*;

public class MessageSeed {
    
    final private Color color;
    public Color getStatsColor(){
        return color;
    }
    
    final private MessageKey key;
    public MessageKey getKey() {
        return key;
    }
    
    final private Object[] args;
    public Object[] getArgs(){
        return args;
    }

    public MessageSeed(Color stats, MessageKey key, Object... args) {
        this.color = stats;
        this.key = key;
        this.args = args;
    }

}
