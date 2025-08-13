package pro.eng.yui.oss.d2h.github;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Gitコマンドを実行するためのユーティリティクラス。
 * リポジトリに対するfetch、add、commit、pushの基本的なGit操作をJavaから実行できる
 */
@Component //singleton
public class GitUtil {

    private final GitConfig config;
    private final Secrets secrets;
    
    @Autowired
    public GitUtil(GitConfig gitConfig, Secrets secrets){
        this.config = gitConfig;
        this.secrets = secrets;
    }
    /**
     * リポジトリ初期化を保証する
     * - .git が無ければ clone もしくは init + remote 設定 + fetch + checkout を行う
     * - ネットワーク操作時のみ一時的に認証付きURLを使い、完了後に元のURLへ戻す
     */
    public void ensureRepoInitialized(String originUrl) throws Exception {
        final File repoDir = new File(config.getLocal().getDir());
        if (!repoDir.exists()) {
            repoDir.mkdirs();
        }
        if (isGitRepoDir(repoDir)) {
            return; // 既にgit管理下
        }

        final String branch = config.getRepo().getMain();
        final String authUrl = insertTokenToUrl(originUrl);

        if (isEmptyDir(repoDir)) {
            // 空ディレクトリなら clone で作成
            runGitCommand("clone", authUrl, ".");
            // clone で remote に認証URLが残るため、非認証URLへ戻す
            runGitCommand("remote", "set-url", "origin", originUrl);
            // 目的ブランチへ切替（clone先のデフォルトブランチが異なる可能性に備える）
            try {
                runGitCommand("checkout", branch);
            } catch (Exception ignore) {
                // ブランチが無ければ追跡ブランチで作成
                runGitCommand("checkout", "-B", branch, "origin/" + branch);
            }
        } else {
            // 既存ファイルがある場合は in-place init
            runGitCommand("init");
            try {
                runGitCommand("remote", "add", "origin", originUrl);
            } catch (Exception e) {
                // 既に origin がある場合は上書き
                runGitCommand("remote", "set-url", "origin", originUrl);
            }
            // fetch は一時的に認証付きURLへ切替
            runGitCommand("remote", "set-url", "origin", authUrl);
            try {
                runGitCommand("fetch", "origin", branch);
            } finally {
                runGitCommand("remote", "set-url", "origin", originUrl);
            }
            // リモート追跡ブランチを起点にブランチを作成/更新
            runGitCommand("checkout", "-B", branch, "origin/" + branch);
        }
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
        final String branch = config.getRepo().getMain();
        runGitCommand("pull", "--rebase", "origin", branch);
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
        final String branch = config.getRepo().getMain();
        final String originUrl = getRemoteUrl();
        final String authUrl = insertTokenToUrl(originUrl);
        try {
            runGitCommand("remote", "set-url", "origin", authUrl);
            runGitCommand("push", "origin", branch);
        }finally {
            //必ず元に戻す
            runGitCommand("remote", "set-url", "origin", originUrl);
        }
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
        
        String maskId = config.getUser().getId();
        if(maskId == null){ maskId = ""; }
        String maskToken = secrets.getGitHubToken();
        if(maskToken == null){ maskToken = ""; }

        // 出力をログに吐く、または集めておく
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll(maskId, "**user**");
                line = line.replaceAll(maskToken, "**token**");
                System.out.println(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String outputCommand = String.join(" ", gitCommand);
            outputCommand = outputCommand.replaceAll(maskId, "**user**");
            outputCommand = outputCommand.replaceAll(maskToken, "**token**");
            throw new RuntimeException("コマンド失敗: " + outputCommand);
        }
    }

    /**
     * 現在のリモートURL取得
     */
    private String getRemoteUrl() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "remote", "get-url", "origin");
        pb.directory(new File(config.getLocal().getDir()));
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            return reader.readLine();
        }
    }

    /**
     * URL部分に認証情報を挿入
     */
    private String insertTokenToUrl(String url) {
        final String HTTPS_PROTOCOL = "https://";
        if (url.startsWith(HTTPS_PROTOCOL)) {
            return  HTTPS_PROTOCOL 
                    + config.getUser().getId() + ":" + secrets.getGitHubToken() + "@"
                    + url.substring(HTTPS_PROTOCOL.length());
        } else {
            throw new IllegalArgumentException("HTTPS URLのみ対応: " + url);
        }
    }

    private boolean isGitRepoDir(File dir) {
        return new File(dir, ".git").isDirectory();
    }

    private boolean isEmptyDir(File dir) {
        File[] files = dir.listFiles();
        return files == null || files.length == 0;
    }

}
