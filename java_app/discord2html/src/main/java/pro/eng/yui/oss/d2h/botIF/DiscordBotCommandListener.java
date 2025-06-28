package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DiscordBotCommandListener extends ListenerAdapter {
    
    public DiscordBotCommandListener(){
        // nothing to do
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        super.onSlashCommandInteraction(event);
    }
    
}
