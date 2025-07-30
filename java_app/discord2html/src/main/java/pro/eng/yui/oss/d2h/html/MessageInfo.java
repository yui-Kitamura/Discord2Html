package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Users;

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
    
    public MessageInfo(Message msg, UsersDAO usersDao){
        this.createdTimestamp = DATE_FORMAT.format(Date.from(msg.getTimeCreated().toInstant()));
        GuildId guildId = new GuildId(msg.getGuild());
        UserId userId = new UserId(msg.getAuthor());
        this.userInfo =  usersDao.select(guildId, userId);
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
