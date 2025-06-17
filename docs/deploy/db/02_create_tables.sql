CREATE TABLE channels (
    channel_id    BIGINT PRIMARY KEY,
    channel_name  VARCHAR(255) NOT NULL,
    status        VARCHAR(50),
    last_recorded DATETIME
);

CREATE TABLE users (
    user_id     BIGINT PRIMARY KEY,
    user_name   VARCHAR(255) NOT NULL,
    nickname    VARCHAR(255),
    avatar      VARCHAR(255)
);

CREATE TABLE roles (
    role_id      BIGINT PRIMARY KEY,
    admin_flg    BOOLEAN DEFAULT FALSE,
    anon_flg     BOOLEAN DEFAULT TRUE
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
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
