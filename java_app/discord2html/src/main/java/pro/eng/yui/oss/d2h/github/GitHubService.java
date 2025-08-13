package pro.eng.yui.oss.d2h.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.GitHubConsts;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service to push HTML files to GitHub repository
 */
@Service
public class GitHubService {

    private final GitUtil gitUtil;
    private final GitConfig gitConfig;

    @Autowired
    public GitHubService(GitUtil gitUtil, GitConfig gitConfig) {
        this.gitUtil = gitUtil;
        this.gitConfig = gitConfig;
    }

    /**
     * Push HTML file to GitHub repository
     * 
     * @param htmlFilePath Path to the HTML file to push
     * @throws Exception If any error occurs during the push process
     */
    public void pushHtmlToGitHub(Path htmlFilePath) throws Exception {
        if (!Files.exists(htmlFilePath)) {
            throw new IOException("HTML file does not exist: " + htmlFilePath);
        }

        String repoDir = gitConfig.getLocal().getDir();
        File repoDirFile = new File(repoDir);
        if (!repoDirFile.exists() || !repoDirFile.isDirectory()) {
            repoDirFile.mkdirs();
        }
        // repoの確実な初期化
        gitUtil.ensureRepoInitialized(gitConfig.getRepo().getUrl());

        String dateDir = GitHubConsts.DATE_FORMAT.format(new Date());
        File targetDir = new File(repoDirFile, GitHubConsts.ARCHIVES_DIR + dateDir);
        targetDir.mkdirs();

        // Copy HTML file to the repository
        File targetFile = new File(targetDir, htmlFilePath.getFileName().toString());
        Files.copy(htmlFilePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Add, commit, and push the file
        List<String> filesToAdd = new ArrayList<>();
        filesToAdd.add(getRelativePath(repoDirFile, targetFile));

        try {
            gitUtil.fetch();
            gitUtil.pullRebase();

            // Add, commit, and push
            gitUtil.add(filesToAdd);
            gitUtil.commit(GitHubConsts.COMMIT_PREFIX + htmlFilePath.getFileName());
            gitUtil.push();
        } catch (Exception e) {
            throw new Exception("Failed to push HTML file to GitHub", e);
        }
    }

    /**
     * Get relative path from base directory to target file
     */
    private String getRelativePath(File baseDir, File targetFile) {
        String basePath = baseDir.getAbsolutePath();
        String targetPath = targetFile.getAbsolutePath();
        
        if (targetPath.startsWith(basePath)) {
            String relativePath = targetPath.substring(basePath.length());
            relativePath = relativePath.replace('\\', '/');
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        
        return targetFile.getName();
    }
}