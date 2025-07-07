package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstVarChar {

    protected final String value;
    private final int length;

    public String getValue() {
        return value;
    }

    public AbstVarChar(String value, int limit){
        if(value == null) {
            throw new IllegalArgumentException(new NullPointerException("value"));
        }
        if(limit < 1) {
            throw new IllegalArgumentException();
        }
        this.value = value;
        length = limit;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + value.hashCode();
        hash = hash * 31 + Integer.hashCode(length);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstVarChar other = (AbstVarChar) obj;
        if(!(this.value.equals(other.value)) ){ return false; }
        if(this.length != other.length){ return false; }
        return true;
    }
    
    public boolean equalsIgnoreCase(Object obj){
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstVarChar other = (AbstVarChar) obj;
        if(!(this.value.equalsIgnoreCase(other.value)) ){ return false; }
        if(this.length != other.length){ return false; }
        return true;
    }

    @Override
    public String toString() {
        return value +"("+ value.length() +"/"+ length +")" ;
    }
}
