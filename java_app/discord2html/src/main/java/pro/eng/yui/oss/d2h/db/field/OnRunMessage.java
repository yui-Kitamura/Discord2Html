package pro.eng.yui.oss.d2h.db.field;

import pro.eng.yui.oss.d2h.consts.OnRunMessageMode;

public class OnRunMessage extends AbstVarChar {

    public static int LIMIT = 10;

    public OnRunMessage(OnRunMessageMode value){
        super(value.name(), LIMIT);
    }
    public OnRunMessage(String value){
        super(OnRunMessageMode.get(value).name(), LIMIT);
    }

    public OnRunMessageMode get(){
        return OnRunMessageMode.get(value);
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
