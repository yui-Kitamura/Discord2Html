package pro.eng.yui.oss.d2h.db.field;

public class IsChanged extends AbstFlgs {
    
    public IsChanged(boolean value){
        super(value);
    }
    
    public static IsChanged CHANGED = new IsChanged(true);
    public static IsChanged NOT_CHANGED = new IsChanged(false);

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

