package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
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

    public Users(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (user_id == null ? 0 : user_id.hashCode());
        hash = hash * 31 + (user_name == null ? 0 : user_name.hashCode());
        hash = hash * 31 + (nickname == null ? 0 : nickname.hashCode());
        hash = hash * 31 + (avatar == null ? 0 : avatar.hashCode());
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
        if(!Objects.equals(user_name, other.user_name)){
            return false;
        }
        if(!Objects.equals(nickname, other.nickname)){
            return false;
        }
        if(!Objects.equals(avatar, other.avatar)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
