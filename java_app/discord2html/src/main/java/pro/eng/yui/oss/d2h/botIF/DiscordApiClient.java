package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.db.field.AccessToken;
import pro.eng.yui.oss.d2h.db.field.UserId;

import java.util.Map;

@Service
public class DiscordApiClient {

    public static final String REDIRECT_PATH = "/login/oauth2/code/discord";
    private final String DISCORD_API_BASE = "https://discord.com/api";
    private final String REDIRECT_URI;

    private final RestTemplate restTemplate;

    private final ApplicationConfig appConfig;
    private final Secrets secrets;

    @Autowired
    public DiscordApiClient(ApplicationConfig appConfig, Secrets secrets) {
        this.restTemplate = new RestTemplate();
        this.appConfig = appConfig;
        this.secrets = secrets;
        this.REDIRECT_URI = appConfig.getDiscordAuthRedirectUriHost() + REDIRECT_PATH;
    }

    protected <T> T get(String endpoint, AccessToken accessToken, Class<T> responseType) {
        String url = DISCORD_API_BASE + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getValue());
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                responseType
        );
        return response.getBody();
    }
    
    public UserId fetchUserId(AccessToken token){
        Map userInfo = get("/users/@me", token, Map.class);
        return new UserId(Long.parseUnsignedLong(userInfo.get("id").toString()));
    }

}
