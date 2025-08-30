package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstName extends AbstVarChar {
    public static String EMPTY_NAME = "";
    public static String SUFFIX_DELETED = "(削除済み)";

    public AbstName(String value, int limit) {
        super(value, limit);
    }
}
