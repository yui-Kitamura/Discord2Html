package pro.eng.yui.oss.d2h.botIF;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuthController {

    public OAuthController(){
        // nothing to do
    }
    
    @GetMapping("/login/oauth2/code/discord")
    public String oauth2redirect(@RequestParam("code") String code,
                                 @RequestParam(value = "guild_id", required = false) String guildId,
                                 Model model
    ) {
    
        try {
            System.out.println(code);
            //Token情報の登録
            //service.registerOrUpdateNewToken(client);
        }catch(Exception e) {
            throw new IllegalStateException("authできましたが処理エラーです", e);
        }

        //画面に返す
        // model.addAttribute("user", authentication.getPrincipal().getAttributes());
        return "success";
    }

}
