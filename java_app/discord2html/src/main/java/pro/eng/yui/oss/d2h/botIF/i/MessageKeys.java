package pro.eng.yui.oss.d2h.botIF.i;

public interface MessageKeys {

    /** {0} */
    MessageKey COMMON_RAW = new MessageKey("common.raw");

    /** このチャンネルはアーカイブ対象です。処理を開始します >>> */
    MessageKey COMMON_INFO_ARCHIVE_TARGET_START = new MessageKey("common.info.archive_target_start");
    /** アーカイブが作成されました。処理を終了します <<<\n{0} */
    MessageKey COMMON_INFO_ARCHIVE_CREATED_END = new MessageKey("common.info.archive_created_end");
    /** コマンドはサーバーチャンネルでのみ有効です */
    MessageKey COMMON_ERROR_GUILD_CHANNEL_ONLY = new MessageKey("common.error.guild_channel_only");
    /** このチャンネルではコマンドを使用できません */
    MessageKey COMMON_ERROR_INVALID_CHANNEL = new MessageKey("common.error.invalid_channel");
    /** D2Hボットは呼び出されましたが、サポートされていません */
    MessageKey COMMON_ERROR_NOT_SUPPORTED = new MessageKey("common.error.not_supported");
    /** サブコマンドが不足しています。`/d2h help` をご覧ください */
    MessageKey COMMON_ERROR_MISSING_SUBCOMMAND = new MessageKey("common.error.missing_subcommand");
    /** コマンド実行クラスが見つかりません。入力を確認してください。`/d2h help` */
    MessageKey COMMON_ERROR_RUNNER_NOT_FOUND = new MessageKey("common.error.runner_not_found");
    /** この操作を行うための権限（ロール）がありません */
    MessageKey COMMON_ERROR_NO_PERMISSION = new MessageKey("common.error.no_permission");
    /** 不明なサブコマンドです。`/d2h help` をご覧ください */
    MessageKey COMMON_ERROR_UNKNOWN_SUBCOMMAND = new MessageKey("common.error.unknown_subcommand");
    /** botサーバーでエラーが発生しました。 >> `{0}` */
    MessageKey COMMON_ERROR_INTERNAL_SERVER_ERROR = new MessageKey("common.error.internal_server_error");

    /** 設定が正常に更新されました */
    MessageKey RUNNER_ME_SUCCESS = new MessageKey("runner.me.success");
    /** botバージョン： {0}\nGitHub： {1} */
    MessageKey RUNNER_HELP_VERSION_INFO = new MessageKey("runner.help.version_info");
    /** アーカイブ運用ポリシー(TOS)： {0} */
    MessageKey RUNNER_HELP_TOS_INFO = new MessageKey("runner.help.tos_info");
    /** DMにヘルプガイドを送信しました */
    MessageKey RUNNER_HELP_DM_SENT = new MessageKey("runner.help.dm_sent");
    /** 匿名化のサーバー設定が正常に更新されました */
    MessageKey RUNNER_ANONYMOUS_SUCCESS = new MessageKey("runner.anonymous.success");
    /** アーカイブ設定が正常に更新されました */
    MessageKey RUNNER_ARCHIVE_CONFIG_SUCCESS = new MessageKey("runner.archive_config.success");
    /** アーカイブ対象： {0} */
    MessageKey RUNNER_ARCHIVE_CONFIG_TARGETS = new MessageKey("runner.archive_config.targets");
    /** アーカイブ対象： （なし） */
    MessageKey RUNNER_ARCHIVE_CONFIG_NONE = new MessageKey("runner.archive_config.none");
    /** アーカイブ対象： （サーバーが見つかりません） */
    MessageKey RUNNER_ARCHIVE_CONFIG_GUILD_UNRESOLVED = new MessageKey("runner.archive_config.guild_unresolved");
    /** 自動アーカイブスケジュールが正常に更新されました */
    MessageKey RUNNER_AUTO_ARCHIVE_SUCCESS = new MessageKey("runner.auto_archive.success");
    /** サーバー設定が正常に更新されました */
    MessageKey RUNNER_GUILD_SUCCESS = new MessageKey("runner.guild.success");
    /** 現在の実行予定時刻： {0} */
    MessageKey RUNNER_AUTO_ARCHIVE_CURRENT = new MessageKey("runner.auto_archive.current");
    /** 新しい実行予定時刻： {0} */
    MessageKey RUNNER_AUTO_ARCHIVE_NEW = new MessageKey("runner.auto_archive.new");
    /** オプトアウト設定が正常に更新されました */
    MessageKey RUNNER_OPTOUT_SUCCESS = new MessageKey("runner.optout.success");
    /** オプトインオプション(True/False)が必要です */
    MessageKey RUNNER_OPTOUT_ERROR_OPTIN_REQUIRED = new MessageKey("runner.optout.error.optin_required");
    /** このサーバー全体（全チャンネル） */
    MessageKey RUNNER_OPTOUT_SCOPE_GUILD = new MessageKey("runner.optout.scope.guild");
    /** チャンネル：#{0} */
    MessageKey RUNNER_OPTOUT_SCOPE_CHANNEL = new MessageKey("runner.optout.scope.channel");
    /** {0} のオプトインを記録しました{1} */
    MessageKey RUNNER_OPTOUT_OPTIN_SUCCESS = new MessageKey("runner.optout.optin.success");
    /** \n【補足】：個別チャンネルのオプトアウト設定は残っています。そのチャンネルはアーカイブから除外されます。 */
    MessageKey RUNNER_OPTOUT_OPTIN_NOTE = new MessageKey("runner.optout.optin.note");
    /** {0} のオプトアウトを記録しました{1} */
    MessageKey RUNNER_OPTOUT_OPTOUT_SUCCESS = new MessageKey("runner.optout.optout.success");
    /** ロール設定が正常に更新されました */
    MessageKey RUNNER_ROLE_SUCCESS = new MessageKey("runner.role.success");
    /** アーカイブを作成しました。{0} */
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS = new MessageKey("runner.run_archive.success");
    /** GitHubへプッシュされます。 */
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS_PUSH = new MessageKey("runner.run_archive.success.push");
    /** （ローカル保存のみ）。 */
    MessageKey RUNNER_RUN_ARCHIVE_SUCCESS_LOCAL = new MessageKey("runner.run_archive.success.local");
    /** 一部のチャンネルでアーカイブに失敗しました。詳細：\n{0} */
    MessageKey RUNNER_RUN_ARCHIVE_FAIL_NOTES = new MessageKey("runner.run_archive.fail_notes");
}
