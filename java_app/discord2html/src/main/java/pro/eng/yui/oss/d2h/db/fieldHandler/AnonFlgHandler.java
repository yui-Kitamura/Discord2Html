package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.AnonFlg;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.BOOLEAN)
@MappedTypes(AnonFlg.class)
public class AnonFlgHandler extends BaseTypeHandler<AnonFlg> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AnonFlg parameter, JdbcType jdbcType) throws SQLException {
        ps.setBoolean(i, parameter.getValue());
    }

    @Override
    public AnonFlg getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean val = rs.getBoolean(columnName);
        return rs.wasNull() ? null : new AnonFlg(val);
    }

    @Override
    public AnonFlg getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean val = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : new AnonFlg(val);
    }

    @Override
    public AnonFlg getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        boolean val = cs.getBoolean(columnIndex);
        return cs.wasNull() ? null : new AnonFlg(val);
    }
}
