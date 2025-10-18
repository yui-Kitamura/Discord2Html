package pro.eng.yui.oss.d2h.consts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public final class DateTimeUtil {

    private DateTimeUtil() {}

    public static final Pattern DISCORD_TIME_PATTERN = Pattern.compile("<t:(\\d+):([A-Za-z])>");
    /** JST timezone constant (Asia/Tokyo). */
    public static final TimeZone JST = TimeZone.getTimeZone("Asia/Tokyo");
    public static final String endOfDay = "2359";

    private static ThreadLocal<SimpleDateFormat> tl(String pattern) {
        return ThreadLocal.withInitial(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            sdf.setTimeZone(JST);
            return sdf;
        });
    }

    // Common formatters (ThreadLocal to ensure thread-safety of SimpleDateFormat usage)
    private static final ThreadLocal<SimpleDateFormat> SDF_MILLIS = tl("yyyy/MM/dd HH:mm:ss.SSS");
    private static final ThreadLocal<SimpleDateFormat> SDF_FULL = tl("yyyy/MM/dd HH:mm:ss");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATE_ONLY = tl("yyyy/MM/dd");
    private static final ThreadLocal<SimpleDateFormat> SDF_FOLDER = tl("yyyyMMddHHmm");
    private static final ThreadLocal<SimpleDateFormat> SDF_DATE8 = tl("yyyyMMdd");
    private static final ThreadLocal<SimpleDateFormat> SDF_TIME = tl("HH:mm:ss");
    private static final ThreadLocal<SimpleDateFormat> SDF_ISO = tl("yyyy-MM-dd'T'HH:mm:ss");

    /** Returns a per-thread formatter for pattern HH:mm:ss.SSS */
    public static SimpleDateFormat mill() { return SDF_MILLIS.get(); }
    /** Returns a per-thread formatter for pattern yyyy/MM/dd HH:mm:ss */
    public static SimpleDateFormat full() { return SDF_FULL.get(); }
    /** Returns a per-thread formatter for pattern yyyy/MM/dd */
    public static SimpleDateFormat dateOnly() { return SDF_DATE_ONLY.get(); }
    /** Returns a per-thread formatter for pattern yyyyMMddHHmmss */
    public static SimpleDateFormat folder() { return SDF_FOLDER.get(); }
    /** Returns a per-thread formatter for pattern yyyyMMdd */
    public static SimpleDateFormat date8() { return SDF_DATE8.get(); }
    /** Returns a per-thread formatter for pattern HH:mm:ss */
    public static SimpleDateFormat time() { return SDF_TIME.get(); }
    /** Returns a per-thread ISO8601 formatter for pattern yyyy-MM-ddTHH:mm:ss */
    public static SimpleDateFormat iso() { return SDF_ISO.get(); }

    /** Validate string is exactly yyyyMMdd and represents a real calendar date (JST). */
    public static boolean isValidDate8(String s) {
        if (s == null || !s.matches("\\d{8}")) {
            return false;
        }
        try {
            date8().parse(s);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Parse yyyyMMdd to a JST Calendar set to 00:00:00.000 of that day.
     * Throws IllegalArgumentException if invalid.
     */
    public static Calendar toJstCalendarFromDate8(String date8Str) {
        if (!isValidDate8(date8Str)) {
            throw new IllegalArgumentException("Invalid yyyyMMdd: " + date8Str);
        }
        try {
            Date d = date8().parse(date8Str);
            Calendar cal = Calendar.getInstance(JST);
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid yyyyMMdd: " + date8Str, e);
        }
    }

    /** Format date to yyyyMMdd in JST. */
    public static String formatDate8(Date d) { return date8().format(d); }
    /** Format calendar to yyyyMMdd in JST. */
    public static String formatDate8(Calendar cal) { return date8().format(cal.getTime()); }
    /** Get current JST date as yyyyMMdd. */
    public static String nowDate8() { return date8().format(Calendar.getInstance(JST).getTime()); }
    
    public static Calendar getFromUnix(long unixValue){
        Calendar cal = Calendar.getInstance(JST);
        cal.setTimeInMillis(unixValue * 1000);
        return cal;
    }

    public static Calendar getFromUnix(String unixValue) {
        try {
            return getFromUnix(Long.parseLong(unixValue));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid unix timestamp: " + unixValue, e);
        }
    }

}
