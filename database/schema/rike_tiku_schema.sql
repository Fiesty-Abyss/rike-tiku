-- MySQL dump 10.13  Distrib 8.4.10, for Win64 (x86_64)
--
-- Host: localhost    Database: rike_tiku
-- ------------------------------------------------------
-- Server version	8.4.10

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ban_ji`
--

DROP TABLE IF EXISTS `ban_ji`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ban_ji` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ban_ji_bian_ma` varchar(64) NOT NULL,
  `ban_ji_ming_cheng` varchar(128) NOT NULL,
  `nian_ji` varchar(32) NOT NULL,
  `ru_xue_nian_fen` smallint NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ban_ji_bian_ma` (`ban_ji_bian_ma`),
  KEY `idx_ban_ji_nian_ji_zhuang_tai` (`nian_ji`,`zhuang_tai`,`yi_shan_chu`),
  CONSTRAINT `ck_ban_ji_ru_xue_nian_fen` CHECK ((`ru_xue_nian_fen` between 2000 and 2100)),
  CONSTRAINT `ck_ban_ji_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ban_ji_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'GRADUATED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ban_ji_xue_sheng`
--

DROP TABLE IF EXISTS `ban_ji_xue_sheng`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ban_ji_xue_sheng` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ban_ji_id` bigint NOT NULL,
  `xue_sheng_id` bigint NOT NULL,
  `shi_fou_zhu_ban_ji` tinyint NOT NULL DEFAULT '0',
  `jia_ru_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `tui_chu_shi_jian` datetime(3) DEFAULT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `you_xiao_guan_xi_biao_shi` tinyint GENERATED ALWAYS AS ((case when ((`zhuang_tai` = _utf8mb4'ACTIVE') and (`tui_chu_shi_jian` is null)) then 1 else NULL end)) STORED,
  `you_xiao_zhu_ban_ji_xue_sheng_id` bigint GENERATED ALWAYS AS ((case when ((`shi_fou_zhu_ban_ji` = 1) and (`zhuang_tai` = _utf8mb4'ACTIVE') and (`tui_chu_shi_jian` is null)) then `xue_sheng_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ban_ji_xue_sheng_active` (`ban_ji_id`,`xue_sheng_id`,`you_xiao_guan_xi_biao_shi`),
  UNIQUE KEY `uk_xue_sheng_active_main_class` (`you_xiao_zhu_ban_ji_xue_sheng_id`),
  KEY `idx_ban_ji_xue_sheng_student` (`xue_sheng_id`,`zhuang_tai`),
  KEY `idx_ban_ji_xue_sheng_class` (`ban_ji_id`,`zhuang_tai`),
  CONSTRAINT `fk_ban_ji_xue_sheng_ban_ji` FOREIGN KEY (`ban_ji_id`) REFERENCES `ban_ji` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ban_ji_xue_sheng_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ban_ji_xue_sheng_shi_jian` CHECK (((`tui_chu_shi_jian` is null) or (`tui_chu_shi_jian` >= `jia_ru_shi_jian`))),
  CONSTRAINT `ck_ban_ji_xue_sheng_tui_chu` CHECK ((((`zhuang_tai` = _utf8mb4'ACTIVE') and (`tui_chu_shi_jian` is null)) or ((`zhuang_tai` = _utf8mb4'EXITED') and (`tui_chu_shi_jian` is not null)))),
  CONSTRAINT `ck_ban_ji_xue_sheng_zhu_ban_ji` CHECK ((`shi_fou_zhu_ban_ji` in (0,1))),
  CONSTRAINT `ck_ban_ji_xue_sheng_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'EXITED')))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级学生关系及历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dao_ru_pi_ci`
--

