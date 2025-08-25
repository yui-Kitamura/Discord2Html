package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.channel.concrete.Category;

public class CategoryName extends AbstVarChar {
    public static int LIMIT = 255;

    public CategoryName(String value){
        super(value, LIMIT);
    }
    public CategoryName(Category category){
        this(category.getName());
    }

    @Override
    public int hashCode() { return super.hashCode(); }

    @Override
    public boolean equals(Object obj) { return super.equals(obj); }

    @Override
    public String toString() { return super.toString(); }
}
