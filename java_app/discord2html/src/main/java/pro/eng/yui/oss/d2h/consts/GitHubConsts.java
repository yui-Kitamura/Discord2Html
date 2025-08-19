package pro.eng.yui.oss.d2h.consts;

import java.text.SimpleDateFormat;

/**
 * Constants for GitHub operations
 */
public class GitHubConsts {
    
    /**
     * Directory in the repository where archives are stored
     */
    public static final String ARCHIVES_DIR = "gh_pages/archives/";
    
    /**
     * Date format for archive directory structure
     */
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * Commit message prefix for archive files
     */
    public static final String COMMIT_PREFIX = "Add archive: ";
}