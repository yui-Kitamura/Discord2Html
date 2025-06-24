package pro.eng.yui.oss.d2h.botIF;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pro.eng.yui.oss.d2h.db.model.Users;

@Controller
public class OAuthController {

    private final DiscordBot bot;
    private final OAuthService service;
    
    @Autowired
    public OAuthController(DiscordBot bot, OAuthService service){
        this.bot = bot;
        this.service = service;
    }
    
    @GetMapping(DiscordApiClient.REDIRECT_PATH)
    public String oauth2redirect(@RequestParam("code") String code,
                                 @RequestParam(value = "guild_id", required = false) String guildId,
                                 Model model
    ) {
    
        try {
            System.out.println(code);

            ResponseToken tokenInfo = service.callApiGetAccessTokenByCode(code);

            System.out.println(tokenInfo);
            
            Users user = service.getUserByToken(tokenInfo.getAccessToken());

            System.out.println(user);
            
            //Token情報の登録
            service.registerOrUpdateNewToken(user, tokenInfo);
            bot.refreshToken();
            
        }catch(Exception e) {
            throw new IllegalStateException("authできましたが処理エラーです", e);
        }

        //画面に返す
        // model.addAttribute("user", authentication.getPrincipal().getAttributes());
        return "success";
    }

}
