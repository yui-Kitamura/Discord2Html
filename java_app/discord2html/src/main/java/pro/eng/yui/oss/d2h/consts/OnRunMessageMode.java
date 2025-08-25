package pro.eng.yui.oss.d2h.consts;

public enum OnRunMessageMode {
    START,
    END,
    BOTH;

    public static OnRunMessageMode get(String value){
        for (OnRunMessageMode m : OnRunMessageMode.values()) {
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

    public boolean isStart(){ return this == START; }
    public boolean isEnd(){ return this == END; }
    public boolean isBoth(){ return this == BOTH; }
}
