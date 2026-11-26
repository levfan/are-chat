-- 0.创建数据库
CREATE DATABASE IF NOT EXISTS `are_chat` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 1. 确保用户存在（如果不存在则创建，防止环境变量没生效的极端情况，但通常不需要）
-- 注意：这里用 IDENTIFIED BY 可能会重置密码，如果不想重置密码，只保留下面的 GRANT 即可
CREATE USER IF NOT EXISTS 'are_chat'@'%' IDENTIFIED BY 'Ht@706706706';

-- 2. 赋予 Root 级别的所有权限 (核心)
GRANT ALL PRIVILEGES ON *.* TO 'are_chat'@'%' WITH GRANT OPTION;

-- 3. 刷新
FLUSH PRIVILEGES;


-- 1.创建只能查询的用户
CREATE USER IF NOT EXISTS 'data_query_user'@'%' IDENTIFIED BY 'data_query_user';

-- 2.只授予SELECT权限（查询权限）
GRANT SELECT ON are_chat.* TO 'data_query_user'@'%';

-- 3.如果需要限制到特定表，可以这样：
-- GRANT SELECT ON archives.your_table_name TO 'data_query_user'@'%';

-- 4. 刷新
FLUSH PRIVILEGES;