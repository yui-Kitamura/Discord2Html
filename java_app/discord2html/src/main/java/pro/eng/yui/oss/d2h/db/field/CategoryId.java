package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.channel.concrete.Category;

public class CategoryId extends AbstIds{
    
    public static long NO_CATEGORY_ID = 0L;
    
    public CategoryId(long value){
        super(value);
    }
    public CategoryId(Category category){
        super(category.getIdLong());
    }
    
    public static CategoryId NO_CATEGORY = new CategoryId(NO_CATEGORY_ID);

    @Override
    public int hashCode() { return super.hashCode(); }

    @Override
    public boolean equals(Object obj) { return super.equals(obj); }

    @Override
    public String toString() { return super.toString(); }
}
