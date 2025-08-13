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
    
    @Value("${discord.oauth2.redirectUri.host}")
    private String discordAuthRedirectUriHost;
    public String getDiscordAuthRedirectUriHost(){
        return discordAuthRedirectUriHost;
    }

    @Value("${d2h.output.path}")
    private String outputPath;
    public String getOutputPath(){
        return outputPath;
    }
    
    @Value("${d2h.github.push:false}")
    private boolean pushToGitHub;
    public boolean getPushToGitHub(){
        return pushToGitHub;
    }
    
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter(){
        return new ForwardedHeaderFilter();
    }

}