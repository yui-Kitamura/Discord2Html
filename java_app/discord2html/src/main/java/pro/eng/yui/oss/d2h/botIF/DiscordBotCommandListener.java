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
import pro.eng.yui.oss.d2h.botIF.runner.*;

import java.util.Arrays;
import java.util.List;

@Component
public class DiscordBotCommandListener extends ListenerAdapter {
    
    public static final List<String> commands = Arrays.asList("d2h");
    public static final List<SubcommandData> D2H_SUB_COMMANDS = List.of(
            new SubcommandData("archive", "change channel archive settings")
                    .addOption(OptionType.CHANNEL, "channel", "target channel", true)
                    .addOptions(new OptionData(
                            OptionType.STRING, "mode", "do archive", true
                            )
                            .addChoice("ignore", "ignore")
                            .addChoice("monitor", "monitor")
                    )
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
    private final RoleRunner roleRunner;
    private final AnonymousSettingRunner anonymousSettingRunner;
    private final MeRunner meRunner;
    private final HelpRunner helpRunner;
    private final RunArchiveRunner runArchiveRunner;
    private final ArchiveConfigRunner archiveConfigRunner;

    @Autowired
    public DiscordBotCommandListener(DiscordBotUtils bot,
                                     HelpRunner help, MeRunner me, RoleRunner role,
                                     AnonymousSettingRunner anon, RunArchiveRunner run,
                                     ArchiveConfigRunner archive){
        this.bot = bot;
        this.roleRunner = role;
        this.anonymousSettingRunner = anon;
        this.meRunner = me;
        this.helpRunner = help;
        this.runArchiveRunner = run;
        this.archiveConfigRunner = archive;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        final String command = event.getName();
        final String sub = event.getSubcommandName();
        
        if((event.getChannel() instanceof GuildChannel) == false) {
            event.reply("commands is enabled only in server channel").queue();
            return;
        }
        if (isAcceptedChannel(event.getGuildChannel()) == false) {
            event.reply("you can NOT USE commands in this channel")
                    .setEphemeral(true) //visible=false
                    .queue();
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

        event.deferReply().queue();
        try {

            bot.upsertGuildInfoToDB(event.getGuild());
            bot.upsertGuildChannelToDB(event.getGuild());

            switch (sub) {
                case "archive" -> runArchive(event);
                case "run" -> runRun(event);
                case "role" -> runRole(event);
                case "anonymous" -> runAnonymous(event);
                case "me" -> runMe(event);
                case "help" -> runHelp(event);
                default -> {
                    event.getHook()
                            .sendMessage("unknown subcommand. Use `/d2h help`")
                            .setSuppressedNotifications(true)
                            .queue();
                }
            }
        }catch(Exception unexpected) {
            event.getHook()
                .sendMessage("something wrong in bot server. >> `"+ unexpected.getMessage() +"`")
                .queue();
            return;
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
    /** コマンド実行チャンネルの確認 */
    protected boolean isAcceptedChannel(GuildChannel channel){
        return bot.getAdminTaggedChannelList(channel.getGuild()).contains(channel);
    }
    
    private void runArchive(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        if(isAcceptedChannel(event.getGuildChannel()) == false) {
            return;
        }
        archiveConfigRunner.run(event.getOptions());
        event.getHook().sendMessage(archiveConfigRunner.afterRunMessage()).queue();
    }
    
    private void runRun(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        if(isAcceptedChannel(event.getGuildChannel()) == false) {
            return;
        }
        runArchiveRunner.run(event.getMember(), event.getOptions());
        event.getHook().sendMessage(runArchiveRunner.afterRunMessage()).queue();
    }
    
    private void runRole(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        roleRunner.run(event.getMember(), event.getOptions());
        event.getHook().sendMessage(roleRunner.afterRunMessage()).queue();
    }
    
    private void runMe(SlashCommandInteractionEvent event){
        //do not need to check //if(hasAdminPermission(event) == false) == false)
        meRunner.run(event.getMember(), event.getOptions());
        event.getHook().sendMessage(meRunner.afterRunMessage()).queue();
    }
    
    private void runAnonymous(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        anonymousSettingRunner.run(event.getGuild(), event.getOptions());
        event.getHook().sendMessage(anonymousSettingRunner.afterRunMessage()).queue();
    }
    
    private void runHelp(SlashCommandInteractionEvent event){
        //do not need to check //if(hasAdminPermission(event) == false) == false)
        helpRunner.run(event.getMember(), bot.isD2hAdmin(event.getMember()));
        event.getHook().sendMessage(helpRunner.afterRunMessage()).queue();
    }

}
