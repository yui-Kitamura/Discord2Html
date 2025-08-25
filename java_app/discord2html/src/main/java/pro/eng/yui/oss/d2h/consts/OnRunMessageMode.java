package pro.eng.yui.oss.d2h.consts;

public enum OnRunMessageMode {
    ON,
    OFF;

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

    public boolean isOn(){ return this == ON; }
    public boolean isOff(){ return this == OFF; }
}
