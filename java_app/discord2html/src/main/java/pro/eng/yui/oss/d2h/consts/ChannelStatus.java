package pro.eng.yui.oss.d2h.consts;

public enum ChannelStatus {
    MONITOR,
    IGNORE,
    DELETED;
    
    public static ChannelStatus get(String value){
        for(ChannelStatus cs : ChannelStatus.values()) {
            if (cs.name().equalsIgnoreCase(value)) {
                return cs;
            }
        }
        throw new IllegalArgumentException("not defined: "+ value);
    }
    public static boolean validName(String value){
        try {
            ChannelStatus.get(value);
            return true;
        }catch (IllegalArgumentException e) {
            return false;
        }
    }
}
