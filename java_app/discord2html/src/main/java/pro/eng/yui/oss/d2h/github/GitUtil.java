package pro.eng.yui.oss.d2h.github;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Gitコマンドを実行するためのユーティリティクラス。
 * リポジトリに対するfetch、add、commit、pushの基本的なGit操作をJavaから実行できる
 */
public class GitUtil {

    private GitUtil(){
        /* utility class */ 
    }
    
    /**
     * fetch
     */
    public static void fetch(File repoDir) throws Exception {
        runCommand(repoDir, "git", "fetch");
    }

    /**
     * リベースでリモート追従
     */
    public static void pullRebase(File repoDir, String remote, String branch) throws Exception {
        runCommand(repoDir, "git", "pull", "--rebase", remote, branch);
    }

    /**
     * ファイルをadd
     */
    public static void add(File repoDir, List<String> filePaths) throws Exception {
        for (String path : filePaths) {
            runCommand(repoDir, "git", "add", path);
        }
    }

    /**
     * コミット
     */
    public static void commit(File repoDir, String message) throws Exception {
        runCommand(repoDir, "git", "commit", "-m", message);
    }

    /**
     * push
     */
    public static void push(File repoDir, String remote, String branch) throws Exception {
        runCommand(repoDir, "git", "push", remote, branch);
    }

    /**
     * 汎用コマンド実行
     */
    private static void runCommand(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 出力をログに吐く、または集めておく
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("コマンド失敗: " + String.join(" ", command));
        }
    }


}
