package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pro.eng.yui.oss.d2h.db.field.AccessToken;
import pro.eng.yui.oss.d2h.db.field.RefreshToken;

@Controller
public class OAuthController {

    private final OAuth2AuthorizedClientService authClientService;

    @Autowired
    public OAuthController(OAuth2AuthorizedClientService authClientService){
        this.authClientService = authClientService;
    }
    
    @GetMapping("/success")
    public String success(@AuthenticationPrincipal OAuth2AuthenticationToken authentication, Model model) {

        OAuth2AuthorizedClient client = authClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client != null) {
            AccessToken tokenValue = null;
            RefreshToken refreshToken = null;
            if (client.getAccessToken() != null) {
                tokenValue = new AccessToken(client.getAccessToken().getTokenValue());
            }
            if (client.getRefreshToken() != null) {
                refreshToken = new RefreshToken(client.getRefreshToken().getTokenValue());
            }
            
            //TODO DB-insert or update
            
        } else {
            throw new IllegalStateException("authできましたがclientが取得できません");
        }

        //画面に返す
        model.addAttribute("user", authentication.getPrincipal().getAttributes());
        return "success";
    }

}
