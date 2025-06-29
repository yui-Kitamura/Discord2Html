package pro.eng.yui.oss.d2h.db.field;

public class RunsOn2 extends RunsOn {
    
    public RunsOn2(int value){
        super(value);
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

