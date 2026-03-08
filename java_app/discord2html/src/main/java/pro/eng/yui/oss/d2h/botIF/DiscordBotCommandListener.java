package pro.eng.yui.oss.d2h.botIF;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.botIF.i.MessageKeys;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;
import pro.eng.yui.oss.d2h.botIF.runner.*;
import pro.eng.yui.oss.d2h.db.field.GuildId;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
                    .addOption(OptionType.BOOLEAN, "opt-in", "set True to re-consent (opt-in), False to opt-out", true)
                    .addOption(OptionType.CHANNEL, "channel", "target channel (omit for guild-wide)", false)
            ,
            new SubcommandData("schedule", "change auto-archive cycle hours (start at 0:00JST)")
                    .addOption(OptionType.INTEGER, "cycle", "execute every N hours (1-23), starting at 0:00, if 0 then only midnight", false)
            ,
            new SubcommandData("guild", "change guild settings")
                    .addOptions(new OptionData(OptionType.STRING, "lang", "change bot language", false)
                            .addChoice("ja-JP", "ja-JP")
                            .addChoice("en-US", "en-US")
                    )
            ,
            new SubcommandData("help", "send you about this bots command help")
                    .addOption(OptionType.BOOLEAN, "version", "show bot version", false)
                    .addOption(OptionType.BOOLEAN, "tos", "show archive policy (TOS) link", false)
    );

    private final DiscordBotUtils botUtils;
    private final RoleRunner roleRunner;
    private final AnonymousSettingRunner anonymousSettingRunner;
    private final MeRunner meRunner;
    private final HelpRunner helpRunner;
    private final RunArchiveRunner runArchiveRunner;
    private final ArchiveConfigRunner archiveConfigRunner;
    private final AutoArchiveScheduleRunner autoArchiveScheduleRunner;
    private final OptoutRunner optoutRunner;
    private final GuildSettingRunner guildSettingRunner;

    @Autowired
    public DiscordBotCommandListener(DiscordBotUtils botUtils,
                                     HelpRunner help, MeRunner me, RoleRunner role,
                                     AnonymousSettingRunner anon, RunArchiveRunner run,
                                     ArchiveConfigRunner archive, AutoArchiveScheduleRunner schedule,
                                     OptoutRunner optoutRunner, GuildSettingRunner guildSettingRunner){
        this.botUtils = botUtils;
        this.roleRunner = role;
        this.anonymousSettingRunner = anon;
        this.meRunner = me;
        this.helpRunner = help;
        this.runArchiveRunner = run;
        this.archiveConfigRunner = archive;
        this.autoArchiveScheduleRunner = schedule;
        this.optoutRunner = optoutRunner;
        this.guildSettingRunner = guildSettingRunner;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        final String command = event.getName();
        final String sub = event.getSubcommandName();
        final Locale locale = botUtils.getLocale(event.getGuild());
        
        if((event.getChannel() instanceof GuildChannel) == false) {
            event.replyEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.WARN, MessageKeys.COMMON_ERROR_GUILD_CHANNEL_ONLY), locale)).queue();
            return;
        }
        if (isAcceptedChannel(event.getGuildChannel()) == false) {
            event.replyEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.ERROR, MessageKeys.COMMON_ERROR_INVALID_CHANNEL), locale))
                    .setEphemeral(true) //visible=false
                    .queue();
            return;
        }
        
        if(commands.contains(command) == false) {
            event.replyEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.ERROR, MessageKeys.COMMON_ERROR_NOT_SUPPORTED), locale))
                    .setEphemeral(true)
                    .setSuppressedNotifications(true)
                    .queue();
            return;
        }
        if(sub == null) {
            event.replyEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.WARN, MessageKeys.COMMON_ERROR_MISSING_SUBCOMMAND), locale))
                    .setEphemeral(true)
                    .setSuppressedNotifications(true)
                    .queue();
            return;
        }

        IRunner runner = getRunnerBySub(sub);
        if (runner != null) {
            event.deferReply(runner.shouldDeferEphemeral()).queue();
        } else {
            event.deferReply(true).queue();
            event.getHook()
                    .editOriginalEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.WARN, MessageKeys.COMMON_ERROR_RUNNER_NOT_FOUND), locale))
                    .queue();
            return;
        }
        
        // 権限チェック
        if(hasPermission(event, runner) == false) {
            event.getHook()
                    .editOriginalEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.ERROR, MessageKeys.COMMON_ERROR_NO_PERMISSION), locale))
                    .queue();
            return;
        }
        
        // 処理実行
        try {
            botUtils.upsertGuildInfoToDB(event.getGuild());
            botUtils.upsertGuildChannelToDB(event.getGuild());

            switch (sub) {
                case "archive" -> runArchive(event);
                case "run" -> runRun(event);
                case "role" -> runRole(event);
                case "anonymous" -> runAnonymous(event);
                case "me" -> runMe(event);
                case "help" -> runHelp(event);
                case "schedule" -> runSchedule(event);
                case "optout" -> runOptout(event);
                case "guild" -> runGuild(event);
                default -> {
                    event.getHook()
                            .editOriginalEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.ERROR, MessageKeys.COMMON_ERROR_UNKNOWN_SUBCOMMAND), locale))
                            .queue();
                    return;
                }
            }

            event.getHook()
                    .editOriginalEmbeds(botUtils.buildStatusEmbed(runner.afterRunMessage(), locale))
                    .queue();
            
        }catch(Exception unexpected) {
            event.getHook()
                .editOriginalEmbeds(botUtils.buildStatusEmbed(new MessageSeed(IRunner.ERROR, MessageKeys.COMMON_ERROR_INTERNAL_SERVER_ERROR, unexpected.getMessage()), locale))
                .queue();
            return;
        }
    }
    
    /** コマンド実行チャンネルの確認 */
    protected boolean isAcceptedChannel(GuildChannel channel){
        return botUtils.getAdminTaggedChannelList(channel.getGuild()).contains(channel);
    }
    protected boolean hasPermission(@NotNull SlashCommandInteractionEvent event, @NotNull IRunner runner){
        IRunner.RequiredPermissionType required = runner.requiredPermissionType(event.getOptions());
        switch (required) {
            case DENY -> {
                return false;
            }
            case ANY -> {
                return true;
            }
            case D2H_ADMIN -> {
                return botUtils.isD2hAdmin(event.getMember());
            }
            case SERVER_ADMIN -> {
                try{
                    for(Role r : event.getMember().getRoles()) {
                        if (r.hasPermission(Permission.ADMINISTRATOR)) {
                            return true;
                        }
                    }
                    return false; 
                }catch(NullPointerException e){ return false; }
            }
        }
        return false;
    }
    
    private void runArchive(SlashCommandInteractionEvent event){
        archiveConfigRunner.run(event.getGuild(), event.getOptions());
    }
    
    private void runRun(SlashCommandInteractionEvent event){
        runArchiveRunner.run(new GuildId(event.getGuild()), event.getOptions());
    }
    
    private void runRole(SlashCommandInteractionEvent event){
        roleRunner.run(event.getOptions());
    }
    
    private void runMe(SlashCommandInteractionEvent event){
        meRunner.run(event.getMember(), event.getOptions());
    }
    
    private void runOptout(SlashCommandInteractionEvent event){
        optoutRunner.run(event.getMember(), event.getOptions());
    }
    /** 匿名周期の変更 */
    private void runAnonymous(SlashCommandInteractionEvent event){
        anonymousSettingRunner.run(event.getGuild(), event.getOptions());
    }
    
    private void runHelp(SlashCommandInteractionEvent event){
        helpRunner.run(event.getMember(), event.getOptions());
    }

    private void runSchedule(SlashCommandInteractionEvent event){
        autoArchiveScheduleRunner.run(event.getGuild(), event.getOptions());
    }

    private void runGuild(SlashCommandInteractionEvent event){
        guildSettingRunner.run(event.getGuild(), event.getOptions());
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
            case "guild" -> guildSettingRunner;
            default -> null;
        };
    }

}
