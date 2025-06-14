package pro.eng.yui.oss.d2h.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Gitコマンドを実行するためのユーティリティクラス。
 * リポジトリに対するfetch、add、commit、pushの基本的なGit操作をJavaから実行できる
 */
@Component //singleton
public class GitUtil {

    private GitConfig config;
    
    @Autowired
    public GitUtil(GitConfig gitConfig){
        config = gitConfig;
    }
    
    /**
     * fetch
     */
    public void fetch() throws Exception {
        runGitCommand("fetch");
    }

    /**
     * リベースでリモート追従
     */
    public void pullRebase() throws Exception {
        final String remote = config.getRepo().getOwner()+"/"+config.getRepo().getName();
        final String branch = config.getRepo().getMain();
        runGitCommand("pull", "--rebase", remote, branch);
    }

    /**
     * ファイルをadd
     */
    public void add(List<String> filePaths) throws Exception {
        for (String path : filePaths) {
            runGitCommand("add", path);
        }
    }

    /**
     * コミット
     */
    public void commit(String message) throws Exception {
        runGitCommand("commit", "-m", message);
    }

    /**
     * push
     */
    public void push() throws Exception {
        final String remote = config.getRepo().getOwner()+"/"+config.getRepo().getName();
        final String branch = config.getRepo().getMain();
        runGitCommand("push", remote, branch);
    }

    /**
     * 汎用gitコマンド実行
     */
    private void runGitCommand(String... command) throws Exception {
        String[] gitCommand = new String[command.length + 1];
        gitCommand[0] = "git";
        System.arraycopy(command, 0, gitCommand, 1, command.length);
        ProcessBuilder pb = new ProcessBuilder(gitCommand);
        pb.directory(new File(config.getLocal().getDir()));
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