DROP TABLE IF EXISTS `dao_ru_pi_ci`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dao_ru_pi_ci` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pi_ci_bian_hao` varchar(64) NOT NULL COMMENT '批次业务编号',
  `dao_ru_lei_xing` varchar(32) NOT NULL DEFAULT 'QUESTION' COMMENT '导入对象类型',
  `yuan_shi_wen_jian_ming` varchar(255) NOT NULL COMMENT '原始文件名',
  `yuan_shi_wen_jian_lu_jing` varchar(1000) DEFAULT NULL COMMENT '仓库相对路径或受控存储路径',
  `wen_jian_ha_xi` char(64) DEFAULT NULL COMMENT '原始文件SHA-256',
  `zong_ji_lu_shu` int NOT NULL DEFAULT '0',
  `cheng_gong_shu` int NOT NULL DEFAULT '0',
  `shi_bai_shu` int NOT NULL DEFAULT '0',
  `zhuang_tai` varchar(16) NOT NULL COMMENT 'UPLOADED/VALIDATED/IMPORTED/FAILED',
  `bei_zhu` varchar(1000) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dao_ru_pi_ci_bian_hao` (`pi_ci_bian_hao`),
  CONSTRAINT `ck_dao_ru_pi_ci_ji_shu` CHECK (((`zong_ji_lu_shu` >= 0) and (`cheng_gong_shu` >= 0) and (`shi_bai_shu` >= 0) and ((`cheng_gong_shu` + `shi_bai_shu`) <= `zong_ji_lu_shu`))),
  CONSTRAINT `ck_dao_ru_pi_ci_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'UPLOADED',_utf8mb4'VALIDATED',_utf8mb4'IMPORTED',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jiao_se`
--

DROP TABLE IF EXISTS `jiao_se`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jiao_se` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `jiao_se_dai_ma` varchar(32) NOT NULL,
  `jiao_se_ming_cheng` varchar(64) NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jiao_se_dai_ma` (`jiao_se_dai_ma`),
  KEY `idx_jiao_se_zhuang_tai_shan_chu` (`zhuang_tai`,`yi_shan_chu`),
  CONSTRAINT `ck_jiao_se_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_jiao_se_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jiao_shi_dang_an`
--

DROP TABLE IF EXISTS `jiao_shi_dang_an`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jiao_shi_dang_an` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `yong_hu_id` bigint NOT NULL,
  `gong_hao` varchar(64) NOT NULL,
  `xing_ming` varchar(64) NOT NULL,
  `xian_shi_zhi_wu` varchar(128) DEFAULT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jiao_shi_dang_an_yong_hu` (`yong_hu_id`),
  UNIQUE KEY `uk_jiao_shi_dang_an_gong_hao` (`gong_hao`),
  KEY `idx_jiao_shi_zhuang_tai_shan_chu` (`zhuang_tai`,`yi_shan_chu`),
  CONSTRAINT `fk_jiao_shi_dang_an_yong_hu` FOREIGN KEY (`yong_hu_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_jiao_shi_dang_an_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_jiao_shi_dang_an_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ke_mu`
--

DROP TABLE IF EXISTS `ke_mu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ke_mu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ke_mu_dai_ma` varchar(32) NOT NULL COMMENT '科目英文代码',
  `ke_mu_ming_cheng` varchar(32) NOT NULL COMMENT '科目名称',
  `pai_xu` int NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ke_mu_dai_ma` (`ke_mu_dai_ma`),
  UNIQUE KEY `uk_ke_mu_ming_cheng` (`ke_mu_ming_cheng`),
  CONSTRAINT `ck_ke_mu_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ke_mu_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ren_ke_guan_xi`
--

DROP TABLE IF EXISTS `ren_ke_guan_xi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ren_ke_guan_xi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `jiao_shi_id` bigint NOT NULL,
  `ban_ji_id` bigint NOT NULL,
  `ke_mu_id` bigint NOT NULL,
  `shi_fou_zhu_ren_ke` tinyint NOT NULL DEFAULT '0',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `kai_shi_shi_jian` datetime(3) NOT NULL,
  `jie_shu_shi_jian` datetime(3) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ren_ke_jiao_shi_ban_ji_ke_mu` (`jiao_shi_id`,`ban_ji_id`,`ke_mu_id`),
  KEY `idx_ren_ke_ban_ji_ke_mu_zhuang_tai` (`ban_ji_id`,`ke_mu_id`,`zhuang_tai`),
  KEY `idx_ren_ke_jiao_shi_zhuang_tai` (`jiao_shi_id`,`zhuang_tai`),
  KEY `fk_ren_ke_guan_xi_ke_mu` (`ke_mu_id`),
  CONSTRAINT `fk_ren_ke_guan_xi_ban_ji` FOREIGN KEY (`ban_ji_id`) REFERENCES `ban_ji` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ren_ke_guan_xi_jiao_shi` FOREIGN KEY (`jiao_shi_id`) REFERENCES `jiao_shi_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ren_ke_guan_xi_ke_mu` FOREIGN KEY (`ke_mu_id`) REFERENCES `ke_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ren_ke_shi_jian` CHECK (((`jie_shu_shi_jian` is null) or (`jie_shu_shi_jian` >= `kai_shi_shi_jian`))),
  CONSTRAINT `ck_ren_ke_zhu_ren_ke` CHECK ((`shi_fou_zhu_ren_ke` in (0,1))),
  CONSTRAINT `ck_ren_ke_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'ENDED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师班级科目三元任课关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu`
