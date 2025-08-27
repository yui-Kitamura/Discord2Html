package pro.eng.yui.oss.d2h.db.model;

import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.ChannelId;
import pro.eng.yui.oss.d2h.db.field.ChannelName;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.CategoryId;
import pro.eng.yui.oss.d2h.db.field.CategoryName;

import java.util.Objects;

public class Channels {
    
    /** DiscordチャンネルID */
    private ChannelId channel_id;
    public void setChannelId(ChannelId newValue){
        this.channel_id = newValue;
    }
    public ChannelId getChannelId(){
        return channel_id;
    }
    
    /** DiscordサーバID */
    private GuildId guild_id;
    public void setGuildId(GuildId newValue){
        this.guild_id = newValue;
    }
    public GuildId getGuidId(){
        return guild_id;
    }
    
    /** Discordチャンネル名 */
    private ChannelName channel_name;
    public void setChannelName(ChannelName newValue){
        this.channel_name = newValue;
    }
    public ChannelName getChannelName() {
        return channel_name;
    }

    /** DiscordカテゴリID（削除時の追跡用） */
    private CategoryId category_id;
    public void setCategoryId(CategoryId newValue){ this.category_id = newValue; }
    public CategoryId getCategoryId(){ return category_id; }

    /** Discordカテゴリ名（削除時の追跡用） */
    private CategoryName category_name;
    public void setCategoryName(CategoryName newValue){ this.category_name = newValue; }
    public CategoryName getCategoryName(){ return category_name; }

    public Channels(){
        // nothing to do
    }
    public Channels(GuildChannel ch){
        this.guild_id = new GuildId(ch.getGuild());
        this.channel_id = new ChannelId(ch);
        this.channel_name = new ChannelName(ch);
        if (ch instanceof ICategorizableChannel cc && cc.getParentCategory() != null) {
            this.category_id = new CategoryId(cc.getParentCategory());
            this.category_name = new CategoryName(cc.getParentCategory());
        } else {
            this.category_id = new CategoryId(0L);
            this.category_name = new CategoryName("");
        }
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (channel_id == null ? 0 : channel_id.hashCode());
        hash = hash * 31 + (guild_id == null ? 0 : guild_id.hashCode());
        hash = hash * 31 + (channel_name == null ? 0 : channel_name.hashCode());
        hash = hash * 31 + (category_id == null ? 0 : category_id.hashCode());
        hash = hash * 31 + (category_name == null ? 0 : category_name.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        Channels other = (Channels) obj;
        if(!Objects.equals(channel_id, other.channel_id)){
            return false;
        }
        if(!Objects.equals(guild_id, other.guild_id)) {
            return false;
        }
        if(!Objects.equals(channel_name, other.channel_name)){
            return false;
        }
        if(!Objects.equals(category_id, other.category_id)){
            return false;
        }
        if(!Objects.equals(category_name, other.category_name)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
