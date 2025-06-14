package pro.eng.yui.oss.d2h.db;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class DbConnection {
    
    private final DbConfig config;
    
    @Autowired
    public DbConnection(DbConfig dbConfig){
        this.config = dbConfig;
    }
    
    @Bean
    public PooledDataSource dataSource(){
        PooledDataSource dataSource;
        dataSource = new PooledDataSource(
                "org.mariadb.jdbc.Driver",
                String.format("jdbc:mariadb://%s:%d/%s",
                        config.getHost().getAddress(),
                        config.getHost().getPort(),
                        config.getHost().getSchema()
                ),
                config.getUser().getId(),
                config.getUser().getPassword()
        );
        dataSource.setPoolMaximumActiveConnections(
                config.getServer().getConnection().getMax()
        );
        dataSource.setPoolTimeToWait(
                config.getServer().getConnection().getTimeout() * 1000
        );
        return dataSource;
    }
}
