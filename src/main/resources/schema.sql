-- H2 内存库建表脚本（仅嵌入式数据源自动执行；MySQL 老库已有同结构表，无需执行）
-- 表结构字段名与老项目保持一致，兼容旧 MySQL 数据

CREATE TABLE IF NOT EXISTS uploaded_file (
    ID           VARCHAR(36) PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_path  VARCHAR(255) NOT NULL,
    content_type VARCHAR(127),
    size         BIGINT       NOT NULL,
    sha256       VARCHAR(64)  NOT NULL,
    uploaded_at  BIGINT,
    CONSTRAINT uq_uploaded_file_sha256 UNIQUE (sha256)
);

-- ============ IM 好友/私聊（2026 重构新增，MySQL 老库需手工执行同结构 DDL） ============
-- 老库若已按旧版建过下表，用 ALTER 补列：
--   ALTER TABLE friend ADD COLUMN muted TINYINT DEFAULT 0, ADD COLUMN last_seen_at BIGINT,
--     ADD COLUMN tag VARCHAR(32) DEFAULT '', ADD COLUMN blocked TINYINT DEFAULT 0;
--   ALTER TABLE private_message ADD COLUMN reply_to_id VARCHAR(36),
--     ADD COLUMN read_flag TINYINT DEFAULT 0, ADD COLUMN edited TINYINT DEFAULT 0;
--   ALTER TABLE friend_request ADD COLUMN message VARCHAR(100);
--   ALTER TABLE user_profile ADD COLUMN presence_status VARCHAR(16) DEFAULT 'online';

-- ============ 账号体系（手机号注册） ============
-- 只有 app_user 里有记录的账号才是合法用户；用户名与手机号均唯一。
-- 老库升级：
--   CREATE TABLE app_user (...);  -- 同下方结构
--   之后为存量用户名补手机号与初始密码（password_hash 由应用生成）

CREATE TABLE IF NOT EXISTS app_user (
    ID              VARCHAR(36) PRIMARY KEY,
    username        VARCHAR(64) NOT NULL,       -- 登录名（小写字母/数字/下划线）
    phone           VARCHAR(20) NOT NULL,       -- 手机号（注册凭据）
    nickname        VARCHAR(32),
    password_hash   VARCHAR(255) NOT NULL,      -- pbkdf2$迭代次数$salt$hash
    avatar          VARCHAR(16),
    signature       VARCHAR(100),
    presence_status VARCHAR(16) DEFAULT 'online',
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    created         BIGINT NOT NULL,
    last_login_at   BIGINT,
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT uq_app_user_phone UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS friend (
    ID              VARCHAR(36) PRIMARY KEY,
    owner_username  VARCHAR(64) NOT NULL,
    friend_username VARCHAR(64) NOT NULL,
    remark          VARCHAR(64),
    tag             VARCHAR(32) DEFAULT '',
    pinned          TINYINT DEFAULT 0,
    muted           TINYINT DEFAULT 0,
    blocked         TINYINT DEFAULT 0,
    last_read_at    BIGINT DEFAULT 0,
    last_seen_at    BIGINT,
    created         BIGINT NOT NULL,
    CONSTRAINT uq_friend_pair UNIQUE (owner_username, friend_username)
);

CREATE TABLE IF NOT EXISTS friend_request (
    ID         VARCHAR(36) PRIMARY KEY,
    from_user  VARCHAR(64) NOT NULL,
    to_user    VARCHAR(64) NOT NULL,
    message    VARCHAR(100),
    status     VARCHAR(16) NOT NULL,     -- PENDING / ACCEPTED / REJECTED
    created    BIGINT NOT NULL,
    updated_at BIGINT
);

CREATE TABLE IF NOT EXISTS private_message (
    ID        VARCHAR(36) PRIMARY KEY,
    from_user VARCHAR(64) NOT NULL,
    to_user   VARCHAR(64) NOT NULL,
    content   VARCHAR(2000) NOT NULL,
    msg_type  VARCHAR(16) NOT NULL,      -- text / image / poke / system / card / location
    status    VARCHAR(16) NOT NULL,      -- SENT / RECALLED
    reply_to_id VARCHAR(36),
    read_flag TINYINT DEFAULT 0,         -- 对方是否已读（已读回执）
    edited    TINYINT DEFAULT 0,         -- 发送后是否编辑过
    created   BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profile (
    username        VARCHAR(64) PRIMARY KEY,
    nickname        VARCHAR(32),
    signature       VARCHAR(100),
    avatar          VARCHAR(16),
    presence_status VARCHAR(16) DEFAULT 'online',  -- online / busy / away
    updated_at      BIGINT
);

-- 消息表情回应（每条消息每个用户每个表情一行，toggle 语义）
CREATE TABLE IF NOT EXISTS message_reaction (
    ID       VARCHAR(36) PRIMARY KEY,
    msg_id   VARCHAR(36) NOT NULL,
    username VARCHAR(64) NOT NULL,
    emoji    VARCHAR(8) NOT NULL,
    created  BIGINT NOT NULL,
    CONSTRAINT uq_reaction UNIQUE (msg_id, username, emoji)
);

-- 收藏的消息（个人视角，跨会话）
CREATE TABLE IF NOT EXISTS message_star (
    ID       VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    msg_id   VARCHAR(36) NOT NULL,
    created  BIGINT NOT NULL,
    CONSTRAINT uq_star UNIQUE (username, msg_id)
);
