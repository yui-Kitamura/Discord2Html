package pro.eng.yui.oss.d2h.db;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.config.Secrets;

@Service
public class DbConnection {
    
    private final DbConfig config;
    private final Secrets secrets;
    
    @Autowired
    public DbConnection(DbConfig dbConfig, Secrets secrets){
        this.config = dbConfig;
        this.secrets = secrets;
    }
    
    @Bean
    public PooledDataSource dataSource(){
        String uri = config.getUrl();
        System.out.println(uri);
        PooledDataSource dataSource;
        dataSource = new PooledDataSource(
                "org.mariadb.jdbc.Driver", uri,
                config.getUserId(),
                secrets.getDatabasePass()
        );
        dataSource.setPoolMaximumActiveConnections(
                config.getServerConnectionMax()
        );
        dataSource.setPoolTimeToWait(
                config.getServerConnectionTimeout() * 1000
        );
        return dataSource;
    }
}
