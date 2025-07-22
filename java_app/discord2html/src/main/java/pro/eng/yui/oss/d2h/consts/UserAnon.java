package pro.eng.yui.oss.d2h.consts;

public enum UserAnon {
    ANONYMOUS,
    OPEN;

    public static UserAnon get(String value){
        for(UserAnon ua : UserAnon.values()) {
            if (ua.name().equalsIgnoreCase(value)) {
                return ua;
            }
        }
        throw new IllegalArgumentException("not defined: "+ value);
    }

    /**
     * <code>true</code> then <code>Anonymous</code>
     */
    public static UserAnon get(boolean bool){
        if(bool) {
            return ANONYMOUS;
        }else {
            return OPEN;
        }
    }
    
    public static boolean validName(String value){
        try {
            UserAnon.get(value);
            return true;
        }catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public boolean isAnon(){
        return this == ANONYMOUS;
    }
    public boolean isOpen(){
        return this == OPEN;
    }
}
