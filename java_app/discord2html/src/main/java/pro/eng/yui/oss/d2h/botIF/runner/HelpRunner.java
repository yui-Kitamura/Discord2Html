package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.github.GitConfig;
import pro.eng.yui.oss.d2h.github.GitUtil;

import java.util.List;

@Component
public class HelpRunner implements IRunner {
    
    private final GitConfig gitConfig;
    private final Secrets secrets;
    private final GitUtil gitUtil;
    
    private String lastAfterRunMessage = "bot sent you help guid to DM";
    private boolean lastShouldDeferEphemeral = true;
    
    public HelpRunner(GitConfig gitConfig, Secrets secrets, GitUtil gitUtil){
        this.gitConfig = gitConfig;
        this.secrets = secrets;
        this.gitUtil = gitUtil;
    }
    
    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.ANY;
    }

    /** Overload for /d2h help with options: version or tos link */
    public void run(@NotNull Member member, boolean isAdmin, boolean showVersion, boolean showTos){
        String returnMessage = "";

        if (showVersion) {
            final String ver = secrets.getBotVersion();
            returnMessage +=
                    "bot version: " + ver + "\n" +
                    "GitHub: " + gitConfig.getRepo().getUrl();
            this.lastShouldDeferEphemeral = true;
        }
        if (showTos) {
            final String url = gitUtil.getPagesUrlSafe() + "/tos.html";
            if(returnMessage.isEmpty() == false){ returnMessage += "\n"; }   
            returnMessage += "アーカイブ運用ポリシー(TOS): " + url;
            this.lastShouldDeferEphemeral = true;
        }
        
        if (showVersion == false && showTos == false) {
            member.getUser().openPrivateChannel().queue(
                    ch -> ch.sendMessage(buildHelpText()).queue(),
                    err -> { /* nothing to do */ }
            );
            this.lastAfterRunMessage = "bot sent you help guid to DM";
            this.lastShouldDeferEphemeral = true;
        }
        
        this.lastAfterRunMessage = returnMessage;
    }

    private String buildHelpText(){
        StringBuilder sb = new StringBuilder();
        sb.append("Discord2Html ヘルプ (/d2h コマンド)\n");
        sb.append("\n");
        sb.append("[注意]\n");
        sb.append("- コマンドはサーバーチャンネルでのみ使用できます（DMでは不可）。\n");
        sb.append("- ボット設定で 管理タグ付きチャンネル のみ実行可能です。\n");
        sb.append("- ほとんどのコマンドは 管理者権限(D2H Admin) が必要です。\n");
        sb.append("\n");
        sb.append("/d2h コマンド一覧\n");
        sb.append("- /d2h archive\n");
        sb.append("  権限: だれでも\n");
        sb.append("  アーカイブ対象のチャンネル一覧を表示します。\n");
        sb.append("- /d2h archive channel:<チャンネル> mode:(ignore|monitor)\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  対象チャンネルのアーカイブ対象設定を変更します。\n");
        sb.append("\n");
        sb.append("- /d2h run [target:<チャンネル>]\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  今すぐアーカイブ処理を実行します。target を指定するとそのチャンネル、未指定時はサーバーの全対象チャンネルに対して動作します。\n");
        sb.append("\n");
        sb.append("- /d2h role role:<ロール> anonymous:(anonymous|open)\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  指定ロール所持ユーザの匿名設定を一括で変更します。\n");
        sb.append("\n");
        sb.append("- /d2h schedule cycle:<0-23>\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  サーバーの自動アーカイブ実行周期（時間）を設定します。起算時刻は 0:00 です。例: cycle=6 の場合 0, 6, 12, 18 時に実行します。\n0を指定すると深夜0時の1回だけ実行します。");
        sb.append("\n");
        sb.append("- /d2h anonymous menu:cycle cycle:<数値>\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  サーバーの匿名名前サイクル時間を変更します。推奨レンジ: 1〜24（時間）。\n");
        sb.append("\n");
        sb.append("- /d2h me anonymous:(anonymous|open)\n");
        sb.append("  権限: だれでも\n");
        sb.append("  自分の匿名設定を変更します。\n");
        sb.append("\n");
        sb.append("- /d2h optout opt-in:(True|False) [channel:<チャンネル>] \n");
        sb.append("  権限: だれでも\n");
        sb.append("  個人のアーカイブ同意設定を変更します。\n");
        sb.append("  - opt-in=True: 再同意(オプトイン)として記録します。False: オプトアウトを記録します。\n");
        sb.append("  - channel を省略するとサーバー全体(ギルド)に対する設定、指定するとそのチャンネルにのみ適用されます。\n");
        sb.append("\n");
        sb.append("- /d2h help\n");
        sb.append("  権限: だれでも\n");
        sb.append("  このヘルプを表示します。\n");
        sb.append("\n");
        sb.append("[実行場所の制限]\n");
        sb.append("すべてのコマンドは「管理タグ付きチャンネル」でのみ有効です。その他のチャンネルで実行すると拒否されます。\n");
        sb.append("\n");
        sb.append("【「管理タグ付きチャンネル」の正確な仕様】\n");
        sb.append("- サーバーロール「D2H-admin」がそのチャンネルに対して「メッセージ履歴の閲覧（Permission.MESSAGE_HISTORY）」権限を持っている場合、そのチャンネルは「管理タグ付き」と判定されます。\n");
        sb.append("- ボット参加時に「D2H-admin」ロールが自動作成され、全チャンネルで同権限が「拒否」に初期化されます。\n");
        sb.append("- 運用者が「管理タグ」を付けたいチャンネルでは、該当チャンネルの権限設定で「D2H-admin」ロールに「メッセージ履歴」権限を「許可」に変更してください。\n");
        sb.append("- 上記が「許可」になっているチャンネルのみがコマンド実行可能な「管理タグ付きチャンネル」として扱われます（カテゴリーは対象外）。\n");
        sb.append("\n");
        sb.append("[GitHub]\n");
        sb.append("リポジトリ: ").append(gitConfig.getRepo().getUrl()).append("\n");
        sb.append("\n");
        sb.append("[TOS]\n");
        sb.append("アーカイブ運用ポリシー: ").append(gitUtil.getPagesUrlSafe()).append("/tos.html\n");

        return sb.toString();
    }

    @Override
    public String afterRunMessage() {
        return lastAfterRunMessage;
    }
    
    @Override
    public boolean shouldDeferEphemeral() {
        return lastShouldDeferEphemeral;
    }
}
