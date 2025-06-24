package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class ApplicationConfig {

    @Value("${discord.client.id}")
    private String discordClientId;
    public String getDiscordClientId() {
        return discordClientId;
    }
    
    @Value("${discord.oauth2.redirectUri}")
    private String discordAuthRedirectUri;
    public String getDiscordAuthRedirectUri(){
        return discordAuthRedirectUri;
    }
    
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter(){
        return new ForwardedHeaderFilter();
    }

}