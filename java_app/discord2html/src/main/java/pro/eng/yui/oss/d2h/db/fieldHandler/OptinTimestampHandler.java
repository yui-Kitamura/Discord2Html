package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.OptinTimestamp;

import java.sql.*;

@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(OptinTimestamp.class)
public class OptinTimestampHandler extends BaseTypeHandler<OptinTimestamp> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OptinTimestamp parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter.getValue());
    }

    @Override
    public OptinTimestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp val = rs.getTimestamp(columnName);
        return rs.wasNull() ? null : new OptinTimestamp(val);
    }

    @Override
    public OptinTimestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp val = rs.getTimestamp(columnIndex);
        return rs.wasNull() ? null : new OptinTimestamp(val);
    }

    @Override
    public OptinTimestamp getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp val = cs.getTimestamp(columnIndex);
        return cs.wasNull() ? null : new OptinTimestamp(val);
    }
}
