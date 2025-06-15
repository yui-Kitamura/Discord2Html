-- スキーマの作成
CREATE SCHEMA IF NOT EXISTS discord2html DEFAULT CHARACTER SET utf8mb4;

-- データベースユーザの作成（パスワードはプレースホルダ）
CREATE USER IF NOT EXISTS 'discord2html_admin'@'localhost' 
    IDENTIFIED BY '{{YOUR_PASSWORD_HERE}}';

-- 作成したユーザに権限を付与
GRANT ALL PRIVILEGES ON discord2html.* TO 'discord2html_admin'@'localhost';
FLUSH PRIVILEGES;