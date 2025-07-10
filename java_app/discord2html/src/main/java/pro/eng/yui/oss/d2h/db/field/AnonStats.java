package pro.eng.yui.oss.d2h.db.field;

import pro.eng.yui.oss.d2h.consts.UserAnon;

public class AnonStats extends AbstVarChar {

    public static int LIMIT = 10;
    
    public AnonStats(UserAnon value){
        super(value.name(), LIMIT);
    }
    public AnonStats(String value){
        super(UserAnon.get(value).name(), LIMIT);
    }
    
    public UserAnon get(){
        return UserAnon.get(value);
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
