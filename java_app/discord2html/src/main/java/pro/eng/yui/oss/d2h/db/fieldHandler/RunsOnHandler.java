package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.RunsOn;

import java.sql.*;

@MappedJdbcTypes(JdbcType.INTEGER)
@MappedTypes(RunsOn.class)
public class RunsOnHandler extends BaseTypeHandler<RunsOn> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RunsOn parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public RunsOn getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int val = rs.getInt(columnName);
        return rs.wasNull() ? null : new RunsOn(val);
    }

    @Override
    public RunsOn getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int val = rs.getInt(columnIndex);
        return rs.wasNull() ? null : new RunsOn(val);
    }

    @Override
    public RunsOn getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int val = cs.getInt(columnIndex);
        return cs.wasNull() ? null : new RunsOn(val);
    }
}

