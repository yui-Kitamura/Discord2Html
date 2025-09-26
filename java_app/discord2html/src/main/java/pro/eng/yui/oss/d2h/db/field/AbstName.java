package pro.eng.yui.oss.d2h.db.field;

public abstract class AbstName extends AbstVarChar {
    public static String EMPTY_NAME = "";
    public static String SUFFIX_DELETED = "(削除済み)";
    public static String UNKNOWN = "(不明)";
    public static String ANON = "(非公開希望)";

    public AbstName(String value, int limit) {
        super(value, limit);
    }
}
