package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.Guild;

public class GuildName extends AbstVarChar {
    
    public static int LIMIT = 255;
    
    public GuildName(String value){
        super(value, LIMIT);
    }
    public GuildName(Guild guild){
        this(guild.getName());
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
