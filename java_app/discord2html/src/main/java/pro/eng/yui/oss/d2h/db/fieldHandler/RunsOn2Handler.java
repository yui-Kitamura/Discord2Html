package pro.eng.yui.oss.d2h.db.fieldHandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import pro.eng.yui.oss.d2h.db.field.RunsOn2;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.INTEGER)
@MappedTypes(RunsOn2.class)
public class RunsOn2Handler extends BaseTypeHandler<RunsOn2> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RunsOn2 parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    @Override
    public RunsOn2 getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int val = rs.getInt(columnName);
        return rs.wasNull() ? null : new RunsOn2(val);
    }

    @Override
    public RunsOn2 getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int val = rs.getInt(columnIndex);
        return rs.wasNull() ? null : new RunsOn2(val);
    }

    @Override
    public RunsOn2 getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int val = cs.getInt(columnIndex);
        return cs.wasNull() ? null : new RunsOn2(val);
    }
}
