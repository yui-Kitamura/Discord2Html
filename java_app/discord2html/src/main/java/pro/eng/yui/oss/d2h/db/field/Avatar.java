package pro.eng.yui.oss.d2h.db.field;

import java.net.MalformedURLException;
import java.net.URL;

public class Avatar extends AbstVarChar {
    
    public static String IMG_HOST = "https://cdn.discordapp.com/avatars";
    public static int LIMIT = 255;
    
    public Avatar(String value){
        super(value, LIMIT);
    }
    
    public URL getImgPath(UserId userId){
        try {
            return new URL(IMG_HOST + "/" + userId.toString() + "/" + this.value + ".png");
        }catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }
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
