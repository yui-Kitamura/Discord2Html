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
import java.util.stream.Stream;

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
     * Push multiple HTML files to GitHub repository in a single operation
     * 
     * @param htmlFilePaths List of paths to the HTML files to push
     * @throws Exception If any error occurs during the push process
     */
    public void pushHtmlFilesToGitHub(List<Path> htmlFilePaths) throws Exception {
        if (htmlFilePaths == null || htmlFilePaths.isEmpty()) {
            return; // Nothing to push
        }
        
        // Validate all files exist
        for (Path path : htmlFilePaths) {
            if (!Files.exists(path)) {
                throw new IOException("HTML file does not exist: " + path);
            }
        }

        String repoDir = gitConfig.getLocal().getDir();
        File repoDirFile = new File(repoDir);
        if (repoDirFile.exists()) {
            deleteDirectoryRecursively(repoDirFile.toPath());
        }
        // Initialize repository
        gitUtil.ensureRepoInitialized();

        String dateDir = GitHubConsts.DATE_FORMAT.format(new Date());
        File dateArchiveDir = new File(repoDirFile, GitHubConsts.ARCHIVES_DIR + dateDir);
        dateArchiveDir.mkdirs();

        // Copy all HTML files to the repository and collect paths for git add
        List<String> filesToAdd = new ArrayList<>();
        for (Path htmlFilePath : htmlFilePaths) {
            String fileName = htmlFilePath.getFileName().toString();
            File targetFile;
            if ("index.html".equalsIgnoreCase(fileName)) {
                // Place index.html at gh_pages root
                File ghPagesRoot = new File(repoDirFile, "gh_pages");
                ghPagesRoot.mkdirs();
                targetFile = new File(ghPagesRoot, "index.html");
            } else if (htmlFilePath.getParent() != null &&
                    htmlFilePath.getParent().getFileName() != null &&
                    "archives".equalsIgnoreCase(htmlFilePath.getParent().getFileName().toString())) {
                // Place archives/<channel>.html under gh_pages/archives/
                File archivesRoot = new File(repoDirFile, "gh_pages/archives");
                archivesRoot.mkdirs();
                targetFile = new File(archivesRoot, fileName);
            } else {
                // Default: place under daily archive directory
                targetFile = new File(dateArchiveDir, fileName);
            }
            Files.copy(htmlFilePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            filesToAdd.add(getRelativePath(repoDirFile, targetFile));
        }

        try {
            gitUtil.fetch();

            gitUtil.add(filesToAdd);
            String commitMessage;
            if(filesToAdd.size() == 2) { // archives/channel.html + archive/yMd/channel.html 
                commitMessage = new File(filesToAdd.get(0)).getName();
            }else {
                commitMessage = "multiple files";
            }
            gitUtil.commit(GitHubConsts.COMMIT_PREFIX + commitMessage);

            gitUtil.pullRebase();
            gitUtil.push();
        } catch (Exception e) {
            throw new Exception("Failed to push HTML files to GitHub", e);
        }
    }

    /**
     * 相対パスの取得
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

    /** フォルダ内容を含めた削除 */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteDirectoryRecursively(entry);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        path.toFile().setReadable(true);
        Files.delete(path);
    }
}