--

DROP TABLE IF EXISTS `ti_mu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ke_mu_id` bigint NOT NULL COMMENT '科目',
  `fu_ti_mu_id` bigint DEFAULT NULL COMMENT '结构化子题的母题',
  `dao_ru_pi_ci_id` bigint DEFAULT NULL COMMENT '导入批次',
  `ti_mu_lei_xing` varchar(32) NOT NULL COMMENT 'SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_BLANK',
  `shi_yong_mo_shi` varchar(32) NOT NULL COMMENT 'ONLINE_PRACTICE/TOPIC_LEARNING',
  `ti_gan` longtext NOT NULL COMMENT '题干正文，保留附件对象标记',
  `zheng_que_da_an` json NOT NULL COMMENT '按题型定义的版本化答案JSON',
  `nan_du` tinyint NOT NULL COMMENT '1 easy，2 medium，3 hard',
  `nan_du_shuo_ming` varchar(500) DEFAULT NULL COMMENT '难度判定说明',
  `shi_fou_ke_zi_dong_pan_fen` tinyint(1) NOT NULL DEFAULT '1',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/PUBLISHED/DISABLED',
  `nei_rong_ha_xi` char(64) NOT NULL COMMENT '规范化题干与选项SHA-256',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_nei_rong_ha_xi` (`ke_mu_id`,`nei_rong_ha_xi`),
  KEY `idx_ti_mu_ke_mu_zhuang_tai_nan_du` (`ke_mu_id`,`zhuang_tai`,`nan_du`),
  KEY `idx_ti_mu_fu_ti_mu` (`fu_ti_mu_id`),
  KEY `idx_ti_mu_dao_ru_pi_ci` (`dao_ru_pi_ci_id`),
  CONSTRAINT `fk_ti_mu_dao_ru_pi_ci` FOREIGN KEY (`dao_ru_pi_ci_id`) REFERENCES `dao_ru_pi_ci` (`id`),
  CONSTRAINT `fk_ti_mu_fu_ti_mu` FOREIGN KEY (`fu_ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `fk_ti_mu_ke_mu` FOREIGN KEY (`ke_mu_id`) REFERENCES `ke_mu` (`id`),
  CONSTRAINT `ck_ti_mu_da_an_json` CHECK ((json_type(`zheng_que_da_an`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_ti_mu_ke_pan_fen` CHECK ((`shi_fou_ke_zi_dong_pan_fen` in (0,1))),
  CONSTRAINT `ck_ti_mu_lei_xing` CHECK ((`ti_mu_lei_xing` in (_utf8mb4'SINGLE_CHOICE',_utf8mb4'MULTIPLE_CHOICE',_utf8mb4'FILL_BLANK',_utf8mb4'SUBJECTIVE'))),
  CONSTRAINT `ck_ti_mu_nan_du` CHECK ((`nan_du` in (1,2,3))),
  CONSTRAINT `ck_ti_mu_shi_yong_mo_shi` CHECK ((`shi_yong_mo_shi` in (_utf8mb4'ONLINE_PRACTICE',_utf8mb4'TOPIC_LEARNING'))),
  CONSTRAINT `ck_ti_mu_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_zhu_guan_mo_shi` CHECK (((`ti_mu_lei_xing` <> _utf8mb4'SUBJECTIVE') or ((`shi_yong_mo_shi` = _utf8mb4'TOPIC_LEARNING') and (`shi_fou_ke_zi_dong_pan_fen` = 0)))),
  CONSTRAINT `ck_ti_mu_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'PUBLISHED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_fu_jian`
--

DROP TABLE IF EXISTS `ti_mu_fu_jian`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_fu_jian` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `ti_mu_xuan_xiang_id` bigint DEFAULT NULL COMMENT '关联位置为OPTION时使用',
  `ti_mu_jie_xi_id` bigint DEFAULT NULL COMMENT '关联位置为STANDARD_ANALYSIS时使用',
  `guan_lian_wei_zhi` varchar(32) NOT NULL COMMENT 'QUESTION/OPTION/STANDARD_ANALYSIS/ANSWER',
  `fu_jian_lei_xing` varchar(16) NOT NULL COMMENT 'IMAGE/FORMULA/OTHER',
  `yuan_shi_wen_jian_ming` varchar(255) NOT NULL,
  `xiang_dui_lu_jing` varchar(1000) NOT NULL COMMENT '文件系统相对路径，不保存BLOB',
  `nei_rong_ha_xi` char(64) NOT NULL COMMENT '附件SHA-256',
  `dui_xiang_biao_shi` varchar(64) DEFAULT NULL COMMENT '例如I126、F107，与正文对象标记对应',
  `zheng_wen_zi_fu_wei_zhi` int DEFAULT NULL COMMENT '对象标记在关联正文中的1基字符位置',
  `yuan_shi_ye_ma` varchar(32) DEFAULT NULL,
  `fu_jian_shuo_ming` varchar(1000) DEFAULT NULL,
  `pai_xu` int NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_fu_jian_dui_xiang` (`ti_mu_id`,`guan_lian_wei_zhi`,`dui_xiang_biao_shi`),
  KEY `idx_ti_mu_fu_jian_pai_xu` (`ti_mu_id`,`guan_lian_wei_zhi`,`pai_xu`),
  KEY `idx_ti_mu_fu_jian_xuan_xiang` (`ti_mu_xuan_xiang_id`),
  KEY `idx_ti_mu_fu_jian_jie_xi` (`ti_mu_jie_xi_id`),
  CONSTRAINT `fk_ti_mu_fu_jian_jie_xi` FOREIGN KEY (`ti_mu_jie_xi_id`) REFERENCES `ti_mu_jie_xi` (`id`),
  CONSTRAINT `fk_ti_mu_fu_jian_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `fk_ti_mu_fu_jian_xuan_xiang` FOREIGN KEY (`ti_mu_xuan_xiang_id`) REFERENCES `ti_mu_xuan_xiang` (`id`),
  CONSTRAINT `ck_ti_mu_fu_jian_guan_lian` CHECK ((((`guan_lian_wei_zhi` = _utf8mb4'OPTION') and (`ti_mu_xuan_xiang_id` is not null) and (`ti_mu_jie_xi_id` is null)) or ((`guan_lian_wei_zhi` = _utf8mb4'STANDARD_ANALYSIS') and (`ti_mu_xuan_xiang_id` is null) and (`ti_mu_jie_xi_id` is not null)) or ((`guan_lian_wei_zhi` in (_utf8mb4'QUESTION',_utf8mb4'ANSWER')) and (`ti_mu_xuan_xiang_id` is null) and (`ti_mu_jie_xi_id` is null)))),
  CONSTRAINT `ck_ti_mu_fu_jian_lei_xing` CHECK ((`fu_jian_lei_xing` in (_utf8mb4'IMAGE',_utf8mb4'FORMULA',_utf8mb4'OTHER'))),
  CONSTRAINT `ck_ti_mu_fu_jian_pai_xu` CHECK ((`pai_xu` >= 1)),
  CONSTRAINT `ck_ti_mu_fu_jian_wei_zhi` CHECK ((`guan_lian_wei_zhi` in (_utf8mb4'QUESTION',_utf8mb4'OPTION',_utf8mb4'STANDARD_ANALYSIS',_utf8mb4'ANSWER'))),
  CONSTRAINT `ck_ti_mu_fu_jian_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_fu_jian_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED',_utf8mb4'MISSING')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目附件引用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_jie_xi`
--

DROP TABLE IF EXISTS `ti_mu_jie_xi`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_jie_xi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `jie_xi_lei_xing` varchar(16) NOT NULL COMMENT 'STANDARD/TEACHER/AI',
  `jie_xi_nei_rong` longtext NOT NULL COMMENT '解析正文，保留附件对象标记',
  `ban_ben_hao` int NOT NULL DEFAULT '1',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'DRAFT',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_jie_xi_ban_ben` (`ti_mu_id`,`jie_xi_lei_xing`,`ban_ben_hao`),
  CONSTRAINT `fk_ti_mu_jie_xi_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `ck_ti_mu_jie_xi_ban_ben` CHECK ((`ban_ben_hao` >= 1)),
  CONSTRAINT `ck_ti_mu_jie_xi_lei_xing` CHECK ((`jie_xi_lei_xing` in (_utf8mb4'STANDARD',_utf8mb4'TEACHER',_utf8mb4'AI'))),
  CONSTRAINT `ck_ti_mu_jie_xi_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_jie_xi_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'PUBLISHED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目解析版本';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_lai_yuan`
--

DROP TABLE IF EXISTS `ti_mu_lai_yuan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_lai_yuan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `nei_rong_lei_xing` varchar(32) NOT NULL COMMENT 'QUESTION/ANSWER/STANDARD_ANALYSIS',
  `lai_yuan_lei_xing` varchar(32) NOT NULL COMMENT 'REAL_EXAM/AI_GENERATED/TEACHER_CREATED',
  `lai_yuan_ming_cheng` varchar(500) NOT NULL,
  `lai_yuan_di_zhi` varchar(1000) DEFAULT NULL COMMENT 'URL或受控文件相对路径',
  `nian_fen` smallint DEFAULT NULL,
  `di_qu` varchar(100) DEFAULT NULL,
  `shi_juan_ming_cheng` varchar(500) DEFAULT NULL,
  `ti_hao` varchar(64) DEFAULT NULL,
  `quan_li_zhuang_tai` varchar(32) NOT NULL,
  `quan_li_yi_ju` varchar(1000) DEFAULT NULL,
  `huo_qu_shi_jian` datetime(3) DEFAULT NULL COMMENT '未知时保持NULL，不猜测',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_lai_yuan_nei_rong` (`ti_mu_id`,`nei_rong_lei_xing`),
  KEY `idx_ti_mu_lai_yuan_shi_juan` (`nian_fen`,`di_qu`,`shi_juan_ming_cheng`),
  CONSTRAINT `fk_ti_mu_lai_yuan_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `ck_ti_mu_lai_yuan_lei_xing` CHECK ((`lai_yuan_lei_xing` in (_utf8mb4'REAL_EXAM',_utf8mb4'AI_GENERATED',_utf8mb4'TEACHER_CREATED'))),
  CONSTRAINT `ck_ti_mu_lai_yuan_nei_rong` CHECK ((`nei_rong_lei_xing` in (_utf8mb4'QUESTION',_utf8mb4'ANSWER',_utf8mb4'STANDARD_ANALYSIS'))),
  CONSTRAINT `ck_ti_mu_lai_yuan_quan_li` CHECK ((`quan_li_zhuang_tai` in (_utf8mb4'AUTHORIZED',_utf8mb4'OPEN_LICENSE',_utf8mb4'PUBLIC_OFFICIAL',_utf8mb4'USER_PROVIDED',_utf8mb4'COPYRIGHT_UNKNOWN',_utf8mb4'RESTRICTED'))),
  CONSTRAINT `ck_ti_mu_lai_yuan_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目分项来源与权利状态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_shen_he_ji_lu`
--

DROP TABLE IF EXISTS `ti_mu_shen_he_ji_lu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_shen_he_ji_lu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `shen_he_dong_zuo` varchar(32) NOT NULL COMMENT 'SUBMITTED/APPROVED/REJECTED/DISABLED',
  `yuan_zhuang_tai` varchar(16) DEFAULT NULL,
  `mu_biao_zhuang_tai` varchar(16) NOT NULL,
  `shen_he_ren_id` bigint DEFAULT NULL COMMENT '用户模块建立后再加外键',
  `shen_he_yi_jian` varchar(2000) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ti_mu_shen_he_ji_lu` (`ti_mu_id`,`chuang_jian_shi_jian`),
  KEY `idx_ti_mu_shen_he_ren` (`shen_he_ren_id`),
  CONSTRAINT `fk_ti_mu_shen_he_ji_lu_shen_he_ren` FOREIGN KEY (`shen_he_ren_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ti_mu_shen_he_ji_lu_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `ck_ti_mu_shen_he_dong_zuo` CHECK ((`shen_he_dong_zuo` in (_utf8mb4'SUBMITTED',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'DISABLED'))),
  CONSTRAINT `ck_ti_mu_shen_he_mu_biao_zhuang_tai` CHECK ((`mu_biao_zhuang_tai` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'PUBLISHED',_utf8mb4'DISABLED'))),
  CONSTRAINT `ck_ti_mu_shen_he_yuan_zhuang_tai` CHECK (((`yuan_zhuang_tai` is null) or (`yuan_zhuang_tai` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'PUBLISHED',_utf8mb4'DISABLED'))))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目审核轨迹';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_xuan_xiang`
--

DROP TABLE IF EXISTS `ti_mu_xuan_xiang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_xuan_xiang` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `xuan_xiang_biao_shi` varchar(16) NOT NULL COMMENT 'A/B/C/D或未来更多标识',
  `xuan_xiang_nei_rong` longtext NOT NULL,
  `shi_fou_zheng_que` tinyint(1) NOT NULL DEFAULT '0',
  `pai_xu` int NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_xuan_xiang_biao_shi` (`ti_mu_id`,`xuan_xiang_biao_shi`),
  KEY `idx_ti_mu_xuan_xiang_pai_xu` (`ti_mu_id`,`pai_xu`),
  CONSTRAINT `fk_ti_mu_xuan_xiang_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `ck_ti_mu_xuan_xiang_pai_xu` CHECK ((`pai_xu` >= 1)),
  CONSTRAINT `ck_ti_mu_xuan_xiang_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_xuan_xiang_zheng_que` CHECK ((`shi_fou_zheng_que` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ti_mu_zhi_shi_dian`
--

DROP TABLE IF EXISTS `ti_mu_zhi_shi_dian`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ti_mu_zhi_shi_dian` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `zhi_shi_dian_id` bigint NOT NULL,
  `shi_fou_zhu_yao` tinyint(1) NOT NULL DEFAULT '0',
  `pai_xu` int NOT NULL DEFAULT '1',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ti_mu_zhi_shi_dian` (`ti_mu_id`,`zhi_shi_dian_id`),
  KEY `idx_ti_mu_zhi_shi_dian_fan_cha` (`zhi_shi_dian_id`,`ti_mu_id`),
  CONSTRAINT `fk_ti_mu_zhi_shi_dian_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`),
  CONSTRAINT `fk_ti_mu_zhi_shi_dian_zhi_shi_dian` FOREIGN KEY (`zhi_shi_dian_id`) REFERENCES `zhi_shi_dian` (`id`),
  CONSTRAINT `ck_ti_mu_zhi_shi_dian_pai_xu` CHECK ((`pai_xu` >= 1)),
  CONSTRAINT `ck_ti_mu_zhi_shi_dian_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_zhi_shi_dian_zhu_yao` CHECK ((`shi_fou_zhu_yao` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目与知识点多对多关系';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `xue_sheng_dang_an`
--

DROP TABLE IF EXISTS `xue_sheng_dang_an`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xue_sheng_dang_an` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `yong_hu_id` bigint NOT NULL,
  `xue_hao` varchar(64) NOT NULL,
  `xing_ming` varchar(64) NOT NULL,
  `nian_ji` varchar(32) NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xue_sheng_dang_an_yong_hu` (`yong_hu_id`),
  UNIQUE KEY `uk_xue_sheng_dang_an_xue_hao` (`xue_hao`),
  KEY `idx_xue_sheng_nian_ji_zhuang_tai` (`nian_ji`,`zhuang_tai`,`yi_shan_chu`),
  CONSTRAINT `fk_xue_sheng_dang_an_yong_hu` FOREIGN KEY (`yong_hu_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_xue_sheng_dang_an_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_xue_sheng_dang_an_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yong_hu`
--

DROP TABLE IF EXISTS `yong_hu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `yong_hu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `yong_hu_ming` varchar(64) NOT NULL,
  `mi_ma_zhai_yao` varchar(255) NOT NULL,
  `zhang_hao_zhuang_tai` varchar(16) NOT NULL DEFAULT 'ENABLED',
  `shi_fou_shou_ci_deng_lu` tinyint NOT NULL DEFAULT '1',
  `mi_ma_xiu_gai_shi_jian` datetime(3) DEFAULT NULL,
  `zui_hou_deng_lu_shi_jian` datetime(3) DEFAULT NULL,
  `ge_ren_jian_jie` varchar(500) DEFAULT NULL COMMENT '个人简介，最多500字',
  `tou_xiang_mime` varchar(64) DEFAULT NULL COMMENT '头像实际MIME类型',
  `tou_xiang` mediumblob COMMENT '头像原始二进制，接口限制最大2MB',
  `tou_xiang_geng_xin_shi_jian` datetime(3) DEFAULT NULL COMMENT '头像最近更新时间',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_yong_hu_yong_hu_ming` (`yong_hu_ming`),
  KEY `idx_yong_hu_zhuang_tai_shan_chu` (`zhang_hao_zhuang_tai`,`yi_shan_chu`),
  CONSTRAINT `ck_yong_hu_mi_ma_zhai_yao` CHECK ((char_length(`mi_ma_zhai_yao`) >= 50)),
  CONSTRAINT `ck_yong_hu_shou_ci_deng_lu` CHECK ((`shi_fou_shou_ci_deng_lu` in (0,1))),
  CONSTRAINT `ck_yong_hu_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_yong_hu_zhang_hao_zhuang_tai` CHECK ((`zhang_hao_zhuang_tai` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED',_utf8mb4'LOCKED')))
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `yong_hu_jiao_se`
--

DROP TABLE IF EXISTS `yong_hu_jiao_se`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `yong_hu_jiao_se` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `yong_hu_id` bigint NOT NULL,
  `jiao_se_id` bigint NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_yong_hu_jiao_se` (`yong_hu_id`,`jiao_se_id`),
  KEY `idx_yong_hu_jiao_se_jiao_se` (`jiao_se_id`,`zhuang_tai`),
  CONSTRAINT `fk_yong_hu_jiao_se_jiao_se` FOREIGN KEY (`jiao_se_id`) REFERENCES `jiao_se` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_yong_hu_jiao_se_yong_hu` FOREIGN KEY (`yong_hu_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_yong_hu_jiao_se_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zhi_shi_dian`
--

DROP TABLE IF EXISTS `zhi_shi_dian`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zhi_shi_dian` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ke_mu_id` bigint NOT NULL COMMENT '所属科目',
  `fu_zhi_shi_dian_id` bigint DEFAULT NULL COMMENT '父知识点',
  `zhi_shi_dian_ming_cheng` varchar(128) NOT NULL COMMENT '知识点名称',
  `wan_zheng_lu_jing` varchar(500) NOT NULL COMMENT '从一级到当前节点的完整路径',
  `ceng_ji` smallint NOT NULL COMMENT '层级，从1开始',
  `pai_xu` int NOT NULL DEFAULT '0' COMMENT '同级显示顺序',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_zhi_shi_dian_lu_jing` (`ke_mu_id`,`wan_zheng_lu_jing`),
  KEY `idx_zhi_shi_dian_fu` (`fu_zhi_shi_dian_id`),
  CONSTRAINT `fk_zhi_shi_dian_fu` FOREIGN KEY (`fu_zhi_shi_dian_id`) REFERENCES `zhi_shi_dian` (`id`),
  CONSTRAINT `fk_zhi_shi_dian_ke_mu` FOREIGN KEY (`ke_mu_id`) REFERENCES `ke_mu` (`id`),
  CONSTRAINT `ck_zhi_shi_dian_ceng_ji` CHECK ((`ceng_ji` >= 1)),
  CONSTRAINT `ck_zhi_shi_dian_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_zhi_shi_dian_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识点树';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-04 16:15:57
