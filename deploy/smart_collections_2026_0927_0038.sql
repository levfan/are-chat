/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19  Distrib 10.11.16-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: smart_collections
-- ------------------------------------------------------
-- Server version	10.11.16-MariaDB-ubu2204-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_audit`
--

DROP TABLE IF EXISTS `admin_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_audit` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `actor` varchar(64) NOT NULL COMMENT '操作人用户名（管理员）',
  `action` varchar(32) NOT NULL COMMENT '操作类型：APPROVE 通过注册 / REJECT 拒绝注册 / ENABLE 启用账号 / DISABLE 禁用账号 / RESET_PASSWORD 重置密码',
  `target` varchar(64) DEFAULT NULL COMMENT '操作对象，通常为目标用户名',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情说明',
  `created` bigint(20) NOT NULL COMMENT '操作时间（毫秒时间戳）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员操作审计表：审批/启用禁用/重置密码等敏感动作全部留痕';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_audit`
--

LOCK TABLES `admin_audit` WRITE;
/*!40000 ALTER TABLE `admin_audit` DISABLE KEYS */;
INSERT INTO `admin_audit` VALUES
('restore-audit-kaylazho-0001','admin','APPROVE','kaylazho','通过注册申请 restore-reg-kaylazho-000001',1790425217000),
('restore-audit-liufan-0001','admin','APPROVE','liufan','通过注册申请 restore-reg-liufan-000001',1790423861000);
/*!40000 ALTER TABLE `admin_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `content` varchar(500) NOT NULL COMMENT '公告内容',
  `created_by` varchar(64) NOT NULL COMMENT '发布人用户名（管理员）',
  `enabled` tinyint(4) DEFAULT 1 COMMENT '是否启用：1 启用（横幅展示）/ 0 停用',
  `created` bigint(20) NOT NULL COMMENT '发布时间（毫秒时间戳）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全站公告表：管理员发布，所有登录用户顶部横幅展示';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement`
--

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_read`
--

DROP TABLE IF EXISTS `announcement_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_read` (
  `username` varchar(50) DEFAULT NULL COMMENT '已读用户名（区分大小写）',
  `announcement_id` varchar(36) NOT NULL COMMENT '公告ID，关联 announcement.id',
  `read_at` bigint(20) NOT NULL COMMENT '确认已读时间（毫秒时间戳）',
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ann_read` (`username`,`announcement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表：用户点「我知道了」后不再展示该条公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_read`
--

LOCK TABLES `announcement_read` WRITE;
/*!40000 ALTER TABLE `announcement_read` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement_read` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `app_user`
--

DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名，全局唯一且区分大小写',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号，全局唯一，注册与登录标识',
  `nickname` varchar(32) DEFAULT NULL COMMENT '昵称（注册时默认取用户名）',
  `password_hash` varchar(255) DEFAULT NULL COMMENT '登录密码哈希（PBKDF2）',
  `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
  `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
  `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online/busy/away，注册时初始化，实时状态以 user_profile 为准',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE 正常 / DISABLED 禁用 / CLOSED 自助注销（保留占位，禁止登录、不可被搜索加好友）',
  `role` varchar(16) NOT NULL DEFAULT 'USER' COMMENT '角色：USER 普通用户 / ADMIN 管理员（可审批注册、管理用户与公告）',
  `created` bigint(20) NOT NULL COMMENT '注册时间（毫秒时间戳）',
  `last_login_at` bigint(20) DEFAULT NULL COMMENT '最近登录时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_app_user_username` (`username`),
  UNIQUE KEY `uq_app_user_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统合法用户表：注册申请审批通过后创建，用户名与手机号均唯一';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_user`
--

LOCK TABLES `app_user` WRITE;
/*!40000 ALTER TABLE `app_user` DISABLE KEYS */;
INSERT INTO `app_user` VALUES
('29912c20-5c59-46c9-9b31-9fd76395933f','liufan','13772019579','liufan','pbkdf2$60000$KjjiGdd0W9RHOhrD9NQOsg==$1TwqEOGMQVZmbYoKkmWMebRAWh/BIamRgjojJhuhxAY=',NULL,NULL,'online','ACTIVE','USER',1790425510784,1790434383223),
('c13efd57-f2bf-450d-a291-536b87dbac94','kaylazho','153****3818','kaylazho','pbkdf2$60000$eq5r29Sy02oze+UI/bnpGg==$S517Js2NXpFfsdgiMkwbZ1poDEoHlKTl00F58kqErE8=',NULL,NULL,'online','ACTIVE','USER',1790425510784,1790434384976),
('e36c3ecd-7592-426c-9262-110de62a742b','admin','00000000000','admin','pbkdf2$60000$s+HjVurgkWpVdDwRWAZ7MQ==$LU07Uyyp21/kC0Uy/7iF6UDRsLa6c/Hz+g2rktlIWW4=','c0','','online','ACTIVE','ADMIN',1790431371145,1790438386381);
/*!40000 ALTER TABLE `app_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `conversation_pin`
--

DROP TABLE IF EXISTS `conversation_pin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_pin` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `user_a` varchar(50) DEFAULT NULL COMMENT '会话双方用户名之一（字典序较小者）',
  `user_b` varchar(50) DEFAULT NULL COMMENT '会话双方用户名之一（字典序较大者）',
  `msg_id` varchar(36) NOT NULL COMMENT '被置顶的消息ID',
  `created_by` varchar(64) NOT NULL COMMENT '置顶操作人用户名',
  `created` bigint(20) NOT NULL COMMENT '置顶时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_conv_pin` (`user_a`,`user_b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话置顶消息表：每个双人会话最多一条置顶消息，双方共享可见';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `conversation_pin`
--

LOCK TABLES `conversation_pin` WRITE;
/*!40000 ALTER TABLE `conversation_pin` DISABLE KEYS */;
/*!40000 ALTER TABLE `conversation_pin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `friend`
--

DROP TABLE IF EXISTS `friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `owner_username` varchar(50) DEFAULT NULL COMMENT '拥有者用户名（关系归属方）',
  `friend_username` varchar(50) DEFAULT NULL COMMENT '好友用户名',
  `remark` varchar(64) DEFAULT NULL COMMENT '好友备注名',
  `tag` varchar(32) DEFAULT '' COMMENT '好友分组标签',
  `pinned` tinyint(4) DEFAULT 0 COMMENT '是否置顶联系人：1 置顶 / 0 否',
  `muted` tinyint(4) DEFAULT 0 COMMENT '是否消息免打扰：1 开启 / 0 关闭',
  `blocked` tinyint(4) DEFAULT 0 COMMENT '是否拉黑：1 已拉黑 / 0 未拉黑',
  `last_read_at` bigint(20) DEFAULT 0 COMMENT '最近已读时间（毫秒时间戳，用于未读红点计算）',
  `last_seen_at` bigint(20) DEFAULT NULL COMMENT '最近在线时间（毫秒时间戳）',
  `created` bigint(20) NOT NULL COMMENT '成为好友时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_friend_pair` (`owner_username`,`friend_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友关系表：双向各存一行，owner 为拥有者视角';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `friend`
--

LOCK TABLES `friend` WRITE;
/*!40000 ALTER TABLE `friend` DISABLE KEYS */;
INSERT INTO `friend` VALUES
('35ad8a1c-2d48-428e-a00f-ae55e6cf2984','liufan','kaylazho',NULL,'',0,0,0,1790430985308,NULL,1790425510784),
('c7529f83-2b50-4d3f-896a-5c5ab61dbf8c','kaylazho','liufan',NULL,'',0,0,0,1790430026646,NULL,1790425510784);
/*!40000 ALTER TABLE `friend` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `friend_request`
--

DROP TABLE IF EXISTS `friend_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_request` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `from_user` varchar(50) DEFAULT NULL COMMENT '发起方用户名（区分大小写）',
  `to_user` varchar(50) DEFAULT NULL COMMENT '接收方用户名（区分大小写）',
  `message` varchar(100) DEFAULT NULL COMMENT '申请留言',
  `status` varchar(16) NOT NULL COMMENT '申请状态：PENDING 待处理 / ACCEPTED 已接受 / REJECTED 已拒绝',
  `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
  `updated_at` bigint(20) DEFAULT NULL COMMENT '最近处理时间（毫秒时间戳）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='好友申请表：对方同意后双方各建一条 friend 关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `friend_request`
--

LOCK TABLES `friend_request` WRITE;
/*!40000 ALTER TABLE `friend_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `friend_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message_reaction`
--

DROP TABLE IF EXISTS `message_reaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_reaction` (
  `id` varchar(36) NOT NULL,
  `msg_id` varchar(36) NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `emoji` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `created` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reaction` (`msg_id`,`username`,`emoji`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message_reaction`
--

LOCK TABLES `message_reaction` WRITE;
/*!40000 ALTER TABLE `message_reaction` DISABLE KEYS */;
INSERT INTO `message_reaction` VALUES
('3a5ee89a-bcdd-4253-9bce-75923ef9638e','169d0486-4b80-4b45-9e0d-5752caab32e0','kaylazho','😂',1790427668616),
('4bb994e2-8843-4153-b67f-3faadc570b1e','76667cfc-9ec2-400b-8ea8-7a2dbe9d98db','kaylazho','🔥',1790426957786),
('5b2c0e9e-3652-460a-8b59-282eadcd6548','349882ca-b426-4a59-bf53-e466b9c4555a','liufan','👍',1790427008715),
('808c99e6-cf6f-41c0-9df5-126e4220e279','169d0486-4b80-4b45-9e0d-5752caab32e0','kaylazho','👍',1790427668616),
('853fb9a7-7b58-4abc-a47e-d9723fd3986c','d11291dd-c568-43e1-b324-ba7ac28547ae','kaylazho','😢',1790429558158),
('8702be4d-7ff3-497a-9d74-3b603895ba01','63ffe72c-a41c-462d-8e0f-167674c51c5e','liufan','😢',1790426666968),
('9cea323a-9938-4944-8879-ddd7ad7a52ae','42ad26db-cfc3-4566-aa15-79b9c7992bcf','kaylazho','👍',1790427059644),
('a3bd5390-eff7-4820-82c1-e2fed903fc1f','29d7354d-2e6f-4974-a836-873af15a6e60','kaylazho','👍',1790427131186),
('b787332b-4a67-4a0a-bf15-1387951b9d78','8348e385-7163-4809-9f87-b4d41571d3bb','liufan','👍',1790427510934),
('c38b2b21-c59c-4fc5-9d2c-ecbc706f88c3','5ace757e-ac91-4ca0-8f79-0ee1f898a0a6','liufan','😂',1790427272343),
('c6fb6fcd-cb0b-46ab-b9de-afe290015492','f55ded19-3d15-4059-b000-0831c5047820','liufan','😢',1790428584082),
('d0f1231a-ebfd-4cad-8324-b9c484c80256','ab3b75ac-59cb-46fa-bfcb-6252dad19625','liufan','😢',1790426778842),
('d90a6a5f-0702-45a1-a8ed-9640a7faea7b','29d7354d-2e6f-4974-a836-873af15a6e60','liufan','👍',1790427131186),
('f8e4fb42-5e5f-4549-be6f-66e677337db1','d82d8829-25bc-4895-bb9c-05e88930ad6e','liufan','👍',1790426762995),
('ff4ad846-e5fb-4a37-a687-d578f1791e89','7b2b422c-e45d-4a86-8675-7b13287b4c35','liufan','❤️',1790426288923);
/*!40000 ALTER TABLE `message_reaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `message_star`
--

DROP TABLE IF EXISTS `message_star`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_star` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `username` varchar(50) DEFAULT NULL COMMENT '收藏人用户名（区分大小写）',
  `msg_id` varchar(36) NOT NULL COMMENT '收藏的消息ID',
  `created` bigint(20) NOT NULL COMMENT '收藏时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_star` (`username`,`msg_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='消息收藏表：个人视角，跨会话收藏消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message_star`
--

LOCK TABLES `message_star` WRITE;
/*!40000 ALTER TABLE `message_star` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_star` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `private_message`
--

DROP TABLE IF EXISTS `private_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_message` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID（图片/文件消息的下载地址为 /api/files/{id}/download）',
  `from_user` varchar(50) DEFAULT NULL COMMENT '发送方用户名（区分大小写）',
  `to_user` varchar(50) DEFAULT NULL COMMENT '接收方用户名（区分大小写）',
  `content` varchar(2000) NOT NULL COMMENT '消息内容：text 为文本；image 为站内下载地址；card/location/file 为 JSON',
  `msg_type` varchar(16) NOT NULL COMMENT '消息类型：text 文本 / image 图片 / poke 拍一拍 / system 系统 / card 名片 / location 位置 / file 文件',
  `status` varchar(16) NOT NULL COMMENT '消息状态：SENT 已发送 / RECALLED 已撤回（发送2分钟内可撤回）',
  `reply_to_id` varchar(36) DEFAULT NULL COMMENT '引用回复的消息ID',
  `read_flag` tinyint(4) DEFAULT 0 COMMENT '是否已读：1 已读 / 0 未读',
  `edited` tinyint(4) DEFAULT 0 COMMENT '内容是否已编辑：1 已编辑 / 0 未编辑',
  `created` bigint(20) NOT NULL COMMENT '发送时间（毫秒时间戳）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点对点私聊消息表：支持文本/图片/拍一拍/系统/名片/位置/文件消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `private_message`
--

LOCK TABLES `private_message` WRITE;
/*!40000 ALTER TABLE `private_message` DISABLE KEYS */;
INSERT INTO `private_message` VALUES
('03810e0f-e586-4a71-bf8c-f3e3ee5331cf','kaylazho','liufan','可爱的帆宝','text','SENT',NULL,1,0,1790426274398),
('055e39e5-be81-405c-b24e-5b53e9e1453a','liufan','kaylazho','你能看到了吗？','text','SENT',NULL,1,0,1790426316108),
('0758590f-aab1-448c-bd7f-50118fbac5ec','kaylazho','liufan','✨','text','SENT',NULL,1,0,1790429520748),
('0a2b6a99-9a64-42c3-bc77-5db7abdeaeaa','kaylazho','liufan','🎉','text','SENT',NULL,1,0,1790429492339),
('0b027662-ceec-4889-bb33-2657be346271','kaylazho','liufan','💔','text','SENT',NULL,1,0,1790429499440),
('0cefde68-24fa-4808-8288-7cd27cf921e3','kaylazho','liufan','有一天，猪和老虎在外面山洞过夜','text','SENT',NULL,1,0,1790428145506),
('105f13d4-d22d-4e04-b6fe-0cad916e14b7','liufan','kaylazho','辛苦了','text','SENT',NULL,1,0,1790427684818),
('169d0486-4b80-4b45-9e0d-5752caab32e0','liufan','kaylazho','{\"name\":\"酒店-0909-0911.jpg\",\"size\":624181,\"url\":\"/api/files/cb1a31e8-40e1-4610-a771-1604ed1ae062/download\"}','file','SENT',NULL,1,0,1790427668616),
('1bb453b3-34e4-440e-8d3b-231fdcdd3abf','liufan','kaylazho','就是乱发的','text','SENT',NULL,1,0,1790427858215),
('1c4c6323-ab89-41fc-8dfb-27fca0ff86b9','liufan','kaylazho','你冰雪聪明','text','SENT',NULL,1,0,1790427627926),
('1f972d69-90cc-479d-8993-af952e77fda7','liufan','kaylazho','看了','text','SENT',NULL,1,0,1790426271353),
('21925d5d-07dd-4bef-b395-4c4167aa7771','kaylazho','liufan','😄','text','SENT',NULL,1,0,1790426204060),
('2192da07-3683-4e7a-892d-9156d432f56b','kaylazho','liufan','哦，还可以引用','text','SENT',NULL,1,0,1790427154844),
('219e0f66-1587-44d8-9de0-b0cbbb44dc4f','kaylazho','liufan','讲笑话会触发什么奖励机制','text','SENT',NULL,1,0,1790428015406),
('23a33fa6-8c66-4282-bfa8-773c26d9dbde','liufan','kaylazho','10分钟','text','SENT',NULL,1,0,1790429990898),
('28244fef-f744-4846-96b2-155e7e359e4e','kaylazho','liufan','不会讲笑话','text','SENT',NULL,1,0,1790427916563),
('29d7354d-2e6f-4974-a836-873af15a6e60','kaylazho','liufan','这还能撤回消息，咋弄的','text','SENT',NULL,1,0,1790427131186),
('2bd8e426-62f0-4186-8ba4-4c1f5d7bbe4a','kaylazho','liufan','🌹','text','SENT',NULL,1,0,1790429524673),
('2e9c7429-9bb1-4872-8115-f50a5f320db2','liufan','kaylazho','你先上个厕所','text','SENT',NULL,1,0,1790429996626),
('2e9f5fc0-d2e5-4d06-aa8f-c51806609c1d','liufan','kaylazho','在部署程序','text','SENT',NULL,1,0,1790429987799),
('3125b215-4916-463f-9a5b-fb9c54ee6e07','liufan','kaylazho','你琢磨一下','text','SENT',NULL,1,0,1790427624164),
('32bf8ed7-04fd-4223-8b41-b6833d134cd1','liufan','kaylazho','❤️','text','SENT',NULL,1,0,1790429976202),
('349882ca-b426-4a59-bf53-e466b9c4555a','kaylazho','liufan','因为我冰雪聪明','text','SENT',NULL,1,0,1790427008715),
('39588c45-2196-493a-bdd2-f47022edc865','liufan','kaylazho','收到','text','SENT',NULL,1,0,1790427679081),
('3a601077-d655-41b0-95ec-abf9a5b1c16e','liufan','kaylazho','嘿嘿','text','SENT',NULL,1,0,1790426308959),
('3db97e7a-7947-4bbd-a5b5-0ed3067a224b','liufan','kaylazho','触发奖励机制','text','SENT',NULL,1,0,1790428087747),
('42ad26db-cfc3-4566-aa15-79b9c7992bcf','liufan','kaylazho','牛批','text','SENT',NULL,1,0,1790427059644),
('46a48594-f4fe-485a-8e47-896786547d47','liufan','kaylazho','老虎被猪打死了？','text','SENT',NULL,1,0,1790428181843),
('4944bcfb-4f0d-485c-8087-8aea64b8e3e9','kaylazho','liufan','🎁','text','SENT',NULL,1,0,1790429528779),
('506145d2-4f8c-4aec-a18a-88d0c82b08e1','kaylazho','liufan','是不是猜不出来','text','SENT',NULL,1,0,1790428828618),
('5067ecae-9d57-4d06-848b-89c9ae7fed5c','kaylazho','liufan','🐷也猜不到呀','text','SENT',NULL,1,0,1790428909347),
('545287bb-cf23-40c6-929b-c582ff0800c3','kaylazho','liufan','[拍一拍]','poke','SENT',NULL,1,0,1790425875296),
('54db10fd-7617-4148-bd29-5397f648fd8f','kaylazho','liufan','看到了','text','SENT',NULL,1,0,1790427907453),
('54f6d25a-5e18-4d59-97d2-f4f82df012df','kaylazho','liufan','高冷女王','text','SENT',NULL,1,0,1790427963945),
('582db41d-e4bb-4fc6-83f8-27e1906e7acd','kaylazho','liufan','不是的','text','SENT',NULL,1,0,1790428436823),
('5ace757e-ac91-4ca0-8f79-0ee1f898a0a6','kaylazho','liufan','回应是干啥的','text','SENT',NULL,1,0,1790427272343),
('610c2c7c-a048-40f5-b911-bf4239e91fc1','kaylazho','liufan','没事，你部署吧','text','SENT',NULL,1,0,1790430013133),
('63ffe72c-a41c-462d-8e0f-167674c51c5e','kaylazho','liufan','帅哥，你qq号多少','text','SENT',NULL,1,0,1790426666968),
('6e5240b9-b376-4a04-b027-9479bf4c162f','liufan','kaylazho','就是 cute 一下','text','RECALLED',NULL,1,0,1790427417829),
('7285eddb-e90a-45b8-8225-d472a60f9543','liufan','kaylazho','好的','text','SENT',NULL,0,0,1790430985308),
('76667cfc-9ec2-400b-8ea8-7a2dbe9d98db','liufan','kaylazho','对，你咋知道','text','SENT',NULL,1,0,1790426957786),
('766833b5-b995-49b4-8109-8d8d0a4cd0ea','kaylazho','liufan','不是','text','SENT',NULL,1,0,1790428817237),
('78010695-074a-40d9-9b98-fea1541d464b','liufan','kaylazho','不用这个app','text','SENT',NULL,1,0,1790426711922),
('7801f499-ef3c-46e0-aafa-2f3e040df15c','kaylazho','liufan','😜','text','SENT',NULL,1,0,1790429005841),
('7b2a7f1f-67e2-4c4e-a973-f9a8af1fa7a8','kaylazho','liufan','哦哦，实验呢','text','SENT',NULL,1,0,1790427878739),
('7b2b422c-e45d-4a86-8675-7b13287b4c35','kaylazho','liufan','❤️','text','SENT',NULL,1,0,1790426288923),
('82ceea0a-828a-48d0-a96d-34d7ffa530c4','kaylazho','liufan','搓手手，期待','text','SENT',NULL,1,0,1790429341556),
('8348e385-7163-4809-9f87-b4d41571d3bb','kaylazho','liufan','没看出来咋玩的','text','SENT',NULL,1,0,1790427510934),
('834bc16d-82ae-4505-862b-965159b31a19','liufan','kaylazho','我部署一下','text','SENT',NULL,1,0,1790429240175),
('859691a4-0391-4819-85dd-e9689631ce4d','kaylazho','liufan','猜猜这是为啥','text','SENT',NULL,1,0,1790428190562),
('8696b715-5bda-41dd-9cfa-19b690906256','kaylazho','liufan','不是','text','SENT','46a48594-f4fe-485a-8e47-896786547d47',1,0,1790428203811),
('86b05f6c-56d1-462e-a6db-da6a6cfb244f','kaylazho','liufan','再猜','text','SENT',NULL,1,0,1790428446678),
('87584f19-bd0d-49b7-8d1d-61ec7d477863','kaylazho','liufan','🤣','text','SENT',NULL,1,0,1790428916458),
('88f0d498-be7e-4acd-96e1-e0cd9bcf8b28','kaylazho','liufan','你不是让我给你讲个笑话，我讲了','text','SENT',NULL,1,0,1790429220460),
('891a7ea8-6c4e-4f32-91df-df0d3dd3a946','liufan','kaylazho','老虎被猪臭死了','text','SENT',NULL,1,0,1790428570543),
('92abfcc4-5b6a-4e71-897b-8b6675e42149','kaylazho','liufan','⭐','text','SENT',NULL,1,0,1790429511972),
('97ee6ba9-3406-4dfd-8a14-a70f32d70288','liufan','kaylazho','被压死了','text','SENT',NULL,1,0,1790428663224),
('9f488d23-00d3-4c2e-a8c1-5f7f3486a2a3','liufan','kaylazho','老虎被猪吃了','text','SENT',NULL,1,0,1790428391432),
('9f695f2c-c730-45c1-8a42-42bf89920961','liufan','kaylazho','在部署啊','text','SENT',NULL,1,0,1790429450334),
('a17363c3-c602-44c2-a150-0129659666cc','liufan','kaylazho','牛屁','text','RECALLED',NULL,1,0,1790427050609),
('a5ee3b72-f030-4852-bfe9-b4295117286e','liufan','kaylazho','给你发呢','text','SENT',NULL,1,0,1790427743662),
('aa68d34c-3d51-41a0-87e4-cb1cc6497a32','kaylazho','liufan','你今天一天就是手搓了一个网站嘛','text','SENT',NULL,1,0,1790426868704),
('ab3b75ac-59cb-46fa-bfcb-6252dad19625','kaylazho','liufan','把你打成🐷','text','SENT',NULL,1,0,1790426778842),
('accb6fd1-bf9e-4ec8-80e5-92adabdf7e4e','kaylazho','liufan','结果，第2天，老虎死了。','text','SENT',NULL,1,0,1790428162683),
('b19c5dca-a025-4d7a-b1f9-413828c2f29c','kaylazho','liufan','天生严肃','text','SENT',NULL,1,0,1790427925018),
('b5935a98-6db7-444f-b48b-711f2375a07d','kaylazho','liufan','这个界面感觉怪怪的','text','SENT',NULL,1,0,1790425676451),
('b692e69c-edba-4fd8-90aa-4eb9b1cb5be7','kaylazho','liufan','嘿嘿','text','SENT',NULL,1,0,1790429320133),
('be03782a-70c9-4711-aad0-ea84548550a5','liufan','kaylazho','。。。。','text','SENT',NULL,1,0,1790428937713),
('c094dbca-1323-4807-a858-2a8fcd2564ad','kaylazho','liufan','奖励啥','text','SENT',NULL,1,0,1790429230277),
('c30bec27-faf0-42eb-82f1-929d50dbf299','liufan','kaylazho','弄了两天了','text','SENT',NULL,1,0,1790426882189),
('c50295b0-7ada-4010-89e0-7b5ca68851d7','liufan','kaylazho','山洞塌陷了','text','SENT',NULL,1,0,1790428659902),
('c572615f-ec09-4973-81ea-fc5cc6933454','liufan','kaylazho','下次狠狠 ** 你','text','SENT',NULL,1,0,1790428957599),
('c60c3ae4-a5cf-458f-bc08-0bb904cac171','kaylazho','liufan','还能针对消息发表情，哦哟，厉害','text','SENT',NULL,1,0,1790427206931),
('c7bb26ce-8940-479b-9d82-fbd2546a52b9','kaylazho','liufan','❤️','text','SENT',NULL,1,0,1790429506017),
('c99dd17f-f483-4ee3-b071-5d341a487621','kaylazho','liufan','猪也在山洞，猪也没死呀','text','SENT',NULL,1,0,1790428744966),
('cb4ebbc6-eefa-46f6-8f45-59aab0cee4ca','kaylazho','liufan','弄好了，我再过来玩','text','SENT',NULL,1,0,1790430026646),
('cdb4ab08-9a4f-49fb-a77b-88daf876ec6e','liufan','kaylazho','看到通知公告没有？','text','SENT',NULL,1,0,1790427865372),
('cfd5688b-348a-47c3-a678-07da64855c65','kaylazho','liufan','哈哈哈哈，你给谁发呢','text','SENT',NULL,1,0,1790427707708),
('d11291dd-c568-43e1-b324-ba7ac28547ae','kaylazho','liufan','只有礼花和红心有特效','text','SENT',NULL,1,0,1790429558158),
('d76a04b4-8c31-4fd8-b01d-ad26e2a3349c','liufan','kaylazho','现在社交软件，都有这个功能','text','SENT',NULL,1,0,1790427439622),
('d82d8829-25bc-4895-bb9c-05e88930ad6e','kaylazho','liufan','🐷','text','SENT',NULL,1,0,1790426762995),
('db7434ea-bc34-41c4-b80f-70e706e8a2c6','kaylazho','liufan','🔥','text','SENT',NULL,1,0,1790429517230),
('dbb2c192-2af6-4413-8a71-c00cbd54ab7d','kaylazho','liufan','哦哟，你这是要赚外快去嘛','text','SENT',NULL,1,0,1790426911853),
('de898ee8-5aa4-4c3b-926b-034ba5114452','liufan','kaylazho','就是 care 一下','text','SENT',NULL,1,0,1790427431946),
('dfa457c4-3619-458e-990e-7d7a49b50f91','liufan','kaylazho','我没qq啊','text','SENT',NULL,1,0,1790426700700),
('e6fe3dba-bbae-41d9-a477-0b78ca71162f','liufan','kaylazho','我猜不到','text','SENT',NULL,1,0,1790428875460),
('ecf63724-2121-4424-a83d-8b01fe6649da','liufan','kaylazho','我要修复bug','text','SENT',NULL,1,0,1790428093884),
('ed1738cb-2712-4538-9bb2-102cdf8e48ed','kaylazho','liufan','猜得到吗','text','SENT',NULL,1,0,1790428275645),
('ee6c584f-8145-44fd-95ce-3a317f97a5b1','kaylazho','liufan','你现在在干嘛呀','text','SENT',NULL,1,0,1790429421074),
('f187e71b-274c-4280-b2b4-4adb77a888c1','liufan','kaylazho','刚好压住老虎了','text','SENT',NULL,1,0,1790428778571),
('f21ce322-04a6-4839-8d11-bced45a26072','liufan','kaylazho','我们已经成为好友，现在开始聊天吧！','system','SENT',NULL,1,0,1790425510784),
('f4c7ed37-8616-4ca2-96c6-bf782708b83a','kaylazho','liufan','[拍一拍]','poke','SENT',NULL,1,0,1790425873149),
('f55ded19-3d15-4059-b000-0831c5047820','kaylazho','liufan','也不是','text','SENT',NULL,1,0,1790428584082),
('f755fcd5-2a67-452e-bf93-d4a4e2b1311d','kaylazho','liufan','就我手机看着就很怪','text','SENT',NULL,1,0,1790426322335),
('f8f78db3-8016-4033-bab8-9352dce5a947','kaylazho','liufan','你给我发的文档干啥的','text','SENT',NULL,1,0,1790427786296);
/*!40000 ALTER TABLE `private_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registration_application`
--

DROP TABLE IF EXISTS `registration_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `registration_application` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID',
  `phone` varchar(20) DEFAULT NULL COMMENT '申请手机号（区分大小写）',
  `username` varchar(50) DEFAULT NULL COMMENT '申请用户名（区分大小写）',
  `nickname` varchar(32) DEFAULT NULL COMMENT '注册时填写的昵称（审批通过后写入 app_user/user_profile，历史申请可能为空）',
  `password_hash` varchar(255) DEFAULT NULL COMMENT '登录密码哈希（PBKDF2，审批通过时原样搬入 app_user）',
  `status` varchar(16) NOT NULL COMMENT '审批状态：PENDING 待审批 / APPROVED 已通过 / REJECTED 已拒绝',
  `reject_reason` varchar(200) DEFAULT NULL COMMENT '拒绝原因',
  `created` bigint(20) NOT NULL COMMENT '申请时间（毫秒时间戳）',
  `reviewed_at` bigint(20) DEFAULT NULL COMMENT '审批时间（毫秒时间戳）',
  `reviewed_by` varchar(64) DEFAULT NULL COMMENT '审批人用户名（管理员）',
  PRIMARY KEY (`id`),
  KEY `idx_reg_app_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='注册申请表：管理员审批通过后才真正创建 app_user 账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registration_application`
--

LOCK TABLES `registration_application` WRITE;
/*!40000 ALTER TABLE `registration_application` DISABLE KEYS */;
INSERT INTO `registration_application` VALUES
('5bbf3e5c-86b5-45d7-bb22-b473104bb795','13900009999','newbie','小新','pbkdf2$60000$8E8k9/Z6id9CGzxu4Z/eag==$qA8A2INO78U2Z6oRK8jxwK+5PYEFSq9AQV+TRGB5LXI=','PENDING',NULL,1790438386649,NULL,NULL),
('restore-reg-kaylazho-000001','153****3818','kaylazho',NULL,'pbkdf2$60000$WCLcEIXKMGKFvBqNUhM8BQ==$hBEVxAUMNz6o6EqzhXN/xSWFWGz3ye4JhZJpOuBy6NA=','APPROVED',NULL,1790425181000,1790425217000,'admin'),
('restore-reg-liufan-000001','137****9579','liufan',NULL,'pbkdf2$60000$ibH0Y+8vDC2TRG1NIWlkdw==$yAQfZwcXPb3wabMGnBbN3mebuiH/ilE4CjVB5N2Ol7s=','APPROVED',NULL,1790423816000,1790423861000,'admin');
/*!40000 ALTER TABLE `registration_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `uploaded_file`
--

DROP TABLE IF EXISTS `uploaded_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `uploaded_file` (
  `id` varchar(36) NOT NULL COMMENT '主键UUID（站内下载地址为 /api/files/{id}/download）',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `stored_path` varchar(255) NOT NULL COMMENT '服务端存储路径',
  `content_type` varchar(127) DEFAULT NULL COMMENT 'MIME 类型',
  `size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
  `sha256` varchar(64) NOT NULL COMMENT '文件内容 SHA-256，全局唯一，用于秒传去重',
  `uploaded_at` bigint(20) DEFAULT NULL COMMENT '上传时间（毫秒时间戳）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_uploaded_file_sha256` (`sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='上传文件档案表：按 SHA-256 内容寻址，天然去重';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `uploaded_file`
--

LOCK TABLES `uploaded_file` WRITE;
/*!40000 ALTER TABLE `uploaded_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `uploaded_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profile`
--

DROP TABLE IF EXISTS `user_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profile` (
  `username` varchar(50) NOT NULL COMMENT '用户名，主键，关联 app_user.username（区分大小写）',
  `nickname` varchar(32) DEFAULT NULL COMMENT '昵称',
  `signature` varchar(100) DEFAULT NULL COMMENT '个性签名',
  `avatar` varchar(16) DEFAULT NULL COMMENT '头像标识（内置头像色档编号，如 c0）',
  `presence_status` varchar(16) DEFAULT 'online' COMMENT '在线状态：online 在线 / busy 忙碌 / away 离开',
  `updated_at` bigint(20) DEFAULT NULL COMMENT '资料更新时间（毫秒时间戳）',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户资料表：与 app_user 一一对应（注册时同步创建），存昵称/签名/头像/在线状态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_profile`
--

LOCK TABLES `user_profile` WRITE;
/*!40000 ALTER TABLE `user_profile` DISABLE KEYS */;
INSERT INTO `user_profile` VALUES
('admin','admin','','c0','away',1790436679535),
('kaylazho','kaylazho',NULL,NULL,'online',NULL),
('liufan','liufan',NULL,NULL,'online',NULL);
/*!40000 ALTER TABLE `user_profile` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-27  0:38:40
