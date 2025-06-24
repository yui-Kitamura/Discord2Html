package pro.eng.yui.oss.d2h.db.field;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Scope extends AbstText {
    
    protected static String DELIMITER = " ";
    
    public Scope(String value){
        super(value);
    }
    
    public Scope(Set<String> scopes){
        super(String.join(DELIMITER, scopes));
    }

    public Set<String> getScopeSet(){
        return new HashSet<>(Arrays.asList(getValue().split(DELIMITER)));
    }
}
