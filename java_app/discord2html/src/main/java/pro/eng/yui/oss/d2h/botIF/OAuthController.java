package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;

@Controller
public class OAuthController {
    
    private final ApplicationConfig config;
    private final Secrets secrets;
    
    @Autowired
    public OAuthController(ApplicationConfig config, Secrets secrets){
        this.config = config;
        this.secrets = secrets;
    }

    @GetMapping("/oauth2/callback")
    public String callback(@RequestParam(name = "code", required = false) String code,
                           @RequestParam(name = "error", required = false) String error,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", error);
            return "error";
        }
        if (code == null) {
            model.addAttribute("error", "No authorization code provided.");
            return "error";
        }

        // Discordへトークンリクエスト
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", config.getDiscordClientId());
        params.add("client_secret", secrets.getDiscordSecret());
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", config.getDiscordRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://discord.com/api/oauth2/token", request, String.class
        );

        // レスポンス内容によって分岐
        if (response.getStatusCode().is2xxSuccessful()) {
            model.addAttribute("tokenResponse", response.getBody());
            
            //TODO DBへの鯖情報登録
            
            return "success";
        } else {
            model.addAttribute("error", "Failed to exchange code for token");
            return "error";
        }
    }

}
