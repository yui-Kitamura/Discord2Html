package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
        this.contentRaw = extractContentIncludingEmbeds(msg);
        this.attachments = msg.getAttachments();
        this.reactions = msg.getReactions();
        if(msg.getReferencedMessage() == null) {
            this.refOriginMessageContent = null;
        }else{
            String content = extractContentIncludingEmbeds(msg.getReferencedMessage());
            this.refOriginMessageContent = content.length() > 30 ? content.substring(0, 30) : content;
        }
    }

    private static String extractContentIncludingEmbeds(Message msg) {
        String content = msg.getContentRaw();
        if (content == null || content.trim().isEmpty()) {
            List<MessageEmbed> embeds = msg.getEmbeds();
            if (embeds != null && !embeds.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (MessageEmbed e : embeds) {
                    if (e.getTitle() != null && !e.getTitle().isEmpty()) {
                        sb.append(e.getTitle()).append("\n");
                    }
                    if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                        sb.append(e.getDescription()).append("\n");
                    }
                    List<MessageEmbed.Field> fields = e.getFields();
                    if (fields != null) {
                        for (MessageEmbed.Field f : fields) {
                            if (f == null) continue;
                            boolean hasName = f.getName() != null && !f.getName().isEmpty();
                            boolean hasValue = f.getValue() != null && !f.getValue().isEmpty();
                            if (hasName || hasValue) {
                                if (hasName) sb.append(f.getName());
                                if (hasName && hasValue) sb.append(": ");
                                if (hasValue) sb.append(f.getValue());
                                sb.append("\n");
                            }
                        }
                    }
                    if (e.getFooter() != null && e.getFooter().getText() != null && !e.getFooter().getText().isEmpty()) {
                        sb.append(e.getFooter().getText()).append("\n");
                    }
                    if (e.getAuthor() != null && e.getAuthor().getName() != null && !e.getAuthor().getName().isEmpty()) {
                        sb.append(e.getAuthor().getName()).append("\n");
                    }
                    if (e.getUrl() != null && !e.getUrl().isEmpty()) {
                        sb.append(e.getUrl()).append("\n");
                    }
                }
                content = sb.toString().trim();
            }
        }
        if (content == null) content = "";
        return content;
    }
}
