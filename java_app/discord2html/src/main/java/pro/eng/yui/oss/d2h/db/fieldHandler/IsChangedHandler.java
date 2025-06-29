package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.IsChanged;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.BOOLEAN)
@MappedTypes(IsChanged.class)
public class IsChangedHandler extends BaseTypeHandler<IsChanged> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IsChanged parameter, JdbcType jdbcType) throws SQLException {
        ps.setBoolean(i, parameter.getValue());
    }

    @Override
    public IsChanged getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean val = rs.getBoolean(columnName);
        return rs.wasNull() ? null : new IsChanged(val);
    }

    @Override
    public IsChanged getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean val = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : new IsChanged(val);
    }

    @Override
    public IsChanged getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        boolean val = cs.getBoolean(columnIndex);
        return cs.wasNull() ? null : new IsChanged(val);
    }
}
