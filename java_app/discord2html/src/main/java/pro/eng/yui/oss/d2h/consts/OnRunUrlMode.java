package pro.eng.yui.oss.d2h.consts;

public enum OnRunUrlMode {
    SHARE,
    DENY;

    public static OnRunUrlMode get(String value){
        for (OnRunUrlMode m : OnRunUrlMode.values()) {
            if (m.name().equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("not defined: " + value);
    }

    public static boolean validName(String value){
        try {
            get(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isShare(){ return this == SHARE; }
    public boolean isDeny(){ return this == DENY; }
}
