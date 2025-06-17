package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.AdminFlg;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.BOOLEAN)
@MappedTypes(AdminFlg.class)
public class AdminFlgHandler extends BaseTypeHandler<AdminFlg> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AdminFlg parameter, JdbcType jdbcType) throws SQLException {
        ps.setBoolean(i, parameter.getValue());
    }

    @Override
    public AdminFlg getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean val = rs.getBoolean(columnName);
        return rs.wasNull() ? null : new AdminFlg(val);
    }

    @Override
    public AdminFlg getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean val = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : new AdminFlg(val);
    }

    @Override
    public AdminFlg getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        boolean val = cs.getBoolean(columnIndex);
        return cs.wasNull() ? null : new AdminFlg(val);
    }
}
