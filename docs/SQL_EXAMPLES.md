# 常用只读 SQL 示例

以下查询均为脱敏只读示例。不要用 SQL 绕过应用审核、权限或状态机；AI 配置查询刻意不选择 `api_mi_yao`。

```sql
-- 账号及其 ACTIVE 角色
SELECT u.id, u.yong_hu_ming, r.jiao_se_dai_ma
FROM yong_hu u
JOIN yong_hu_jiao_se ur ON ur.yong_hu_id = u.id AND ur.zhuang_tai = 'ACTIVE'
JOIN jiao_se r ON r.id = ur.jiao_se_id
WHERE u.yong_hu_ming = ?;

-- 学生当前 ACTIVE 主班级
SELECT s.xue_hao, c.ban_ji_bian_ma, c.ban_ji_ming_cheng
FROM xue_sheng_dang_an s
JOIN ban_ji_xue_sheng cs ON cs.xue_sheng_id = s.id
JOIN ban_ji c ON c.id = cs.ban_ji_id
WHERE s.yong_hu_id = ? AND cs.zhuang_tai = 'ACTIVE' AND cs.shi_fou_zhu_ban_ji = 1;

-- 教师 ACTIVE 任课范围
SELECT t.gong_hao, c.ban_ji_bian_ma, k.ke_mu_dai_ma
FROM ren_ke_guan_xi a
JOIN jiao_shi_dang_an t ON t.id = a.jiao_shi_id
JOIN ban_ji c ON c.id = a.ban_ji_id
JOIN ke_mu k ON k.id = a.ke_mu_id
WHERE t.yong_hu_id = ? AND a.zhuang_tai = 'ACTIVE';

-- PUBLISHED 题目与知识点
SELECT q.id, k.ke_mu_dai_ma, q.ti_mu_lei_xing, q.ti_gan, p.wan_zheng_lu_jing
FROM ti_mu q JOIN ke_mu k ON k.id = q.ke_mu_id
LEFT JOIN ti_mu_zhi_shi_dian qp ON qp.ti_mu_id = q.id
LEFT JOIN zhi_shi_dian p ON p.id = qp.zhi_shi_dian_id
WHERE q.zhuang_tai = 'PUBLISHED' AND q.yi_shan_chu = 0
ORDER BY q.id, qp.shi_fou_zhu_yao DESC, qp.pai_xu;

-- 练习冻结题及正式答题事实
SELECT s.id AS session_id, sq.ti_mu_shun_xu, sq.ti_gan_kuai_zhao, a.xue_sheng_da_an,
       a.shi_fou_zheng_que, a.ti_jiao_shi_jian
FROM lian_xi_hui_hua s
JOIN lian_xi_ti_mu sq ON sq.lian_xi_hui_hua_id = s.id
LEFT JOIN xue_sheng_da_ti a ON a.lian_xi_ti_mu_id = sq.id
WHERE s.id = ? ORDER BY sq.ti_mu_shun_xu;

-- 错题
SELECT w.ti_mu_id, w.zhuang_tai, w.cuo_wu_ci_shu, w.zui_jin_cuo_wu_shi_jian
FROM cuo_ti_ji_lu w WHERE w.xue_sheng_id = ? ORDER BY w.zui_jin_cuo_wu_shi_jian DESC;

-- 按知识点聚合已提交答题，作为掌握度服务的只读事实输入
SELECT kp.id AS zhi_shi_dian_id, kp.wan_zheng_lu_jing,
       COUNT(a.id) AS da_ti_shu, SUM(a.shi_fou_zheng_que = 1) AS zheng_que_shu
FROM xue_sheng_da_ti a
JOIN lian_xi_ti_mu sq ON sq.id = a.lian_xi_ti_mu_id
JOIN ti_mu_zhi_shi_dian qk ON qk.ti_mu_id = sq.ti_mu_id
JOIN zhi_shi_dian kp ON kp.id = qk.zhi_shi_dian_id
WHERE a.xue_sheng_id = ? AND a.ti_jiao_shi_jian IS NOT NULL
GROUP BY kp.id, kp.wan_zheng_lu_jing;

-- AI 调用安全元数据与学生分析（不含 Prompt/输出/Key）
SELECT provider_dai_ma, model_dai_ma, yong_tu, shi_fou_cheng_gong,
       hao_shi_hao_miao, shu_ru_token, shu_chu_token, cuo_wu_dai_ma, chuang_jian_shi_jian
FROM ai_diao_yong_ri_zhi ORDER BY id DESC LIMIT 50;
SELECT xue_sheng_da_ti_id, cuo_wu_lei_xing, provider_dai_ma, model_dai_ma, prompt_ban_ben, zhuang_tai
FROM ai_cuo_ti_fen_xi WHERE xue_sheng_id = ? ORDER BY id DESC;

-- 当前题会话与消息；仅在服务层完成本人所有权校验后使用
SELECT c.id, c.lian_xi_ti_mu_id, c.lei_ji_lun_shu, c.zhuang_tai,
       m.fa_yan_jiao_se, m.nei_rong, m.chuang_jian_shi_jian
FROM ai_hui_hua c JOIN ai_xiao_xi m ON m.ai_hui_hua_id = c.id
WHERE c.id = ? AND c.xue_sheng_id = ? ORDER BY m.id;

-- 已启用模型配置，绝不选择 api_mi_yao
SELECT id, provider_dai_ma, mo_xing_dai_ma, api_di_zhi, yong_tu,
       shi_fou_qi_yong, shi_fou_mo_ren, chao_shi_hao_miao, retry_count, zui_da_token
FROM ai_mo_xing_pei_zhi WHERE shi_fou_qi_yong = 1;

-- AI 生成任务与候选质量评价
SELECT id, mu_ti_mu_id, chuang_jian_ren_id, qing_qiu_ha_xi, zhuang_tai,
       yi_sheng_cheng_shu_liang, hao_shi_hao_miao
FROM ai_sheng_cheng_ren_wu ORDER BY id DESC;
SELECT p.ti_mu_id, p.xue_ke_zheng_que_xing, p.da_an_zheng_que_xing, p.ke_jie_xing,
       p.zhi_shi_yi_zhi_xing, p.nan_du_pi_pei, p.shen_he_jie_guo
FROM ai_hou_xuan_ti_zhi_liang_ping_jia p WHERE p.ai_sheng_cheng_ren_wu_id = ?;

-- 候选是否已经按状态机发布
SELECT q.id, q.zhuang_tai, s.lai_yuan_lei_xing, q.fu_ti_mu_id,
       EXISTS (SELECT 1 FROM ti_mu_shen_he_ji_lu r
               WHERE r.ti_mu_id = q.id AND r.shen_he_dong_zuo = 'APPROVED') AS has_approval
FROM ti_mu q
LEFT JOIN ti_mu_lai_yuan s ON s.ti_mu_id = q.id AND s.nei_rong_lei_xing = 'QUESTION'
WHERE q.id = ?;
```
