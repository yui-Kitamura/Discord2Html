package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Secrets {
    
    @Value("${discord.auth}")
    private String discord_auth;
    public String getDiscordAuth() {
        return discord_auth;
    }
    
    @Value("${discord.client.secret}")
    private String discord_secret;
    public String getDiscordSecret(){
        return discord_secret;
    }
    
    @Value("${spring.datasource.password}")
    private String database_pass;
    public String getDatabasePass() {
        return database_pass;
    }
    
    @Value("${github.token}")
    private String github_token;
    public String getGitHubToken(){
        return github_token;
    }
}
