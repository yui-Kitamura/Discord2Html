package pro.eng.yui.oss.d2h.botIF.i;

public interface MessageKeys {

    MessageKey COMMON_RAW = new MessageKey("{0}");
    
    MessageKey COMMON_ERROR_GUILD_CHANNEL_ONLY = new MessageKey("common.error.guild_channel_only");
    MessageKey COMMON_ERROR_INVALID_CHANNEL = new MessageKey("common.error.invalid_channel");
    MessageKey COMMON_ERROR_NOT_SUPPORTED = new MessageKey("common.error.not_supported");
    MessageKey COMMON_ERROR_MISSING_SUBCOMMAND = new MessageKey("common.error.missing_subcommand");
    MessageKey COMMON_ERROR_RUNNER_NOT_FOUND = new MessageKey("common.error.runner_not_found");
    MessageKey COMMON_ERROR_NO_PERMISSION = new MessageKey("common.error.no_permission");
    MessageKey COMMON_ERROR_UNKNOWN_SUBCOMMAND = new MessageKey("common.error.unknown_subcommand");
    MessageKey COMMON_ERROR_INTERNAL_SERVER_ERROR = new MessageKey("common.error.internal_server_error");

    MessageKey RUNNER_ME_SUCCESS = new MessageKey("runner.me.success");
    MessageKey RUNNER_HELP_DM_SENT = new MessageKey("runner.help.dm_sent");
    MessageKey RUNNER_ANONYMOUS_SUCCESS = new MessageKey("runner.anonymous.success");
    MessageKey RUNNER_ARCHIVE_CONFIG_SUCCESS = new MessageKey("runner.archive_config.success");
    MessageKey RUNNER_AUTO_ARCHIVE_SUCCESS = new MessageKey("runner.auto_archive.success");
    MessageKey RUNNER_AUTO_ARCHIVE_CURRENT = new MessageKey("runner.auto_archive.current");
    MessageKey RUNNER_AUTO_ARCHIVE_NEW = new MessageKey("runner.auto_archive.new");
    MessageKey RUNNER_OPTOUT_SUCCESS = new MessageKey("runner.optout.success");
    MessageKey RUNNER_OPTOUT_ERROR_OPTIN_REQUIRED = new MessageKey("runner.optout.error.optin_required");
    MessageKey RUNNER_OPTOUT_SCOPE_GUILD = new MessageKey("runner.optout.scope.guild");
    MessageKey RUNNER_OPTOUT_SCOPE_CHANNEL = new MessageKey("runner.optout.scope.channel");
    MessageKey RUNNER_OPTOUT_OPTIN_SUCCESS = new MessageKey("runner.optout.optin.success");
    MessageKey RUNNER_OPTOUT_OPTIN_NOTE = new MessageKey("runner.optout.optin.note");
    MessageKey RUNNER_OPTOUT_OPTOUT_SUCCESS = new MessageKey("runner.optout.optout.success");
    MessageKey RUNNER_ROLE_SUCCESS = new MessageKey("runner.role.success");
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS = new MessageKey("runner.run_archive.success");
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS_PUSH = new MessageKey("runner.run_archive.success.push");
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS_LOCAL = new MessageKey("runner.run_archive.success.local");
}
