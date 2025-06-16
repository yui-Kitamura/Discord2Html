package pro.eng.yui.oss.d2h;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:spring.xml")
public class DiscordToHtmlBot {
    public static void main(String[] args) {
        SpringApplication.run(DiscordToHtmlBot.class, args);
    }
}