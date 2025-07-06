package pro.eng.yui.oss.d2h.db.field;

public class AnonCycle extends AbstInt {
    
    public AnonCycle(int value){
        super(value);
        if(value < 1 || 24 < value) {
            throw new IllegalArgumentException("runs on time value must between 1-24");
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

