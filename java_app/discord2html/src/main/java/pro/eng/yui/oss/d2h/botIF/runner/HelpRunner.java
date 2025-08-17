package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class HelpRunner implements IRunner {
    
    public HelpRunner(){
        // nothing to do
    }
    
    public void run(@NotNull Member member, boolean isAdmin){
        final User user = member.getUser();
        final String helpText = buildHelpText();
        user.openPrivateChannel().queue(
                ch -> ch.sendMessage(helpText).queue(),
                err -> { /* nothing to do */ }
        );
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
        sb.append("- /d2h anonymous menu:cycle cycle:<数値>\n");
        sb.append("  権限: 管理者のみ\n");
        sb.append("  サーバーの匿名名前サイクル時間を変更します。推奨レンジ: 1〜24（時間）。\n");
        sb.append("\n");
        sb.append("- /d2h me anonymous:(anonymous|open)\n");
        sb.append("  権限: だれでも\n");
        sb.append("  自分の匿名設定を変更します。\n");
        sb.append("\n");
        sb.append("- /d2h help\n");
        sb.append("  権限: だれでも\n");
        sb.append("  このヘルプを表示します。\n");
        sb.append("\n");
        sb.append("[実行場所の制限]\n");
        sb.append("すべてのコマンドは「管理タグ付きチャンネル」でのみ有効です。その他のチャンネルで実行すると拒否されます。\n");
        return sb.toString();
    }

    @Override
    public String afterRunMessage() {
        return "bot sent you help guid to DM";
    }
    
    @Override
    public boolean shouldDeferEphemeral() {
        return true;
    }
}
