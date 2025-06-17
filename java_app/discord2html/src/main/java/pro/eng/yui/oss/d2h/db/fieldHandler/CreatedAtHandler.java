package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.CreatedAt;

import java.sql.*;

@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(CreatedAt.class)
public class CreatedAtHandler extends BaseTypeHandler<CreatedAt> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, CreatedAt parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter.getValue());
    }

    @Override
    public CreatedAt getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp val = rs.getTimestamp(columnName);
        return rs.wasNull() ? null : new CreatedAt(val);
    }

    @Override
    public CreatedAt getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp val = rs.getTimestamp(columnIndex);
        return rs.wasNull() ? null : new CreatedAt(val);
    }

    @Override
    public CreatedAt getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp val = cs.getTimestamp(columnIndex);
        return cs.wasNull() ? null : new CreatedAt(val);
    }
}
