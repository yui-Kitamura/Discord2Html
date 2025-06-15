package pro.eng.yui.oss.d2h.db;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@PropertySource("classpath:database.properties")
@ConfigurationProperties(prefix = "database")
@Component
public class DbConfig {

    private Host host;
    public void setHost(Host host) {
        this.host = host;
    }
    public Host getHost() {
        return host;
    }
    public static class Host {
        /** db address. by default <code>localhost</code> */
        private String address = "localhost";
        public void setAddress(String address) {
            this.address = address;
        }
        public String getAddress() {
            return address;
        }

        /** db port number. usually 3036, but Dev uses 3006.
         * by default <code>3036</code> */
        private int port = 3036;
        public void setPort(int port) {
            this.port = port;
        }
        public int getPort() {
            return port;
        }

        /** database schema name. by default <code>discord2html</code>
         */
        private String schema = "discord2html";
        public void setSchema(String schema) {
            this.schema = schema;
        }
        public String getSchema() {
            return schema;
        }
    }

    private User user;
    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }    
    public static class User {
        private String id;
        public void setId(String id) {
            this.id = id;
        }
        public String getId() {
            return id;
        }
    }

    private Server server;
    public void setServer(Server server) {
        this.server = server;
    }
    public Server getServer() {
        return server;
    }
    public static class Server {
        private Connection connection;
        public void setConnection(Connection connection) {
            this.connection = connection;
        }
        public Connection getConnection() {
            return connection;
        }
        
        public static class Connection {
            /** max connection amount. by default <code>5</code> */
            private int max = 5;
            public void setMax(int max) {
                this.max = max;
            }
            public int getMax() {
                return max;
            }
            
            /** connection timeout in seconds. by default <code>10</code> */
            private int timeout = 10;
            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }
            public int getTimeout() {
                return timeout;
            }
        }
    }
}
