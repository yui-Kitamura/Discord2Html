package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DiscordBotCommandListener extends ListenerAdapter {
    
    public static final List<String> commands = Arrays.asList("d2h");
    public static final List<SubcommandData> D2H_SUB_COMMANDS = List.of(
            new SubcommandData("archive", "change channel archive settings")
                    .addOption(OptionType.CHANNEL, "channel", "target channel", true)
                    .addOption(OptionType.BOOLEAN, "mode", "do archive", true)
            ,
            new SubcommandData("run", "run archive function now")
                    .addOption(OptionType.CHANNEL, "target","target channel", false)
            ,
            new SubcommandData("role", "change role archive settings")
                    .addOption(OptionType.ROLE, "target", "target role", true)
                    .addOptions(new OptionData(
                            OptionType.STRING, "role", "role", true
                            )
                            .addChoice("anonymous", "anonymous")
                            .addChoice("showName", "showName")
                    )
            ,
            new SubcommandData("anonymous", "change anonymous users archive settings")
                    .addOptions(new OptionData(
                            OptionType.STRING, "menu", "setting menu", true)
                            .addChoice("cycle", "cycle")
                            
                    )
                    .addOption(OptionType.INTEGER, "cycle", "change name cycle", true)
            ,
            new SubcommandData("me", "change your archive settings")
                    .addOption(OptionType.BOOLEAN, "anonymous", "hide your name", true)
            ,
            new SubcommandData("help", "send you about this bots command help")
    );

    private final DiscordBotUtils bot;

    @Autowired
    public DiscordBotCommandListener(DiscordBotUtils bot){
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        final String command = event.getName();
        final String sub = event.getSubcommandName();
        
        if((event.getChannel() instanceof GuildChannel) == false) {
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
    
    /** 汎用Admin権限チェック。エラーメッセージのレスポンスつき */
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
