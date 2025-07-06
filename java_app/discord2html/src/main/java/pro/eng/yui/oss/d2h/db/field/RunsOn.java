package pro.eng.yui.oss.d2h.db.field;

public abstract class RunsOn extends AbstInt {
    
    public RunsOn(int value){
        super(value);
        if(value < 0 || 23 < value) {
            throw new IllegalArgumentException("runs on time value must between 0-23");
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean s = super.equals(obj);
        if(s) {
            //追加要素あればここで検証
            return true;
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

}

