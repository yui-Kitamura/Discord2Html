package pro.eng.yui.oss.d2h.db.field;

public class TokenType extends AbstVarChar {
    
    public static int LIMIT = 32;
    
    public TokenType(String value){
        super(value, LIMIT);
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
