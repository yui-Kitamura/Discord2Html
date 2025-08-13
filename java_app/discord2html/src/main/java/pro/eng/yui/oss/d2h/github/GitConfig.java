package pro.eng.yui.oss.d2h.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "github")
@Component
public class GitConfig {

    private Repo repo;
    public void setRepo(Repo repo) {
        this.repo = repo;
    }
    public Repo getRepo() {
        return repo;
    }
    public static class Repo {

        private String owner;
        public void setOwner(String owner) {
            this.owner = owner;
        }
        public String getOwner() {
            return owner;
        }
        
        private String name;
        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
        
        private String main;
        public void setMain(String main) {
            this.main = main;
        }
        public String getMain() {
            return main;
        }

        public String getUrl() {
            return "https://github.com/" + owner + "/" + name;
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
    
    private Local local;
    public void setLocal(Local local) {
        this.local = local;
    }
    public Local getLocal() {
        return local;
    }

    public static class Local {

        private String dir;
        public void setDir(String dir) {
            this.dir = dir;
        }
        public String getDir() {
            return dir;
        }
    }
}
