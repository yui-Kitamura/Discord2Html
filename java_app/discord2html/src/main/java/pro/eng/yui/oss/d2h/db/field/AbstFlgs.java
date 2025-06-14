package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstFlgs {

    protected final boolean value;

    public boolean getValue() {
        return value;
    }
    public boolean isTrue(){
        return value;
    }
    public boolean isFalse(){
        return !value;
    }

    public AbstFlgs(boolean value){
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Boolean.hashCode(value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstFlgs other = (AbstFlgs) obj;
        if(this.value != other.value){ return false; }
        return true;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
