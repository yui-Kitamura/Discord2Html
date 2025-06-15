package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    
    @Value("discord.client.id")
    private String discordClientId;
    public String getDiscordClientId() {
        return discordClientId;
    }
    
    @Value("discord.redirect.uri")
    private String discordRedirectUri;
    public String getDiscordRedirectUri(){
        return discordRedirectUri;
    }
    
}
