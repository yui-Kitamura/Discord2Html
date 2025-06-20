package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstText extends AbstVarChar{

    public AbstText(String value){
        super(value, Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        return value +"("+ value.length() +")" ;
    }
}
