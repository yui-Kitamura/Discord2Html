package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessageInfo {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    static{
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }
    
    private final String contentRaw;
    public String getContentRaw() {
        return this.contentRaw;
    }
    
    private final Users userInfo;
    public Users getUserInfo(){
        return this.userInfo;
    }
    
    private final MessageUserInfo messageUserInfo;
    public MessageUserInfo getMessageUserInfo() {
        return this.messageUserInfo;
    }
    public String getAvatarUrl(){
        return this.messageUserInfo.getAvatarUrl();
    }
    public String getUsername() {
        return this.messageUserInfo.getUsername();
    }
    
    private final String createdTimestamp;
    public String getCreatedTimestamp(){
        return this.createdTimestamp;
    }
    
    private final List<Message.Attachment> attachments;
    public List<Message.Attachment> getAttachments(){
        return this.attachments;
    }

    private final List<MessageReaction> reactions;
    public List<MessageReaction> getReactions(){
        return this.reactions;
    }
    
    /** nullable */
    private final String refOriginMessageContent;
    public String getRefOriginMessageContent(){
        return this.refOriginMessageContent;
    }
    
    public MessageInfo(Message msg, Users authorInfo){
        this.createdTimestamp = DATE_FORMAT.format(Date.from(msg.getTimeCreated().toInstant()));
        this.userInfo = authorInfo;
        this.messageUserInfo = AnonymizationUtil.anonymizeUser(authorInfo);
        this.contentRaw = msg.getContentRaw();
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            String content = msg.getReferencedMessage().getContentRaw();
            this.refOriginMessageContent = content.length() > 30 ? content.substring(0, 30) : content;
        }
    }
}
