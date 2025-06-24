package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.HashMap;
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

    public Users fetchUserByToken(AccessToken token){
        Map userInfo = get("/users/@me", token, Map.class);
        Users user = new Users();
        user.setUserId(new UserId(Long.parseUnsignedLong(userInfo.get("id").toString())));
        user.setNickname(new Nickname(userInfo.get("username").toString()));
        user.setUserName(new UserName(userInfo.get("global_name").toString()));
        user.setAvatar(new Avatar(userInfo.get("avatar").toString()));
        
        return user;
    }

    public ResponseToken exchangeCodeForToken(String code) {
        return post(code);
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
    
    protected ResponseToken post(String code){
        Map<String, String> postMap = new HashMap<>();
        postMap.put("grant_type", "authorization_code");
        postMap.put("code", code);
        return post("/oauth2/token", postMap, ResponseToken.class);
    }
    
    protected <T> T post(String endpoint, AccessToken accessToken, Class<T> responseType){
        Map<String, String> postMap = new HashMap<>();
        postMap.put("access_token", accessToken.getValue());
        return post(endpoint, postMap, responseType);
    }
    
    private <T> T post(String endpoint, Map<String, String> postMap, Class<T> responseType){
        String url = DISCORD_API_BASE + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        postMap.put("client_id", appConfig.getDiscordClientId());
        postMap.put("client_secret", secrets.getDiscordAuth());
        postMap.put("redirect_uri", REDIRECT_URI);

        HttpEntity<Map<String,String>> request = new HttpEntity<>(postMap, headers);

        ResponseEntity<T> response = restTemplate.postForEntity(url, request, responseType);
        return response.getBody();
    }

}
