package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.nio.file.Path;

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
    public Path getOutputPath(){
        return Path.of(outputPath);
    }
    
    @Value("${d2h.github.push:false}")
    private boolean pushToGitHub;
    public boolean getPushToGitHub(){
        return pushToGitHub;
    }

    // Maximum number of days users can look back for manual archive runs (inclusive). Default 7 days.
    @Value("${d2h.run.maxLookbackDays}")
    private int maxLookbackDays;
    public int getMaxLookbackDays() { return maxLookbackDays; }

    @Value("${github.repo.name}")
    private String githubRepoName;
    public String getGithubRepoName() {
        return githubRepoName;
    }
    
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter(){
        return new ForwardedHeaderFilter();
    }

}