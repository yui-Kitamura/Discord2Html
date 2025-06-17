package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.IssuedAt;

import java.sql.*;

@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(IssuedAt.class)
public class IssuedAtHandler extends BaseTypeHandler<IssuedAt> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, IssuedAt parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter.getValue());
    }

    @Override
    public IssuedAt getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp val = rs.getTimestamp(columnName);
        return rs.wasNull() ? null : new IssuedAt(val);
    }

    @Override
    public IssuedAt getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp val = rs.getTimestamp(columnIndex);
        return rs.wasNull() ? null : new IssuedAt(val);
    }

    @Override
    public IssuedAt getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp val = cs.getTimestamp(columnIndex);
        return cs.wasNull() ? null : new IssuedAt(val);
    }
}
