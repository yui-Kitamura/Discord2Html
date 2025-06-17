package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.RoleId;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.BIGINT)
@MappedTypes(RoleId.class)
public class RoleIdHandler extends BaseTypeHandler<RoleId> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RoleId parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter.getValue());
    }

    @Override
    public RoleId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long val = rs.getLong(columnName);
        return rs.wasNull() ? null : new RoleId(val);
    }

    @Override
    public RoleId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        long val = rs.getLong(columnIndex);
        return rs.wasNull() ? null : new RoleId(val);
    }

    @Override
    public RoleId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        long val = cs.getLong(columnIndex);
        return cs.wasNull() ? null : new RoleId(val);
    }
}
