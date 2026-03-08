package pro.eng.yui.oss.d2h.db.field;

import java.util.Locale;

public class Lang extends AbstVarChar {

    public static int LIMIT = 10;
    
    public static final Lang DEFAULT = new Lang(Locale.JAPANESE);
    
    public Lang(String value) {
        super(value, LIMIT);
    }

    public Lang(Locale locale) {
        this(locale.toLanguageTag());
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(this.value);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
