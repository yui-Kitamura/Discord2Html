package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import org.springframework.beans.factory.annotation.Autowired;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.List;

public class MessageInfo {
    
    private final UsersDAO usersDao;
    
    private final String contentRaw;
    public String getContentRaw() {
        return this.contentRaw;
    }
    
    private final Users userInfo;
    public Users getUserInfo(){
        return this.userInfo;
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
        this.usersDao = usersDao;

        GuildId guildId = new GuildId(msg.getGuild());
        UserId userId = new UserId(msg.getAuthor());
        this.userInfo =  usersDao.select(guildId, userId);
        this.contentRaw = msg.getContentRaw();
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            this.refOriginMessageContent = msg.getReferencedMessage().getContentRaw().substring(0, 30);
        }
    }
}
