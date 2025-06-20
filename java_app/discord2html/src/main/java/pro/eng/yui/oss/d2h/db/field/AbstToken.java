package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstToken extends AbstText {

    public AbstToken(String value){
        super(value);
    }

    @Override
    public String toString() {
        return "**token**" +"("+ value.length() +")" ;
    }
}
