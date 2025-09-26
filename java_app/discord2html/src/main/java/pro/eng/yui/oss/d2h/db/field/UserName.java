package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.User;

public class UserName extends AbstName {
    
    public static int LIMIT = 255;
    
    public UserName(String value){
        super(value, LIMIT);
    }
    public UserName(User user){
        this(user.getName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean s = super.equals(obj);
        if(s) {
            //追加要素あればここで検証
            return true;
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
