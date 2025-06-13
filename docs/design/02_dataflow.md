# 機能間データフロー

```mermaid
flowchart TD 

subgraph サーバ_Java
    BATCH[バッチ処理]
    SERVER[Javaプログラム]
    CONFIG[configファイル]
end

subgraph Discord
    BOT[Discord ボット]
end

subgraph GitHub
    REPO[GitHubリポジトリ<br/>Pages対象]
    PAGE[GitHub Pages<br/>静的HTML公開]
end

%% データフロー
BATCH -- 実行リクエスト --> SERVER
SERVER -- 実行指示/設定提供 --> BOT
BOT -- 収集データ返却 --> SERVER

CONFIG <-- 設定参照/反映 --> SERVER


SERVER -- ファイル生成・Push --> REPO
REPO -- Pages自動公開※GitHubActions --> PAGE

BATCH -- 設定参照 --> CONFIG
```

1. バッチ処理がJavaプログラムに対して実行リクエストを送る。
2. Javaプログラムは、必要に応じてDiscordボットへ実行指示や設定情報を提供する。
3. Discordボットがデータ収集などを行い、その結果をJavaプログラムへ返却する。
4. Javaプログラムはconfigファイルを参照・反映しながら各種処理を動作させる。
5. Javaプログラムは取得・処理したデータをファイルとして生成し、GitHubリポジトリへpushする。
6. GitHubリポジトリは、pushされたファイルを元にGitHub Actionsを用いてPagesを自動公開する。
7. GitHub Pagesは公開された静的HTMLとしてWebで情報提供を行う。
