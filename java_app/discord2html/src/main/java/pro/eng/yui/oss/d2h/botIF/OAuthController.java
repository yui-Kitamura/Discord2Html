package pro.eng.yui.oss.d2h.botIF;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthController {
    
    @GetMapping("/success")
    public String success(@AuthenticationPrincipal OAuth2AuthenticationToken authentication, Model model) {
        model.addAttribute("user", authentication.getPrincipal().getAttributes());
        return "success";
    }

}
