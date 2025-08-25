package pro.eng.yui.oss.d2h.db.field;

import pro.eng.yui.oss.d2h.consts.OnRunUrlMode;

public class OnRunUrl extends AbstVarChar {

    public static int LIMIT = 10;

    public OnRunUrl(OnRunUrlMode value){
        super(value.name(), LIMIT);
    }
    public OnRunUrl(String value){
        super(OnRunUrlMode.get(value).name(), LIMIT);
    }

    public OnRunUrlMode get(){
        return OnRunUrlMode.get(value);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean s = super.equals(obj);
        if(s) {
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
