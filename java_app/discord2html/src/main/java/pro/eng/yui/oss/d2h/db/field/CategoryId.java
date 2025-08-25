package pro.eng.yui.oss.d2h.db.field;

import net.dv8tion.jda.api.entities.channel.concrete.Category;

public class CategoryId extends AbstIds{
    public CategoryId(long value){
        super(value);
    }
    public CategoryId(Category category){
        super(category.getIdLong());
    }

    @Override
    public int hashCode() { return super.hashCode(); }

    @Override
    public boolean equals(Object obj) { return super.equals(obj); }

    @Override
    public String toString() { return super.toString(); }
}
