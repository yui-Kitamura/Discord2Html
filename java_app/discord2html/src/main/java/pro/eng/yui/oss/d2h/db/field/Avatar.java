package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.User;

public class Avatar extends AbstVarChar {
    
    public static String IMG_HOST = "https://cdn.discordapp.com/avatars";
    public static int LIMIT = 255;
    
    public Avatar(String value){
        super(value, LIMIT);
    }
    public Avatar(User user){
        this(user.getAvatarId());
    }
    
    public String getImgPath(UserId userId){
        if (this.value == null || this.value.isEmpty()) {
            // Fallback to Discord's default embed avatar when user has no custom avatar
            return "https://cdn.discordapp.com/embed/avatars/0.png";
        }
        return IMG_HOST + "/" + userId.toString() + "/" + this.value + ".png";
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
