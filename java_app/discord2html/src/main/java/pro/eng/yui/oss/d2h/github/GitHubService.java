package pro.eng.yui.oss.d2h.github;

import org.jetbrains.annotations.NotNull;
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

    /** 
     * フォルダ内容を含めた削除 (.gitディレクトリの削除を確実に行う)
     * .gitディレクトリの場合はシステムコマンドを使用して確実に削除を試みる
     * @throws IOException ファイル操作に失敗した場合
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        // Special handling for .git directory - use system command if available
        if (path.toString().endsWith(".git") || new File(path.toFile(), ".git").exists()) {
            try {
                // Try using system command first for .git directories
                boolean deleted = false;
                if (isWindowsEnv()) {
                    // Windows - use rmdir /s /q
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "rmdir", "/s", "/q", path.toString());
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    deleted = (exitCode == 0);
                } else {
                    // Unix-like - use rm -rf
                    ProcessBuilder pb = new ProcessBuilder("rm", "-rf", path.toString());
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    deleted = (exitCode == 0);
                }
                
                if (deleted) {
                    return; // Successfully deleted using system command
                }
                // If system command failed, fall back to Java implementation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore the interrupted status
                System.err.println("Process was interrupted: " + e.getMessage());
                // Fall back to Java implementation
            } catch (IOException e) {
                System.err.println("IO error during system command: " + e.getMessage());
                // Fall back to Java implementation
            } catch (Exception e) {
                System.err.println("Failed to delete using system command: " + e.getMessage());
                // Fall back to Java implementation
            }
        }
        
        // Standard Java implementation for directory deletion
        Files.walkFileTree(path, new java.nio.file.SimpleFileVisitor<Path>() {
            @NotNull
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    try {
                        // Make file writable and not hidden
                        File f = file.toFile();
                        f.setWritable(true);
                        if (f.isHidden()) {
                            try {
                                // On Windows, try to remove hidden attribute
                                if (isWindowsEnv()) {
                                    Runtime.getRuntime().exec("attrib -H \"" + file + "\"");
                                }
                            } catch (Exception ex) {
                                // Ignore if attrib command fails
                            }
                        }
                        Files.delete(file);
                    } catch (IOException ex) {
                        System.err.println("Failed to delete file: " + file + ": " + ex.getMessage());
                    }
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @NotNull
            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                System.err.println("Failed to access file: " + file + ": " + exc.getMessage());
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            
            @NotNull
            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // Try to make the directory writable and try again
                    try {
                        File d = dir.toFile();
                        d.setWritable(true);
                        if (d.isHidden()) {
                            try {
                                if (isWindowsEnv()) {
                                    // On Windows, try to remove hidden attribute
                                    Runtime.getRuntime().exec("attrib -H \"" + dir + "\"");
                                }
                            } catch (Exception ex) {
                                // Ignore if attrib command fails
                            }
                        }
                        Files.delete(dir);
                    } catch (IOException ex) {
                        System.err.println("Failed to delete directory: " + dir + ": " + ex.getMessage());
                    }
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
    
    private boolean isWindowsEnv(){
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}