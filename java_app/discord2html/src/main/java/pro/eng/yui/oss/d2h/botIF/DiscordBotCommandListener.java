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
import pro.eng.yui.oss.d2h.db.field.GuildId;

import java.util.Arrays;
import java.util.List;

@Component
public class DiscordBotCommandListener extends ListenerAdapter {
    
    public static final List<String> commands = Arrays.asList("d2h");
    public static final List<SubcommandData> D2H_SUB_COMMANDS = List.of(
            new SubcommandData("archive", "change channel archive settings")
                    .addOption(OptionType.CHANNEL, "channel", "target channel", false)
                    .addOptions(new OptionData(
                            OptionType.STRING, "mode", "do archive", false
                            )
                            .addChoice("ignore", "ignore")
                            .addChoice("monitor", "monitor")
                    )
                    .addOptions(new OptionData(
                            OptionType.STRING, "onrunmessage", "toggle start/end/both message on run", false
                            )
                            .addChoice("start", "start")
                            .addChoice("end", "end")
                            .addChoice("both", "both")
                    )
                    .addOptions(new OptionData(
                            OptionType.STRING, "onrunurl", "toggle share url on run end", false
                            )
                            .addChoice("share", "share")
                            .addChoice("deny", "deny")
                    )
            ,
            new SubcommandData("run", "run archive function now")
                    .addOption(OptionType.CHANNEL, "target","target channel", false)
                    .addOption(OptionType.STRING, "date","target date (yyyyMMdd, JST)", false)
            ,
            new SubcommandData("role", "change role archive settings")
                    .addOption(OptionType.ROLE, "role", "target role", true)
                    .addOptions(new OptionData(
                            OptionType.STRING, "anonymous", "set user with role as", true
                            )
                            .addChoice("anonymous", "anonymous")
                            .addChoice("open", "open")
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
                    .addOptions(new OptionData(
                                    OptionType.STRING, "anonymous", "set you as", true
                            )
                                    .addChoice("anonymous", "anonymous")
                                    .addChoice("open", "open")
                    )
            ,
            new SubcommandData("optout", "opt-out or re-consent your archive settings")
                    .addOption(OptionType.CHANNEL, "channel", "target channel (omit for guild-wide)", false)
                    .addOption(OptionType.BOOLEAN, "opt-in", "set True to re-consent (opt-in), False to opt-out", true)
            ,
            new SubcommandData("schedule", "change auto-archive cycle hours (start at 0:00JST)")
                    .addOption(OptionType.INTEGER, "cycle", "execute every N hours (1-23), starting at 0:00, if 0 then only midnight", false)
            ,
            new SubcommandData("help", "send you about this bots command help")
                    .addOption(OptionType.BOOLEAN, "version", "show bot version", false)
    );

    private final DiscordBotUtils bot;
    private final RoleRunner roleRunner;
    private final AnonymousSettingRunner anonymousSettingRunner;
    private final MeRunner meRunner;
    private final HelpRunner helpRunner;
    private final RunArchiveRunner runArchiveRunner;
    private final ArchiveConfigRunner archiveConfigRunner;
    private final AutoArchiveScheduleRunner autoArchiveScheduleRunner;
    private final OptoutRunner optoutRunner;

    @Autowired
    public DiscordBotCommandListener(DiscordBotUtils bot,
                                     HelpRunner help, MeRunner me, RoleRunner role,
                                     AnonymousSettingRunner anon, RunArchiveRunner run,
                                     ArchiveConfigRunner archive, AutoArchiveScheduleRunner schedule,
                                     OptoutRunner optoutRunner){
        this.bot = bot;
        this.roleRunner = role;
        this.anonymousSettingRunner = anon;
        this.meRunner = me;
        this.helpRunner = help;
        this.runArchiveRunner = run;
        this.archiveConfigRunner = archive;
        this.autoArchiveScheduleRunner = schedule;
        this.optoutRunner = optoutRunner;
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

        IRunner runner = getRunnerBySub(sub);
        if (runner != null) {
            event.deferReply(runner.shouldDeferEphemeral()).queue();
        } else {
            event.deferReply().queue();
        }
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
                case "schedule" -> runSchedule(event);
                case "optout" -> runOptout(event);
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
            event.getHook()
                    .editOriginal("you do NOT have required permission(role) to do this")
                    .queue();
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
        archiveConfigRunner.run(event.getGuild(), event.getOptions());
        event.getHook().sendMessage(archiveConfigRunner.afterRunMessage()).queue();
    }
    
    private void runRun(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        if(isAcceptedChannel(event.getGuildChannel()) == false) {
            return;
        }
        runArchiveRunner.run(new GuildId(event.getGuild()), event.getOptions());
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
    
    private void runOptout(SlashCommandInteractionEvent event){
        // No admin permission required for personal opt-out
        optoutRunner.run(event.getMember(), event.getOptions());
        event.getHook()
                .sendMessage(optoutRunner.afterRunMessage())
                .setEphemeral(optoutRunner.shouldDeferEphemeral())
                .queue();
    }
    
    private void runAnonymous(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        anonymousSettingRunner.run(event.getGuild(), event.getOptions());
        event.getHook().sendMessage(anonymousSettingRunner.afterRunMessage()).queue();
    }
    
    private void runHelp(SlashCommandInteractionEvent event){
        // do not need to check admin for help
        var opt = event.getOption("version");
        boolean showVersion = (opt != null) && opt.getAsBoolean();
        // delegate main processing to HelpRunner
        helpRunner.run(event.getMember(), bot.isD2hAdmin(event.getMember()), showVersion);
        event.getHook()
                .sendMessage(helpRunner.afterRunMessage())
                .setEphemeral(helpRunner.shouldDeferEphemeral())
                .queue();
    }

    private void runSchedule(SlashCommandInteractionEvent event){
        if(hasAdminPermission(event) == false) {
            return;
        }
        if(isAcceptedChannel(event.getGuildChannel()) == false) {
            return;
        }
        autoArchiveScheduleRunner.run(event.getGuild(), event.getOptions());
        event.getHook().sendMessage(autoArchiveScheduleRunner.afterRunMessage()).queue();
    }
    
    private IRunner getRunnerBySub(String sub) {
        return switch (sub) {
            case "archive" -> archiveConfigRunner;
            case "run" -> runArchiveRunner;
            case "role" -> roleRunner;
            case "anonymous" -> anonymousSettingRunner;
            case "me" -> meRunner;
            case "help" -> helpRunner;
            case "schedule" -> autoArchiveScheduleRunner;
            case "optout" -> optoutRunner;
            default -> null;
        };
    }

}
