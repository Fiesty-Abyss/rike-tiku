/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_cuo_ti_fen_xi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `xue_sheng_da_ti_id` bigint NOT NULL,
  `xue_sheng_id` bigint NOT NULL,
  `cuo_wu_lei_xing` varchar(32) DEFAULT NULL,
  `cuo_wu_yuan_yin` varchar(1200) DEFAULT NULL,
  `zheng_que_si_lu` varchar(1600) DEFAULT NULL,
  `chang_jian_cuo_wu` json DEFAULT NULL,
  `fu_xi_jian_yi` json DEFAULT NULL,
  `provider_dai_ma` varchar(64) DEFAULT NULL,
  `model_dai_ma` varchar(128) DEFAULT NULL,
  `prompt_ban_ben` varchar(32) NOT NULL,
  `shu_ru_shi_shi_ha_xi` char(64) NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL,
  `cuo_wu_dai_ma` varchar(64) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_cuo_ti_fen_xi_da_ti` (`xue_sheng_da_ti_id`),
  KEY `idx_ai_cuo_ti_fen_xi_xue_sheng_status` (`xue_sheng_id`,`zhuang_tai`,`geng_xin_shi_jian`),
  CONSTRAINT `fk_ai_cuo_ti_fen_xi_da_ti` FOREIGN KEY (`xue_sheng_da_ti_id`) REFERENCES `xue_sheng_da_ti` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_cuo_ti_fen_xi_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_cuo_ti_fen_xi_arrays` CHECK ((((`chang_jian_cuo_wu` is null) or (json_type(`chang_jian_cuo_wu`) = _utf8mb4'ARRAY')) and ((`fu_xi_jian_yi` is null) or (json_type(`fu_xi_jian_yi`) = _utf8mb4'ARRAY')))),
  CONSTRAINT `ck_ai_cuo_ti_fen_xi_status` CHECK ((`zhuang_tai` in (_utf8mb4'GENERATING',_utf8mb4'SUCCESS',_utf8mb4'FAILED'))),
  CONSTRAINT `ck_ai_cuo_ti_fen_xi_success` CHECK (((`zhuang_tai` <> _utf8mb4'SUCCESS') or ((`cuo_wu_lei_xing` is not null) and (`cuo_wu_yuan_yin` is not null) and (`zheng_que_si_lu` is not null) and (`chang_jian_cuo_wu` is not null) and (`fu_xi_jian_yi` is not null) and (`provider_dai_ma` is not null) and (`model_dai_ma` is not null) and (`cuo_wu_dai_ma` is null)))),
  CONSTRAINT `ck_ai_cuo_ti_fen_xi_type` CHECK (((`cuo_wu_lei_xing` is null) or (`cuo_wu_lei_xing` in (_utf8mb4'CONCEPT_ERROR',_utf8mb4'CALCULATION_ERROR',_utf8mb4'READING_ERROR',_utf8mb4'REASONING_ERROR',_utf8mb4'MEMORY_ERROR',_utf8mb4'CARELESS_ERROR',_utf8mb4'ANSWER_FORMAT_ERROR',_utf8mb4'UNKNOWN'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定正式答题事实的学生AI错因分析';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_diao_yong_ri_zhi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_dai_ma` varchar(64) NOT NULL,
  `model_dai_ma` varchar(128) NOT NULL,
  `yong_tu` varchar(96) NOT NULL,
  `ye_wu_guan_lian` varchar(128) DEFAULT NULL,
  `shi_fou_cheng_gong` tinyint(1) NOT NULL,
  `hao_shi_hao_miao` bigint NOT NULL,
  `shu_ru_token` int DEFAULT NULL,
  `shu_chu_token` int DEFAULT NULL,
  `cuo_wu_dai_ma` varchar(64) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_diao_yong_created` (`chuang_jian_shi_jian`,`id`),
  KEY `idx_ai_diao_yong_provider_success` (`provider_dai_ma`,`shi_fou_cheng_gong`,`chuang_jian_shi_jian`),
  CONSTRAINT `ck_ai_diao_yong_latency` CHECK ((`hao_shi_hao_miao` >= 0)),
  CONSTRAINT `ck_ai_diao_yong_success` CHECK ((`shi_fou_cheng_gong` in (0,1))),
  CONSTRAINT `ck_ai_diao_yong_tokens` CHECK ((((`shu_ru_token` is null) or (`shu_ru_token` >= 0)) and ((`shu_chu_token` is null) or (`shu_chu_token` >= 0))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏AI模型调用结果日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_hou_xuan_ti_zhi_liang_ping_jia` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ai_sheng_cheng_ren_wu_id` bigint NOT NULL,
  `ti_mu_id` bigint NOT NULL,
  `bian_shi_zhai_yao` varchar(1000) NOT NULL,
  `chong_fu_ti_shi` varchar(32) NOT NULL DEFAULT 'NONE',
  `shi_fou_shi_yong_shi_jue` tinyint(1) NOT NULL DEFAULT '0',
  `xue_ke_zheng_que_xing` tinyint DEFAULT NULL,
  `da_an_zheng_que_xing` tinyint DEFAULT NULL,
  `ke_jie_xing` tinyint DEFAULT NULL,
  `zhi_shi_yi_zhi_xing` tinyint DEFAULT NULL,
  `nan_du_pi_pei` tinyint DEFAULT NULL,
  `shen_he_jie_guo` varchar(16) NOT NULL DEFAULT 'PENDING',
  `shen_he_hao_shi_fen_zhong` int DEFAULT NULL,
  `shen_he_ren_id` bigint DEFAULT NULL,
  `shen_he_ping_lun` varchar(2000) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_quality_question` (`ti_mu_id`),
  KEY `idx_ai_quality_task` (`ai_sheng_cheng_ren_wu_id`),
  KEY `fk_ai_quality_reviewer` (`shen_he_ren_id`),
  CONSTRAINT `fk_ai_quality_question` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_quality_reviewer` FOREIGN KEY (`shen_he_ren_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_quality_task` FOREIGN KEY (`ai_sheng_cheng_ren_wu_id`) REFERENCES `ai_sheng_cheng_ren_wu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_quality_binary` CHECK ((((`xue_ke_zheng_que_xing` is null) or (`xue_ke_zheng_que_xing` in (0,1))) and ((`da_an_zheng_que_xing` is null) or (`da_an_zheng_que_xing` in (0,1))) and ((`ke_jie_xing` is null) or (`ke_jie_xing` in (0,1))) and ((`zhi_shi_yi_zhi_xing` is null) or (`zhi_shi_yi_zhi_xing` in (0,1))) and ((`nan_du_pi_pei` is null) or (`nan_du_pi_pei` in (0,1))))),
  CONSTRAINT `ck_ai_quality_duplicate` CHECK ((`chong_fu_ti_shi` in (_utf8mb4'NONE',_utf8mb4'SUSPECTED_DUPLICATE'))),
  CONSTRAINT `ck_ai_quality_result` CHECK ((`shen_he_jie_guo` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED'))),
  CONSTRAINT `ck_ai_quality_review_minutes` CHECK (((`shen_he_hao_shi_fen_zhong` is null) or (`shen_he_hao_shi_fen_zhong` between 0 and 10080))),
  CONSTRAINT `ck_ai_quality_vision` CHECK ((`shi_fou_shi_yong_shi_jue` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI候选题人工质量评价';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_hui_hua` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `xue_sheng_id` bigint NOT NULL,
  `xue_sheng_da_ti_id` bigint NOT NULL,
  `lian_xi_ti_mu_id` bigint NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `lei_ji_lun_shu` int NOT NULL DEFAULT '0',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ai_hui_hua_xue_sheng_question` (`xue_sheng_id`,`lian_xi_ti_mu_id`,`geng_xin_shi_jian`),
  KEY `fk_ai_hui_hua_da_ti` (`xue_sheng_da_ti_id`),
  KEY `fk_ai_hui_hua_lian_xi_ti_mu` (`lian_xi_ti_mu_id`),
  CONSTRAINT `fk_ai_hui_hua_da_ti` FOREIGN KEY (`xue_sheng_da_ti_id`) REFERENCES `xue_sheng_da_ti` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_hui_hua_lian_xi_ti_mu` FOREIGN KEY (`lian_xi_ti_mu_id`) REFERENCES `lian_xi_ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_hui_hua_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_hui_hua_rounds` CHECK ((`lei_ji_lun_shu` between 0 and 8)),
  CONSTRAINT `ck_ai_hui_hua_status` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'LIMIT_REACHED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定当前题和正式答题事实的学生AI有限会话';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_mo_xing_pei_zhi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_dai_ma` varchar(32) NOT NULL COMMENT 'DEEPSEEK/GLM',
  `mo_xing_dai_ma` varchar(128) NOT NULL,
  `api_di_zhi` varchar(500) NOT NULL,
  `api_mi_yao` varchar(1000) DEFAULT NULL COMMENT '仅本地毕设演示模式保存，API与日志禁止回显',
  `yong_tu` varchar(16) NOT NULL COMMENT 'TEXT/VISION',
  `shi_fou_qi_yong` tinyint(1) NOT NULL DEFAULT '0',
  `shi_fou_mo_ren` tinyint(1) NOT NULL DEFAULT '0',
  `chao_shi_hao_miao` int NOT NULL DEFAULT '30000',
  `zui_da_token` int NOT NULL DEFAULT '1200',
  `retry_count` tinyint NOT NULL DEFAULT '1',
  `zui_jin_ce_shi_zhuang_tai` varchar(16) NOT NULL DEFAULT 'NOT_TESTED',
  `zui_jin_ce_shi_hao_shi` bigint DEFAULT NULL,
  `zui_jin_ce_shi_shi_jian` datetime(3) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_mo_xing_provider_model_usage` (`provider_dai_ma`,`mo_xing_dai_ma`,`yong_tu`),
  KEY `idx_ai_mo_xing_default` (`yong_tu`,`shi_fou_qi_yong`,`shi_fou_mo_ren`),
  CONSTRAINT `ck_ai_mo_xing_flags` CHECK (((`shi_fou_qi_yong` in (0,1)) and (`shi_fou_mo_ren` in (0,1)))),
  CONSTRAINT `ck_ai_mo_xing_provider` CHECK ((`provider_dai_ma` in (_utf8mb4'DEEPSEEK',_utf8mb4'GLM'))),
  CONSTRAINT `ck_ai_mo_xing_retry` CHECK ((`retry_count` between 0 and 1)),
  CONSTRAINT `ck_ai_mo_xing_test_status` CHECK ((`zui_jin_ce_shi_zhuang_tai` in (_utf8mb4'NOT_TESTED',_utf8mb4'SUCCESS',_utf8mb4'FAILED'))),
  CONSTRAINT `ck_ai_mo_xing_timeout` CHECK ((`chao_shi_hao_miao` between 1000 and 120000)),
  CONSTRAINT `ck_ai_mo_xing_tokens` CHECK ((`zui_da_token` between 64 and 8192)),
  CONSTRAINT `ck_ai_mo_xing_usage` CHECK ((`yong_tu` in (_utf8mb4'TEXT',_utf8mb4'VISION')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='本地毕设演示AI模型配置';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_sheng_cheng_ren_wu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mu_ti_mu_id` bigint NOT NULL,
  `chuang_jian_ren_id` bigint NOT NULL,
  `chuang_jian_ren_jiao_se` varchar(16) NOT NULL,
  `mu_biao_ti_xing` varchar(32) NOT NULL,
  `zhi_shi_dian_ids` json NOT NULL,
  `mu_biao_nan_du` tinyint NOT NULL,
  `bian_shi_fang_shi` varchar(32) NOT NULL,
  `sheng_cheng_shu_liang` tinyint NOT NULL,
  `qing_qiu_ha_xi` char(64) NOT NULL,
  `provider_dai_ma` varchar(32) DEFAULT NULL,
  `model_dai_ma` varchar(128) DEFAULT NULL,
  `prompt_ban_ben` varchar(32) NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL,
  `yi_sheng_cheng_shu_liang` tinyint NOT NULL DEFAULT '0',
  `shi_fou_shi_yong_shi_jue` tinyint(1) NOT NULL DEFAULT '0',
  `shi_bai_dai_ma` varchar(64) DEFAULT NULL,
  `hao_shi_hao_miao` bigint DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `wan_cheng_shi_jian` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_sheng_cheng_request_hash` (`qing_qiu_ha_xi`),
  KEY `idx_ai_sheng_cheng_mother_status` (`mu_ti_mu_id`,`zhuang_tai`,`chuang_jian_shi_jian`),
  KEY `fk_ai_sheng_cheng_creator` (`chuang_jian_ren_id`),
  CONSTRAINT `fk_ai_sheng_cheng_creator` FOREIGN KEY (`chuang_jian_ren_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ai_sheng_cheng_mother` FOREIGN KEY (`mu_ti_mu_id`) REFERENCES `ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_sheng_cheng_count` CHECK (((`sheng_cheng_shu_liang` between 1 and 3) and (`yi_sheng_cheng_shu_liang` between 0 and 3))),
  CONSTRAINT `ck_ai_sheng_cheng_difficulty` CHECK ((`mu_biao_nan_du` between 1 and 5)),
  CONSTRAINT `ck_ai_sheng_cheng_mode` CHECK ((`bian_shi_fang_shi` in (_utf8mb4'NUMERIC_CONDITION',_utf8mb4'SCENARIO',_utf8mb4'KNOWLEDGE_ANGLE',_utf8mb4'DISTRACTOR',_utf8mb4'COMBINED'))),
  CONSTRAINT `ck_ai_sheng_cheng_points` CHECK ((json_type(`zhi_shi_dian_ids`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_ai_sheng_cheng_role` CHECK ((`chuang_jian_ren_jiao_se` in (_utf8mb4'ADMIN',_utf8mb4'TEACHER'))),
  CONSTRAINT `ck_ai_sheng_cheng_status` CHECK ((`zhuang_tai` in (_utf8mb4'GENERATING',_utf8mb4'SUCCESS',_utf8mb4'FAILED'))),
  CONSTRAINT `ck_ai_sheng_cheng_type` CHECK ((`mu_biao_ti_xing` in (_utf8mb4'SINGLE_CHOICE',_utf8mb4'MULTIPLE_CHOICE',_utf8mb4'FILL_BLANK'))),
  CONSTRAINT `ck_ai_sheng_cheng_vision` CHECK ((`shi_fou_shi_yong_shi_jue` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI候选变式题生成任务';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_shi_jue_shang_xia_wen` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ti_mu_id` bigint NOT NULL,
  `fu_jian_ji_he_ha_xi` char(64) NOT NULL,
  `provider_dai_ma` varchar(32) NOT NULL,
  `model_dai_ma` varchar(128) NOT NULL,
  `prompt_ban_ben` varchar(32) NOT NULL,
  `shi_jue_json` json DEFAULT NULL,
  `zhuang_tai` varchar(16) NOT NULL,
  `cuo_wu_dai_ma` varchar(64) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_vision_context` (`ti_mu_id`,`fu_jian_ji_he_ha_xi`,`provider_dai_ma`,`model_dai_ma`,`prompt_ban_ben`),
  CONSTRAINT `fk_ai_vision_question` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_vision_json` CHECK (((`shi_jue_json` is null) or (json_type(`shi_jue_json`) = _utf8mb4'OBJECT'))),
  CONSTRAINT `ck_ai_vision_status` CHECK ((`zhuang_tai` in (_utf8mb4'SUCCESS',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受控且可复用的题目视觉上下文';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_xiao_xi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ai_hui_hua_id` bigint NOT NULL,
  `fa_yan_jiao_se` varchar(16) NOT NULL,
  `nei_rong` varchar(2000) NOT NULL,
  `xu_hao` int NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_xiao_xi_hui_hua_xu_hao` (`ai_hui_hua_id`,`xu_hao`),
  KEY `idx_ai_xiao_xi_hui_hua_created` (`ai_hui_hua_id`,`chuang_jian_shi_jian`,`id`),
  CONSTRAINT `fk_ai_xiao_xi_hui_hua` FOREIGN KEY (`ai_hui_hua_id`) REFERENCES `ai_hui_hua` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_ai_xiao_xi_content` CHECK ((char_length(trim(`nei_rong`)) between 1 and 2000)),
  CONSTRAINT `ck_ai_xiao_xi_order` CHECK ((`xu_hao` >= 1)),
  CONSTRAINT `ck_ai_xiao_xi_role` CHECK ((`fa_yan_jiao_se` in (_utf8mb4'USER',_utf8mb4'ASSISTANT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生当前题AI会话消息';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级学生关系及历史';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cuo_ti_ji_lu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `xue_sheng_id` bigint NOT NULL,
  `ti_mu_id` bigint NOT NULL,
  `cuo_wu_ci_shu` int NOT NULL DEFAULT '1',
  `lian_xu_zheng_que_ci_shu` int NOT NULL DEFAULT '0',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'NEW',
  `zui_jin_da_ti_id` bigint NOT NULL,
  `zui_jin_cuo_wu_shi_jian` datetime(3) NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cuo_ti_ji_lu_xue_sheng_ti_mu` (`xue_sheng_id`,`ti_mu_id`),
  KEY `idx_cuo_ti_ji_lu_xue_sheng_zhuang_tai` (`xue_sheng_id`,`zhuang_tai`,`zui_jin_cuo_wu_shi_jian`),
  KEY `fk_cuo_ti_ji_lu_ti_mu` (`ti_mu_id`),
  KEY `fk_cuo_ti_ji_lu_zui_jin_da_ti` (`zui_jin_da_ti_id`),
  CONSTRAINT `fk_cuo_ti_ji_lu_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_cuo_ti_ji_lu_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_cuo_ti_ji_lu_zui_jin_da_ti` FOREIGN KEY (`zui_jin_da_ti_id`) REFERENCES `xue_sheng_da_ti` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_cuo_ti_ji_lu_ci_shu` CHECK (((`cuo_wu_ci_shu` >= 1) and (`lian_xu_zheng_que_ci_shu` >= 0))),
  CONSTRAINT `ck_cuo_ti_ji_lu_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'NEW',_utf8mb4'REVIEWING',_utf8mb4'MASTERED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生错题聚合';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入批次';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gao_pin_kao_dian` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ren_ke_guan_xi_id` bigint NOT NULL COMMENT '所属三元任课关系',
  `zhi_shi_dian_id` bigint NOT NULL COMMENT '所属科目知识点',
  `biao_ti` varchar(200) NOT NULL,
  `nei_rong` text NOT NULL,
  `ji_yi_kou_jue` varchar(500) DEFAULT NULL,
  `chang_jian_wu_qu` text,
  `pai_xu` int NOT NULL DEFAULT '0',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_gao_pin_kao_dian_scope_status` (`ren_ke_guan_xi_id`,`zhuang_tai`,`yi_shan_chu`,`pai_xu`,`id`),
  KEY `idx_gao_pin_kao_dian_knowledge` (`zhi_shi_dian_id`),
  CONSTRAINT `fk_gao_pin_kao_dian_knowledge` FOREIGN KEY (`zhi_shi_dian_id`) REFERENCES `zhi_shi_dian` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_gao_pin_kao_dian_scope` FOREIGN KEY (`ren_ke_guan_xi_id`) REFERENCES `ren_ke_guan_xi` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_gao_pin_kao_dian_content` CHECK ((char_length(trim(`nei_rong`)) > 0)),
  CONSTRAINT `ck_gao_pin_kao_dian_deleted` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_gao_pin_kao_dian_order` CHECK ((`pai_xu` >= 0)),
  CONSTRAINT `ck_gao_pin_kao_dian_status` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED'))),
  CONSTRAINT `ck_gao_pin_kao_dian_title` CHECK ((char_length(trim(`biao_ti`)) between 1 and 200))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师维护的班级学科高频考点';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guan_li_cao_zuo_ri_zhi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cao_zuo_ren_yong_hu_id` bigint DEFAULT NULL,
  `mo_kuai` varchar(64) NOT NULL,
  `cao_zuo_lei_xing` varchar(96) NOT NULL,
  `ye_wu_dui_xiang_id` bigint DEFAULT NULL,
  `cao_zuo_jie_guo` varchar(16) NOT NULL,
  `zhai_yao` varchar(1000) DEFAULT NULL,
  `cuo_wu_dai_ma` varchar(96) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_guan_li_cao_zuo_ri_zhi_time` (`chuang_jian_shi_jian`,`id`),
  KEY `idx_guan_li_cao_zuo_ri_zhi_filter` (`mo_kuai`,`cao_zuo_lei_xing`,`cao_zuo_jie_guo`),
  KEY `idx_guan_li_cao_zuo_ri_zhi_operator` (`cao_zuo_ren_yong_hu_id`,`chuang_jian_shi_jian`),
  CONSTRAINT `fk_guan_li_cao_zuo_ri_zhi_operator` FOREIGN KEY (`cao_zuo_ren_yong_hu_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_guan_li_cao_zuo_ri_zhi_result` CHECK ((`cao_zuo_jie_guo` in (_utf8mb4'SUCCESS',_utf8mb4'FAILURE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员高风险操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师档案';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lian_xi_hui_hua` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `xue_sheng_id` bigint NOT NULL,
  `ke_mu_id` bigint NOT NULL,
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'CREATED',
  `ti_mu_shu` int NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `ti_jiao_shi_jian` datetime(3) DEFAULT NULL,
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_lian_xi_hui_hua_xue_sheng_zhuang_tai` (`xue_sheng_id`,`zhuang_tai`,`chuang_jian_shi_jian`),
  KEY `idx_lian_xi_hui_hua_ke_mu` (`ke_mu_id`),
  CONSTRAINT `fk_lian_xi_hui_hua_ke_mu` FOREIGN KEY (`ke_mu_id`) REFERENCES `ke_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_lian_xi_hui_hua_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_lian_xi_hui_hua_ti_mu_shu` CHECK ((`ti_mu_shu` >= 1)),
  CONSTRAINT `ck_lian_xi_hui_hua_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'CREATED',_utf8mb4'SUBMITTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生自主练习会话';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lian_xi_ti_mu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lian_xi_hui_hua_id` bigint NOT NULL,
  `ti_mu_id` bigint NOT NULL,
  `ti_mu_shun_xu` int NOT NULL,
  `fen_zhi` decimal(8,2) NOT NULL DEFAULT '1.00',
  `ti_mu_lei_xing` varchar(32) NOT NULL,
  `nan_du_kuai_zhao` tinyint NOT NULL,
  `ti_gan_kuai_zhao` longtext NOT NULL,
  `xuan_xiang_kuai_zhao` json DEFAULT NULL,
  `zheng_que_da_an_kuai_zhao` json NOT NULL,
  `biao_zhun_jie_xi_kuai_zhao` longtext NOT NULL,
  `zhi_shi_dian_kuai_zhao` json NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lian_xi_ti_mu_hui_hua_shun_xu` (`lian_xi_hui_hua_id`,`ti_mu_shun_xu`),
  UNIQUE KEY `uk_lian_xi_ti_mu_hui_hua_ti_mu` (`lian_xi_hui_hua_id`,`ti_mu_id`),
  KEY `idx_lian_xi_ti_mu_ti_mu` (`ti_mu_id`),
  CONSTRAINT `fk_lian_xi_ti_mu_hui_hua` FOREIGN KEY (`lian_xi_hui_hua_id`) REFERENCES `lian_xi_hui_hua` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_lian_xi_ti_mu_ti_mu` FOREIGN KEY (`ti_mu_id`) REFERENCES `ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_lian_xi_ti_mu_da_an_json` CHECK ((json_type(`zheng_que_da_an_kuai_zhao`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_lian_xi_ti_mu_fen_zhi` CHECK ((`fen_zhi` > 0)),
  CONSTRAINT `ck_lian_xi_ti_mu_lei_xing` CHECK ((`ti_mu_lei_xing` in (_utf8mb4'SINGLE_CHOICE',_utf8mb4'MULTIPLE_CHOICE',_utf8mb4'FILL_BLANK'))),
  CONSTRAINT `ck_lian_xi_ti_mu_nan_du` CHECK ((`nan_du_kuai_zhao` in (1,2,3))),
  CONSTRAINT `ck_lian_xi_ti_mu_shun_xu` CHECK ((`ti_mu_shun_xu` >= 1)),
  CONSTRAINT `ck_lian_xi_ti_mu_xuan_xiang_json` CHECK (((`xuan_xiang_kuai_zhao` is null) or (json_type(`xuan_xiang_kuai_zhao`) = _utf8mb4'ARRAY'))),
  CONSTRAINT `ck_lian_xi_ti_mu_zhi_shi_dian_json` CHECK ((json_type(`zhi_shi_dian_kuai_zhao`) = _utf8mb4'ARRAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='练习题目冻结快照';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师班级科目三元任课关系';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `si_xin_hui_hua` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ren_ke_guan_xi_id` bigint NOT NULL COMMENT '会话对应的三元任课关系',
  `xue_sheng_id` bigint NOT NULL COMMENT '会话学生档案',
  `zhuang_tai` varchar(16) NOT NULL DEFAULT 'ACTIVE',
  `zui_hou_xiao_xi_shi_jian` datetime(3) DEFAULT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `geng_xin_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_si_xin_hui_hua_scope_student` (`ren_ke_guan_xi_id`,`xue_sheng_id`),
  KEY `idx_si_xin_hui_hua_student_recent` (`xue_sheng_id`,`yi_shan_chu`,`zui_hou_xiao_xi_shi_jian`),
  CONSTRAINT `fk_si_xin_hui_hua_scope` FOREIGN KEY (`ren_ke_guan_xi_id`) REFERENCES `ren_ke_guan_xi` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_si_xin_hui_hua_student` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_si_xin_hui_hua_deleted` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_si_xin_hui_hua_status` CHECK ((`zhuang_tai` in (_utf8mb4'ACTIVE',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受三元任课关系约束的师生私信会话';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `si_xin_xiao_xi` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hui_hua_id` bigint NOT NULL,
  `fa_song_ren_yong_hu_id` bigint NOT NULL,
  `nei_rong` varchar(1000) NOT NULL,
  `yi_du` tinyint NOT NULL DEFAULT '0',
  `fa_song_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `yi_du_shi_jian` datetime(3) DEFAULT NULL,
  `yi_shan_chu` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_si_xin_xiao_xi_conversation_time` (`hui_hua_id`,`fa_song_shi_jian`,`id`),
  KEY `idx_si_xin_xiao_xi_unread` (`hui_hua_id`,`yi_du`,`fa_song_ren_yong_hu_id`,`yi_shan_chu`),
  KEY `fk_si_xin_xiao_xi_sender` (`fa_song_ren_yong_hu_id`),
  CONSTRAINT `fk_si_xin_xiao_xi_conversation` FOREIGN KEY (`hui_hua_id`) REFERENCES `si_xin_hui_hua` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_si_xin_xiao_xi_sender` FOREIGN KEY (`fa_song_ren_yong_hu_id`) REFERENCES `yong_hu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_si_xin_xiao_xi_content` CHECK ((char_length(trim(`nei_rong`)) between 1 and 1000)),
  CONSTRAINT `ck_si_xin_xiao_xi_deleted` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_si_xin_xiao_xi_read` CHECK ((`yi_du` in (0,1))),
  CONSTRAINT `ck_si_xin_xiao_xi_read_time` CHECK ((((`yi_du` = 0) and (`yi_du_shi_jian` is null)) or ((`yi_du` = 1) and (`yi_du_shi_jian` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='师生私信文本消息';
/*!40101 SET character_set_client = @saved_cs_client */;
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
  CONSTRAINT `ck_ti_mu_nan_du` CHECK ((`nan_du` between 1 and 5)),
  CONSTRAINT `ck_ti_mu_shi_yong_mo_shi` CHECK ((`shi_yong_mo_shi` in (_utf8mb4'ONLINE_PRACTICE',_utf8mb4'TOPIC_LEARNING'))),
  CONSTRAINT `ck_ti_mu_yi_shan_chu` CHECK ((`yi_shan_chu` in (0,1))),
  CONSTRAINT `ck_ti_mu_zhu_guan_mo_shi` CHECK (((`ti_mu_lei_xing` <> _utf8mb4'SUBJECTIVE') or ((`shi_yong_mo_shi` = _utf8mb4'TOPIC_LEARNING') and (`shi_fou_ke_zi_dong_pan_fen` = 0)))),
  CONSTRAINT `ck_ti_mu_zhuang_tai` CHECK ((`zhuang_tai` in (_utf8mb4'DRAFT',_utf8mb4'PENDING',_utf8mb4'PUBLISHED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目主表';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目附件引用';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目解析版本';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目分项来源与权利状态';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目审核轨迹';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目与知识点多对多关系';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xue_sheng_da_ti` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lian_xi_ti_mu_id` bigint NOT NULL,
  `xue_sheng_id` bigint NOT NULL,
  `xue_sheng_da_an` json NOT NULL,
  `shi_fou_zheng_que` tinyint(1) NOT NULL,
  `de_fen` decimal(8,2) NOT NULL,
  `yong_shi_miao_shu` int DEFAULT NULL,
  `ti_jiao_shi_jian` datetime(3) NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xue_sheng_da_ti_lian_xi_ti_mu` (`lian_xi_ti_mu_id`),
  KEY `idx_xue_sheng_da_ti_xue_sheng` (`xue_sheng_id`,`ti_jiao_shi_jian`),
  CONSTRAINT `fk_xue_sheng_da_ti_lian_xi_ti_mu` FOREIGN KEY (`lian_xi_ti_mu_id`) REFERENCES `lian_xi_ti_mu` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_xue_sheng_da_ti_xue_sheng` FOREIGN KEY (`xue_sheng_id`) REFERENCES `xue_sheng_dang_an` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_xue_sheng_da_ti_de_fen` CHECK ((`de_fen` >= 0)),
  CONSTRAINT `ck_xue_sheng_da_ti_yong_shi` CHECK (((`yong_shi_miao_shu` is null) or (`yong_shi_miao_shu` >= 0))),
  CONSTRAINT `ck_xue_sheng_da_ti_zheng_que` CHECK ((`shi_fou_zheng_que` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生正式答题事实';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生档案';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xue_xi_jie_guo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lian_xi_hui_hua_id` bigint NOT NULL,
  `zong_ti_shu` int NOT NULL,
  `zheng_que_shu` int NOT NULL,
  `zong_de_fen` decimal(10,2) NOT NULL,
  `ti_jiao_shi_jian` datetime(3) NOT NULL,
  `chuang_jian_shi_jian` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_xue_xi_jie_guo_hui_hua` (`lian_xi_hui_hua_id`),
  CONSTRAINT `fk_xue_xi_jie_guo_hui_hua` FOREIGN KEY (`lian_xi_hui_hua_id`) REFERENCES `lian_xi_hui_hua` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_xue_xi_jie_guo_de_fen` CHECK ((`zong_de_fen` >= 0)),
  CONSTRAINT `ck_xue_xi_jie_guo_ji_shu` CHECK (((`zong_ti_shu` >= 1) and (`zheng_que_shu` between 0 and `zong_ti_shu`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='练习最终结果';
/*!40101 SET character_set_client = @saved_cs_client */;
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
  `ge_ren_jian_jie` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `tou_xiang_mime` varchar(64) DEFAULT NULL COMMENT '头像MIME类型',
  `tou_xiang` mediumblob COMMENT '头像原始二进制',
  `tou_xiang_geng_xin_shi_jian` datetime(3) DEFAULT NULL COMMENT '头像更新时间',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联';
/*!40101 SET character_set_client = @saved_cs_client */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识点树';
/*!40101 SET character_set_client = @saved_cs_client */;
