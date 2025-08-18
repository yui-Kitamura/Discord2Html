# Discord2Html
Discord2Htmlは、Discordサーバーの会話内容を定期的にHTMLファイルとして保存・公開するシステムです。
これにより、Discordコミュニティの会話を一般公開可能な形で保存し、オープンなコミュニティ構築を支援します。

## 運用制限
1botにつき1repo用意が必要です

## fork and develop
新規bot開発に際しては、ディレクトリ`gh_pages`の内容を削除してください

## run
- docs/deployに基づきDB（mariadb）を構築してください
- java_app配下resources/secret.properties をGitHubActionで生成します。ActionSecrets値をrepoに登録してください
- DiscordBotは各自登録してください