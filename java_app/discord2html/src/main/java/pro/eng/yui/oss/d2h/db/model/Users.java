package pro.eng.yui.oss.d2h.db.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.db.dao.AnonStatsDAO;
import pro.eng.yui.oss.d2h.db.dao.OptoutDAO;
import pro.eng.yui.oss.d2h.db.field.*;

import java.util.Objects;

public class Users {

    private UserId user_id;
    public void setUserId(UserId newValue){
        this.user_id = newValue;
    }
    public UserId getUserId(){
        return user_id;
    }
    
    private GuildId guild_id;
    public void setGuildId(GuildId newValue){
        this.guild_id = newValue;
    }
    public GuildId getGuildId(){
        return guild_id;
    }
    
    private UserName user_name;
    public void setUserName(UserName newValue){
        this.user_name = newValue;
    }
    public UserName getUserName() {
        return user_name;
    }

    private Nickname nickname;
    public void setNickname(Nickname newValue){
        this.nickname = newValue;
    }
    public Nickname getNickname() {
        return nickname;
    }

    private Avatar avatar;
    public void setAvatar(Avatar newValue){
        this.avatar = newValue;
    }
    public Avatar getAvatar(){
        return avatar;
    }
    
    private AnonStats anon_stats;
    public void setAnonStats(AnonStats ignoreAnon){
        this.anon_stats = ignoreAnon;
    }
    public AnonStats getAnonStats(){
        return anon_stats;
    }

    private boolean optedOut;
    public boolean isOptedOut() { 
        return optedOut; 
    }
    public void setOptedOut(boolean v) { 
        this.optedOut = v; 
    }
    
    public Users(){
        // nothing to do
    }
    /** anon_stats以外を設定する */
    public Users(Member member){
        this.guild_id = new GuildId(member.getGuild());
        this.user_id = new UserId(member.getUser());
        this.user_name = new UserName(member.getUser());
        this.nickname = new Nickname(member);
        this.user_name = new UserName(member.getUser());
        this.avatar = new Avatar(member.getUser());
    }

    /**
     * Construct from a raw User (e.g., webhook/bot) and Guild context.
     * Uses the User's global name as both username and nickname.
     */
    public Users(User user, Guild guild) {
        this.guild_id = new GuildId(guild);
        this.user_id = new UserId(user);
        this.user_name = new UserName(user);
        String base;
        try {
            String gn = user.getGlobalName();
            base = (gn != null && !gn.isBlank()) ? gn : user.getName();
        } catch (Throwable ignore) {
            base = user.getName();
        }
        this.nickname = new Nickname(base);
        this.avatar = new Avatar(user);
    }
    
    public static Users get(Message msg, AnonStatsDAO anonStatsDao, OptoutDAO optoutDao){
        Users author;
        if (msg.getMember() == null) {
            // bot or non-member
            author = new Users(msg.getAuthor(), msg.getGuild());
            UserAnon anonStatus = msg.getAuthor().isBot() ? UserAnon.OPEN : UserAnon.ANONYMOUS;
            author.setAnonStats(new AnonStats(anonStatus));
        } else {
            // member
            author = new Users(msg.getMember());
            UserAnon anonStatus = anonStatsDao.extractAnonStats(msg.getMember());
            author.setAnonStats(new AnonStats(anonStatus));
        }
        try {
            GuildId gid = new GuildId(msg.getGuild());
            UserId uid = new UserId(msg.getAuthor());
            ChannelId chId = new ChannelId(msg.getChannel().getIdLong());
            boolean optedOut = optoutDao.isOptedOut(uid, gid, chId);
            author.setOptedOut(optedOut);
            if (optedOut) {
                // Force anonymous display for opted-out users regardless of their original anon preference
                author.setAnonStats(new AnonStats(UserAnon.ANONYMOUS));
            }
        } catch (Throwable ignore) { /* ignore DB or API errors */ }
        return author;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (user_id == null ? 0 : user_id.hashCode());
        hash = hash * 31 + (guild_id == null ? 0 : guild_id.hashCode());
        hash = hash * 31 + (user_name == null ? 0 : user_name.hashCode());
        hash = hash * 31 + (nickname == null ? 0 : nickname.hashCode());
        hash = hash * 31 + (avatar == null ? 0 : avatar.hashCode());
        hash = hash * 31 + (anon_stats == null ? 0 : anon_stats.hashCode());
        hash = hash * 31 + (optedOut ? 1 : 0);
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        Users other = (Users) obj;
        if(!Objects.equals(user_id, other.user_id)){
            return false;
        }
        if(!Objects.equals(guild_id, other.guild_id)) {
            return false;
        }
        if(!Objects.equals(user_name, other.user_name)){
            return false;
        }
        if(!Objects.equals(nickname, other.nickname)){
            return false;
        }
        if(!Objects.equals(avatar, other.avatar)){
            return false;
        }
        if(!Objects.equals(anon_stats, other.anon_stats)){
            return false;
        }
        if (this.optedOut != other.optedOut) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
