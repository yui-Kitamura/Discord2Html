package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;

import java.awt.Color;
import java.util.List;

/** marker for command runner */
public interface IRunner {
    
    enum RequiredPermissionType {
        DENY,
        ANY,
        D2H_ADMIN,
        SERVER_ADMIN
    }
    
    public final Color INFO = Color.BLUE;
    public final Color SUCCESS = Color.GREEN;
    public final Color WARN = Color.ORANGE;
    public final Color ERROR = Color.RED;

    /** 終了後replyするメッセージ */
    MessageSeed afterRunMessage();

    /**
     * deferReply の引数に用いるフラグ。
     * true = ephemeral（他人に見えない）。false = 通常表示。
     * デフォルトは false。
     */
    default boolean shouldDeferEphemeral() {
        return false;
    }
    
    /**
     * オプションリストから指定した名前のオプションを取得する。
     * 大文字小文字を区別しない。
     * 
     * @param options オプションリスト
     * @param name オプション名
     * @return 見つかった場合はその OptionMapping、見つからない場合は null
     */
    default OptionMapping get(List<OptionMapping> options, String name){
        for(OptionMapping op : options) {
            if (op.getName().equalsIgnoreCase(name)) {
                return op;
            }
        }
        return null;
    }
    
    RequiredPermissionType requiredPermissionType(List<OptionMapping> options);
    
}
