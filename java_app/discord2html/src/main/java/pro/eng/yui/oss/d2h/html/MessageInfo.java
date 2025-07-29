package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;

public class MessageInfo {
    
    public MessageInfo(Message msg){
        msg.getContentRaw();
        msg.getMember();
        msg.getContentRaw();
        msg.getAttachments();
        msg.getReactions();
    }
}
