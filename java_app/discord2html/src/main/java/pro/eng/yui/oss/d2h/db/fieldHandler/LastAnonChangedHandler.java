package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.LastAnonChanged;

import java.sql.*;

@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(LastAnonChanged.class)
public class LastAnonChangedHandler extends BaseTypeHandler<LastAnonChanged> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LastAnonChanged parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter.getValue());
    }

    @Override
    public LastAnonChanged getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp val = rs.getTimestamp(columnName);
        return rs.wasNull() ? null : new LastAnonChanged(val);
    }

    @Override
    public LastAnonChanged getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp val = rs.getTimestamp(columnIndex);
        return rs.wasNull() ? null : new LastAnonChanged(val);
    }

    @Override
    public LastAnonChanged getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp val = cs.getTimestamp(columnIndex);
        return cs.wasNull() ? null : new LastAnonChanged(val);
    }
}
