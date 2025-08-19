package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Component;

/**
 * Utility to resolve the running bot version.
 * It prefers the Implementation-Version from MANIFEST.MF (set by build packaging),
 * and falls back to "dev" when running from IDE/tests.
 */
@Component
public final class VersionUtil {
    private static final String VERSION;
    static {
        String v = null;
        try {
            Package pkg = VersionUtil.class.getPackage();
            if (pkg != null) {
                v = pkg.getImplementationVersion();
            }
        } catch (Exception ignore) {
            // ignore
        }
        if (v == null || v.isBlank()) {
            v = "dev";
        }
        VERSION = v;
    }

    private VersionUtil() {}

    public static String getVersion() {
        return VERSION;
    }
}
