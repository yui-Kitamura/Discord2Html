package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstInt {

    protected final int value;

    public int getValue() {
        return value;
    }

    public AbstInt(int value){
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Integer.hashCode(value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstInt other = (AbstInt) obj;
        if(this.value != other.value){ return false; }
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
}
