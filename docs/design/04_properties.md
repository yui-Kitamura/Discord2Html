# システム可変値

Discord2Htmlシステムは.propertiesファイルを使用して設定を管理します。以下の設定項目が利用可能です：

## Discord Bot設定

- discord.bot.token: Discord BotのTokenを指定します。Discord Developer Portalで取得したトークンを設定してください。

## サーバー設定

- discord.server.id: 監視対象のDiscordサーバーIDを指定します。
- discord.channel.root: 設定コマンドを使えるチャンネルIDを指定します。

## スケジュール設定

- schedule.interval: 実行間隔を分単位で指定します。デフォルト値は60分です。
- schedule.initial.delay: バッチ待機開始後、初回実行までの待機時間を分単位で指定します。デフォルト値は0分です。
