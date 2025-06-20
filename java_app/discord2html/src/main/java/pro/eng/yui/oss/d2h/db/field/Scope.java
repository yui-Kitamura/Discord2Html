package pro.eng.yui.oss.d2h.db.field;

import java.util.Set;

public class Scope extends AbstText {
    
    public Scope(String value){
        super(value);
    }
    
    public Scope(Set<String> scopes){
        super(String.join(" ", scopes));
    }
}
