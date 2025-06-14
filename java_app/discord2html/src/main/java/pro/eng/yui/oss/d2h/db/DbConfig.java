package pro.eng.yui.oss.d2h.db;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:discordBot.properties")
@ConfigurationProperties(prefix = "database")
public class DbConfig {

    private Host host;
    public void setHost(Host host) {
        this.host = host;
    }
    public Host getHost() {
        return host;
    }
    public static class Host {
        private String address;
        public void setAddress(String address) {
            this.address = address;
        }
        public String getAddress() {
            return address;
        }

        private int port;
        public void setPort(int port) {
            this.port = port;
        }
        public int getPort() {
            return port;
        }
        
        private String schema;
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

        private String password;
        public void setPassword(String password) {
            this.password = password;
        }
        public String getPassword() {
            return password;
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
            private int max;
            public void setMax(int max) {
                this.max = max;
            }
            public int getMax() {
                return max;
            }
            
            private int timeout;
            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }
            public int getTimeout() {
                return timeout;
            }
        }
    }
}
