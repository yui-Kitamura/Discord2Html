package pro.eng.yui.oss.d2h.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbConfig {

    @Value("${spring.datasource.url}")
    private String url;
    public String getUrl() {
        return url;
    }

    @Value("${spring.datasource.username}")
    private String userId;
    public String getUserId() {
        return userId;
    }

    /**
     * max connection amount. by default <code>5</code>
     */
    @Value("${database.server.connection.max:5}")
    private int serverConnectionMax;
    public int getServerConnectionMax() {
        return serverConnectionMax;
    }

    /**
     * connection timeout in seconds. by default <code>10</code>
     */
    @Value("${database.server.connection.timeout:10}")
    private int serverConnectionTimeout;
    public int getServerConnectionTimeout() {
        return serverConnectionTimeout;
    }

}
