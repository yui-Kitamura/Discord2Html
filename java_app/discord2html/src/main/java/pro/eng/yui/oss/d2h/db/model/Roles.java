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
    
    private AdminFlg admin_flg;
    public void setAdminFlg(AdminFlg newValue){
        this.admin_flg = newValue;
    }
    public AdminFlg getAdminFlg() {
        return admin_flg;
    }

    private AnonFlg anon_flg;
    public void setAnonFlg(AnonFlg newValue){
        this.anon_flg = newValue;
    }
    public AnonFlg getAnonFlg(){
        return anon_flg;
    }

    public Roles(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (role_id == null ? 0 : role_id.hashCode());
        hash = hash * 31 + (admin_flg == null ? 0 : admin_flg.hashCode());
        hash = hash * 31 + (anon_flg == null ? 0 : anon_flg.hashCode());
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
        if(!Objects.equals(admin_flg, other.admin_flg)){
            return false;
        }
        if(!Objects.equals(anon_flg, other.anon_flg)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
