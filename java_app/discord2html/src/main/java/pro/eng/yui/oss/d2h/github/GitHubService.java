package pro.eng.yui.oss.d2h.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.GitHubConsts;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Paths;
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
    private final ApplicationConfig appConfig;

    @Autowired
    public GitHubService(GitUtil gitUtil, GitConfig gitConfig, ApplicationConfig appConfig) {
        this.gitUtil = gitUtil;
        this.gitConfig = gitConfig;
        this.appConfig = appConfig;
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
        File dateArchiveDir = repoDirFile.toPath().resolve(GitHubConsts.ARCHIVES_DIR).resolve(dateDir).toFile();
        dateArchiveDir.mkdirs();

        // Copy all HTML files to the repository and collect paths for git add
        List<String> filesToAdd = new ArrayList<>();

        // Ensure static assets exist under gh_pages and include them in the staging list
        ensureStaticAssetsInRepo(repoDirFile, filesToAdd);
        // Also ensure custom emoji resources are staged
        ensureEmojisInRepo(repoDirFile, filesToAdd);
        for (Path htmlFilePath : htmlFilePaths) {
            String fileName = htmlFilePath.getFileName().toString();
            File targetFile;
            // Normalize path for checks
            String normalized = htmlFilePath.toString().replace('\\', '/');
            boolean isTopLevelIndexOrHelp = ("index.html".equalsIgnoreCase(fileName) || "help.html".equalsIgnoreCase(fileName))
                    && !normalized.contains("/archives/");
            if (isTopLevelIndexOrHelp) {
                // Place top-level index.html and help.html at gh_pages root
                File ghPagesRoot = new File(repoDirFile, "gh_pages");
                ghPagesRoot.mkdirs();
                targetFile = new File(ghPagesRoot, fileName);
            } else {
                // If the path is under any 'archives' sub-tree, mirror it under gh_pages
                int idx = normalized.indexOf("/archives/");
                if (idx >= 0) {
                    String subPath = normalized.substring(idx + 1); // keep starting at 'archives/...'
                    File ghPagesRoot = new File(repoDirFile, "gh_pages");
                    targetFile = new File(ghPagesRoot, subPath);
                    if (targetFile.getParentFile() != null) {
                        targetFile.getParentFile().mkdirs();
                    }
                } else {
                    continue;
                }
            }
            Files.copy(htmlFilePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            filesToAdd.add(getRelativePath(repoDirFile, targetFile));
        }

        try {
            gitUtil.fetch();

            gitUtil.add(filesToAdd);
            gitUtil.commit(GitHubConsts.COMMIT_PREFIX + "multiple files");

            gitUtil.pullRebaseGhPage();
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

    /**
     * Ensure static assets (css/style.css, D2H_logo.png) exist under gh_pages in the repo
     * and add their relative paths to filesToAdd for staging.
     */
    private void ensureStaticAssetsInRepo(File repoDirFile, List<String> filesToAdd) throws IOException {
        File ghPagesRoot = new File(repoDirFile, "gh_pages");
        File cssDir = new File(ghPagesRoot, "css");
        cssDir.mkdirs();

        // Write style.css from classpath to gh_pages/css/style.css
        byte[] css = readClasspathResource("/static/css/style.css");
        if (css != null) {
            File styleCss = new File(cssDir, "style.css");
            java.nio.file.Files.write(styleCss.toPath(), css);
            filesToAdd.add(getRelativePath(repoDirFile, styleCss));
        }

        // Ensure js directory and copy static JS files used by templates
        File jsDir = new File(ghPagesRoot, "js");
        jsDir.mkdirs();
        // Currently used: /Discord2Html/js/archive-date.js referenced by templates/list.html
        byte[] archiveDateJs = readClasspathResource("/static/js/archive-date.js");
        if (archiveDateJs != null) {
            File jsFile = new File(jsDir, "archive-date.js");
            java.nio.file.Files.write(jsFile.toPath(), archiveDateJs);
            filesToAdd.add(getRelativePath(repoDirFile, jsFile));
        }

        // Write logo to gh_pages/D2H_logo.png
        byte[] logo = readClasspathResource("/D2H_logo.png");
        if (logo != null) {
            File logoFile = new File(ghPagesRoot, "D2H_logo.png");
            java.nio.file.Files.write(logoFile.toPath(), logo);
            filesToAdd.add(getRelativePath(repoDirFile, logoFile));
        }
    }

    /**
     * Copy custom emoji files from outputPath/archives/emoji into gh_pages/archives/emoji in the repo
     * and add them to filesToAdd for staging. Best-effort: silently return if none exist.
     */
    private void ensureEmojisInRepo(File repoDirFile, List<String> filesToAdd) throws IOException {
        if (appConfig == null) return;
        Path sourceEmojiDir = appConfig.getOutputPath().resolve("archives").resolve("emoji");
        if (!Files.exists(sourceEmojiDir) || !Files.isDirectory(sourceEmojiDir)) {
            return;
        }
        File ghPagesRoot = new File(repoDirFile, "gh_pages");
        File targetEmojiDir = new File(new File(ghPagesRoot, "archives"), "emoji");
        targetEmojiDir.mkdirs();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceEmojiDir)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;
                File targetFile = new File(targetEmojiDir, p.getFileName().toString());
                Files.copy(p, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                filesToAdd.add(getRelativePath(repoDirFile, targetFile));
            }
        }
    }

    private byte[] readClasspathResource(String resourcePath) throws IOException {
        try (var in = GitHubService.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }
}