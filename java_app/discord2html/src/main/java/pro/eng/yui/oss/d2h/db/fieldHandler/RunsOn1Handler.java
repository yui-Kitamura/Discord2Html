package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.RunsOn1;

import java.sql.*;

@MappedJdbcTypes(JdbcType.INTEGER)
@MappedTypes(RunsOn1.class)
public class RunsOn1Handler extends BaseTypeHandler<RunsOn1> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RunsOn1 parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public RunsOn1 getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int val = rs.getInt(columnName);
        return rs.wasNull() ? null : new RunsOn1(val);
    }

    @Override
    public RunsOn1 getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int val = rs.getInt(columnIndex);
        return rs.wasNull() ? null : new RunsOn1(val);
    }

    @Override
    public RunsOn1 getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int val = cs.getInt(columnIndex);
        return cs.wasNull() ? null : new RunsOn1(val);
    }
}
