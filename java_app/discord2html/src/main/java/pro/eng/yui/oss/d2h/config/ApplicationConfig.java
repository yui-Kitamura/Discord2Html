package pro.eng.yui.oss.d2h.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApplicationConfig {

    @Value("${discord.client.id}")
    private String discordClientId;
    public String getDiscordClientId() {
        return discordClientId;
    }

    /** d2hプレフィックスをURLに付与する */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/d2h/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/d2h/oauth2/authorization/discord")
                        .defaultSuccessUrl("/d2h/success", true)
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/d2h/login/oauth2/code/*")
                        )
                );
        return http.build();
    }

}