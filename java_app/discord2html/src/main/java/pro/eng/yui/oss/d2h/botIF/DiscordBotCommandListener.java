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
        final String command = event.getName();
        final String sub = event.getSubcommandName();
        
        if(event.isGuildCommand() == false) {
            event.reply("commands is enabled only in server channel").queue();
            return;
        }
        
        if(commands.contains(command) == false) {
            event.reply("bot D2H has called but was not supported")
                    .setSuppressedNotifications(true)
                    .queue();
            return;
        }
        if(sub == null) {
            event.reply("command /D2H required more command message. Use `/d2h help`")
                    .setSuppressedNotifications(true)
                    .queue();
            return;
        }
        switch(sub) {
            case "archive" -> runArchive(event);
            case "run" -> runRun(event);
            case "role" -> runRole(event);
            case "anonymous" -> runAnonymous(event);
            case "me" -> runMe(event);
            case "help" -> runHelp(event);
            default -> {
                event.reply("unknown subcommand. Use `/d2h help`")
                        .setSuppressedNotifications(true)
                        .queue();
            }
        }
    }
    
    protected boolean hasAdminPermission(SlashCommandInteractionEvent event){
        if(bot.isD2hAdmin(event.getMember()) == false) {
            event.reply("you do NOT have required permission(role) to do this").queue();
            return false;
        }
        return true;
    }
    
    private void runArchive(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        event.reply("archive command is running!").queue();
    }
    
    private void runRun(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        event.reply("run command is running!").queue();
    }
    
    private void runRole(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        event.reply("role command is running!").queue();
    }
    
    private void runMe(SlashCommandInteractionEvent event){
        //do not need to check //if(hasAdminPermission(event) == false) == false)
        event.reply("me command is running!").queue();
    }
    
    private void runAnonymous(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        event.reply("anonymous command is running!").queue();
    }
    
    private void runHelp(SlashCommandInteractionEvent event){
        //do not need to check //if(hasAdminPermission(event) == false) == false)
        boolean isAdmin = bot.isD2hAdmin(event.getMember());
        event.reply("help command is running").queue();
    }

}
