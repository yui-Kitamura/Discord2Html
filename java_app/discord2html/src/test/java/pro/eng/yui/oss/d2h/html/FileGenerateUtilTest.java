package pro.eng.yui.oss.d2h.html;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FileGenerateUtilTest {

    private String testableConvertUnixTime(String input) {
        try {
            Method method = FileGenerateUtil.class.getDeclaredMethod("convertUnixTime", String.class);
            method.setAccessible(true);
            return (String) method.invoke(FileGenerateUtil.class, input);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Test
    void convertsSpecifiedPatternsCorrectly() {
        String unix = "1767193198"; // 2025/12/31 23:59:58 JST
        // date-only
        assertEquals("2025/12/31", testableConvertUnixTime("<t:" + unix + ":d>"));
        assertEquals("2025/12/31", testableConvertUnixTime("<t:" + unix + ":D>"));
        // time
        assertEquals("23:59:58", testableConvertUnixTime("<t:" + unix + ":t>"));
        assertEquals("23:59:58", testableConvertUnixTime("<t:" + unix + ":T>"));
        // full
        assertEquals("2025/12/31 23:59:58", testableConvertUnixTime("<t:" + unix + ":f>"));
        assertEquals("2025/12/31 23:59:58", testableConvertUnixTime("<t:" + unix + ":F>"));
        assertEquals("2025/12/31 23:59:58", testableConvertUnixTime("<t:" + unix + ":R>"));
    }

    @Test
    void fallsBackOnUnknownPatternWithoutException() {
        String unix = "1767193198";
        Exception exception = assertThrows(RuntimeException.class,
                () -> testableConvertUnixTime("<t:" + unix + ":x>"));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }
}