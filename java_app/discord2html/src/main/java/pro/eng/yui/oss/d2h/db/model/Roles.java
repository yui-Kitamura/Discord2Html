package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.*;

import java.util.Objects;

public class Roles {

    private RoleId role_id;
    public void setRoleId(RoleId newValue){
        this.role_id = newValue;
    }
    public RoleId getRoleId(){
        return role_id;
    }

    private AnonStats anon_stats;
    public void setAnonStats(AnonStats newValue){
        this.anon_stats = newValue;
    }
    public AnonStats getAnonStats(){
        return anon_stats;
    }

    public Roles(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (role_id == null ? 0 : role_id.hashCode());
        hash = hash * 31 + (anon_stats == null ? 0 : anon_stats.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        Roles other = (Roles) obj;
        if(!Objects.equals(role_id, other.role_id)){
            return false;
        }
        if(!Objects.equals(anon_stats, other.anon_stats)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
