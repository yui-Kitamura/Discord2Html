package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthController {

    private final OAuth2AuthorizedClientService authClientService;
    private final OAuthService service;

    @Autowired
    public OAuthController(
            OAuth2AuthorizedClientService authClientService,
            OAuthService service){
        this.authClientService = authClientService;
        this.service = service;
    }
    
    @GetMapping("/success")
    public String success(@AuthenticationPrincipal OAuth2AuthenticationToken authentication, Model model) {

        OAuth2AuthorizedClient client = authClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client != null) {
            try {
                //Token情報の登録
                service.registerOrUpdateNewToken(client);
            }catch(Exception e) {
                throw new IllegalStateException("authできましたが処理エラーです", e);
            }
        } else {
            throw new IllegalStateException("authできましたがclientが取得できません");
        }

        //画面に返す
        model.addAttribute("user", authentication.getPrincipal().getAttributes());
        return "success";
    }

}
