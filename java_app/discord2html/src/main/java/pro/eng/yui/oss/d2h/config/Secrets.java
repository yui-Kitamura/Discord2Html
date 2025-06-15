package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:secret.properties")
public class Secrets {
    
    @Value("discord.token")
    private String discord_token;
    public String getDiscordToken() {
        return discord_token;
    }
    
    @Value("database.pass")
    private String database_pass;
    public String getDatabasePass() {
        return database_pass;
    }
}
