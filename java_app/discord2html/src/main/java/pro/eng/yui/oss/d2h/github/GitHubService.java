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
            try {
                deleteDirectoryRecursively(repoDirFile.toPath());
            } catch (IOException e) {
                e.printStackTrace(); // continue
            }
        }
        if (!repoDirFile.exists() || !repoDirFile.isDirectory()) {
            repoDirFile.mkdirs();
        }
        // Initialize repository
        gitUtil.ensureRepoInitialized(gitConfig.getRepo().getUrl());

        String dateDir = GitHubConsts.DATE_FORMAT.format(new Date());
        File targetDir = new File(repoDirFile, GitHubConsts.ARCHIVES_DIR + dateDir);
        targetDir.mkdirs();

        // Copy all HTML files to the repository and collect paths for git add
        List<String> filesToAdd = new ArrayList<>();
        for (Path htmlFilePath : htmlFilePaths) {
            File targetFile = new File(targetDir, htmlFilePath.getFileName().toString());
            Files.copy(htmlFilePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            filesToAdd.add(getRelativePath(repoDirFile, targetFile));
        }

        try {
            gitUtil.fetch();
            gitUtil.pullRebase();

            gitUtil.add(filesToAdd);
            gitUtil.commit(GitHubConsts.COMMIT_PREFIX + "multiple files");
            gitUtil.push();
        } catch (Exception e) {
            throw new Exception("Failed to push HTML files to GitHub", e);
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

    /** フォルダ内容を含めた削除 */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    try {
                        file.toFile().setWritable(true);
                        Files.delete(file);
                    } catch (IOException ex) {
                        System.err.println("Failed to delete file: " + file + ": " + ex.getMessage());
                    }
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                System.err.println("Failed to access file: " + file + ": " + exc.getMessage());
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // Try to make the directory writable and try again
                    try {
                        dir.toFile().setWritable(true);
                        Files.delete(dir);
                    } catch (IOException ex) {
                        System.err.println("Failed to delete directory: " + dir + ": " + ex.getMessage());
                    }
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
}