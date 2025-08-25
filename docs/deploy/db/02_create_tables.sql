CREATE TABLE guilds
(
    guild_id    BIGINT UNSIGNED PRIMARY KEY,
    guild_name  VARCHAR(255) NOT NULL,
    joined_time TIMESTAMP,
    anon_cycle  INT DEFAULT 12 NOT NULL,
    last_anon_changed TIMESTAMP,
    runs_on     INT DEFAULT 12 NOT NULL,
    on_run_message VARCHAR(10) DEFAULT 'on' NOT NULL,
    on_run_url VARCHAR(10) DEFAULT 'deny' NOT NULL
);

CREATE TABLE channels
(
    channel_id   BIGINT UNSIGNED PRIMARY KEY,
    guild_id     BIGINT UNSIGNED NOT NULL,
    channel_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (guild_id) REFERENCES guilds(guild_id)
);

CREATE TABLE channel_log
(
    channel_id    BIGINT,
    time_stamp    TIMESTAMP,
    is_changed    BOOLEAN,
    status        VARCHAR(50)
);

CREATE TABLE users (
    user_id     BIGINT UNSIGNED,
    guild_id    BIGINT UNSIGNED,
    user_name   VARCHAR(255) NOT NULL,
    nickname    VARCHAR(255),
    avatar      VARCHAR(255),
    anon_stats  VARCHAR(10),
    PRIMARY KEY (user_id, guild_id)
);

CREATE TABLE roles (
    role_id      BIGINT UNSIGNED PRIMARY KEY,
    guild_id     BIGINT UNSIGNED NOT NULL,
    anon_stats   VARCHAR(10),
    FOREIGN KEY (guild_id) REFERENCES guilds(guild_id)
);

CREATE TABLE user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

CREATE TABLE discord_oauth_token (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    token_type VARCHAR(32),
    scope TEXT,
    expires_at DATETIME,
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
