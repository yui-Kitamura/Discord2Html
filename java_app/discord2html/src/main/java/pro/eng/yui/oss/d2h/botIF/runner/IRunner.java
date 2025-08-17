package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

/** marker for command runner */
public interface IRunner {
    
    /** 終了後replyするメッセージ */
    String afterRunMessage();
    
    /**
     * deferReply の引数に用いるフラグ。
     * true = ephemeral（他人に見えない）。false = 通常表示。
     * デフォルトは false。
     */
    default boolean shouldDeferEphemeral() {
        return false;
    }
    
    default OptionMapping get(List<OptionMapping> options, String name){
        for(OptionMapping op : options) {
            if (op.getName().equalsIgnoreCase(name)) {
                return op;
            }
        }
        return null;
    }
    
    default boolean isNull(Object obj){
        return obj == null;
    }
}
