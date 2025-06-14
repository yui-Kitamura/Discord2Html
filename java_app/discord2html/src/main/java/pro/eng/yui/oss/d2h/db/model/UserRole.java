package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.*;

import java.util.Objects;

public class UserRole {
    
    private UserId user_id;
    public void setUserId(UserId newValue){
        this.user_id = newValue;
    }
    public UserId getUser_id(){
        return user_id;
    }
    
    private RoleId role_id;
    public void setRoleId(RoleId newValue){
        this.role_id = newValue;
    }
    public RoleId getRoleId(){
        return role_id;
    }

    public UserRole(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (user_id == null ? 0 : user_id.hashCode());
        hash = hash * 31 + (role_id == null ? 0 : role_id.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        UserRole other = (UserRole) obj;
        if(!Objects.equals(user_id, other.user_id)){
            return false;
        }
        if(!Objects.equals(role_id, other.role_id)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
