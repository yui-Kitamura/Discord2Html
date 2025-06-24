package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Secrets {
    
    @Value("${discord.client_secret}")
    private String discord_auth;
    public String getDiscordAuth() {
        return discord_auth;
    }
    
    @Value("${discord.bot.token}")
    private String discord_bot_token;
    public String getDiscordBotToken(){
        return discord_bot_token;
    }

    @Value("${github.token}")
    private String github_token;
    public String getGitHubToken(){
        return github_token;
    }
}
