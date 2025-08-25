package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.OnRunUrl;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(OnRunUrl.class)
public class OnRunUrlHandler extends BaseTypeHandler<OnRunUrl> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OnRunUrl parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public OnRunUrl getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        return rs.wasNull() ? null : new OnRunUrl(val);
    }

    @Override
    public OnRunUrl getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        return rs.wasNull() ? null : new OnRunUrl(val);
    }

    @Override
    public OnRunUrl getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        return cs.wasNull() ? null : new OnRunUrl(val);
    }
}
