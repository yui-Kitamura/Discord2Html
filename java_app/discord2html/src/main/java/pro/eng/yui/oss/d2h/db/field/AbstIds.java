package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstIds {

    protected final long value;

    public long getValue() {
        return value;
    }

    public AbstIds(long value){
        if(value < 0) {
            throw new IllegalArgumentException();
        }
        this.value = value;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + Long.hashCode(value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        AbstIds other = (AbstIds) obj;
        if(this.value != other.value){ return false; }
        return true;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
