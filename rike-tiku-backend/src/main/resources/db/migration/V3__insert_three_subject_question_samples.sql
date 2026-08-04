INSERT INTO ke_mu (id, ke_mu_dai_ma, ke_mu_ming_cheng, pai_xu)
VALUES (1, 'PHYSICS', '物理', 1),
       (2, 'CHEMISTRY', '化学', 2),
       (3, 'BIOLOGY', '生物', 3);

INSERT INTO zhi_shi_dian (id, ke_mu_id, fu_zhi_shi_dian_id, zhi_shi_dian_ming_cheng, wan_zheng_lu_jing, ceng_ji, pai_xu)
VALUES (1, 1, NULL, '力学', '力学', 1, 1),
       (2, 1, 1, '机械振动与机械波', '力学>机械振动与机械波', 2, 1),
       (3, 1, 2, '波速、波长与频率', '力学>机械振动与机械波>波速、波长与频率', 3, 1),
       (4, 2, NULL, '化学基本概念', '化学基本概念', 1, 1),
       (5, 2, 4, '化学与社会', '化学基本概念>化学与社会', 2, 1),
       (6, 2, 5, '材料和文物保护', '化学基本概念>化学与社会>材料和文物保护', 3, 1),
       (7, 3, NULL, '分子与细胞', '分子与细胞', 1, 1),
       (8, 3, 7, '细胞的物质输入和输出', '分子与细胞>细胞的物质输入和输出', 2, 1),
       (9, 3, 8, '葡萄糖运输', '分子与细胞>细胞的物质输入和输出>葡萄糖运输', 3, 1);

INSERT INTO dao_ru_pi_ci (
    id, pi_ci_bian_hao, dao_ru_lei_xing, yuan_shi_wen_jian_ming, yuan_shi_wen_jian_lu_jing,
    wen_jian_ha_xi, zong_ji_lu_shu, cheng_gong_shu, shi_bai_shu, zhuang_tai, bei_zhu
) VALUES (
    1, 'QUESTION_SAMPLE_20260804_01', 'QUESTION', 'MVP30入库候选_最终版.json',
    '题库/理综/测试结果/MVP30入库候选_最终版.json',
    '9b063ecb8e9577208da0230e5c67a8beb4808b89714bec49b2d6812892b6a5fd',
    3, 3, 0, 'IMPORTED', '本轮仅选择物理、化学、生物各1题进行真实入库验证，未导入完整30题'
);

INSERT INTO ti_mu (
    id, ke_mu_id, dao_ru_pi_ci_id, ti_mu_lei_xing, shi_yong_mo_shi, ti_gan, zheng_que_da_an,
    nan_du, nan_du_shuo_ming, shi_fou_ke_zi_dong_pan_fen, zhuang_tai, nei_rong_ha_xi
) VALUES
(1, 1, 1, 'SINGLE_CHOICE', 'ONLINE_PRACTICE',
 '船上的人和水下的潜水员都能听见轮船的鸣笛声。声波在空气中和在水中传播时的（ ）',
 JSON_OBJECT('schemaVersion', 1, 'type', 'SINGLE_CHOICE', 'optionLabels', JSON_ARRAY('A')),
 1, '考查波速、波长与频率的直接识记或单步判断，条件明确，运算与推理量较小。', 1, 'PENDING',
 'b7d48647360c75389cb3e8f27adb4ba2da52d5f3dc9313ef17846e744e2738ab'),
(2, 2, 1, 'SINGLE_CHOICE', 'ONLINE_PRACTICE',
 '化学在文物的研究和修复中有重要作用。下列说法错误的是',
 JSON_OBJECT('schemaVersion', 1, 'type', 'SINGLE_CHOICE', 'optionLabels', JSON_ARRAY('C')),
 1, '考查材料和文物保护的直接识记或单步判断，条件明确，运算与推理量较小。', 1, 'PENDING',
 '949249aaed4cc6cba54fd8c8b244624d94f8989238ae5c27b67314faadf04d4f'),
(3, 3, 1, 'SINGLE_CHOICE', 'ONLINE_PRACTICE',
 '葡萄糖是人体所需的一种单糖。下列关于人体内葡萄糖的叙述，错误的是（ ）',
 JSON_OBJECT('schemaVersion', 1, 'type', 'SINGLE_CHOICE', 'optionLabels', JSON_ARRAY('B')),
 1, '考查葡萄糖运输的直接识记或单步判断，条件明确，运算与推理量较小。', 1, 'PENDING',
 '4bfbf7ac95e0e53d5c092c1b52201b9aaa33756024a288f734455527849d375b');

INSERT INTO ti_mu_xuan_xiang (ti_mu_id, xuan_xiang_biao_shi, xuan_xiang_nei_rong, shi_fou_zheng_que, pai_xu)
VALUES
(1, 'A', '波速和波长均不同', 1, 1), (1, 'B', '频率和波速均不同', 0, 2),
(1, 'C', '波长和周期均不同', 0, 3), (1, 'D', '周期和频率均不同', 0, 4),
(2, 'A', '竹简的成分之一纤维素属于天然高分子', 0, 1),
(2, 'B', '龟甲的成分之一羟基磷灰石属于无机物', 0, 2),
(2, 'C', '古陶瓷修复所用的熟石膏，其成分为Ca(OH)₂', 1, 3),
(2, 'D', '古壁画颜料中所用的铁红，其成分为Fe₂O₃', 0, 4),
(3, 'A', '葡萄糖是人体血浆的重要组成成分，其含量受激素的调节', 0, 1),
(3, 'B', '葡萄糖是机体能量的重要来源，能经自由扩散通过细胞膜', 1, 2),
(3, 'C', '血液中的葡萄糖进入肝细胞可被氧化分解或转化为肝糖原', 0, 3),
(3, 'D', '血液中的葡萄糖进入人体脂肪组织细胞可转变为甘油三酯', 0, 4);

INSERT INTO ti_mu_jie_xi (id, ti_mu_id, jie_xi_lei_xing, jie_xi_nei_rong, ban_ben_hao, zhuang_tai)
VALUES
(1, 1, 'STANDARD', '【详解】声波的周期和频率由振源决定，故声波在空气中和在水中传播的周期和频率均相同，但声波在空气和水中传播的波速不同，根据波速与波长关系〔公式对象 F107〕可知，波长也不同。故A正确，BCD错误。\n故选A。', 1, 'PENDING'),
(2, 2, 'STANDARD', '【详解】A．纤维素是一种天然化合物，其分子式为(C₆H₁₀O₅)n，其相对分子质量较高，是一种天然高分子，A正确；\nB．羟基磷灰石又称羟磷灰石、碱式磷酸钙，其化学式为[Ca₁₀(PO₄)₆(OH)₂]，属于无机物，B正确；\nC．熟石膏是主要成分为2CaSO₄·H₂O，Ca(OH)₂为熟石灰的主要成分，C错误；\nD．Fe₂O₃为红色，常被用于油漆、涂料、油墨和橡胶的红色颜料，俗称铁红，D正确；\n故答案选C。', 1, 'PENDING'),
(3, 3, 'STANDARD', '【分析】葡萄糖是细胞生命活动所需要的主要能源物质，常被形容为“生命的燃料”。\n【详解】A、葡萄糖是人体血浆的重要组成成分，血液中的糖称为血糖，血糖含量受胰岛素、胰高血糖素等激素的调节，A正确；\nB、葡萄糖是细胞生命活动所需要的主要能源物质，是机体能量的重要来源，葡萄糖通过细胞膜进入红细胞是协助扩散，进入小肠上皮细胞为主动运输，进入组织细胞一般通过协助扩散，B错误；\nCD、血糖浓度升高时，在胰岛素作用下，血糖可以进入肝细胞进行氧化分解并合成肝糖原，进入脂肪组织细胞转变为甘油三酯，CD正确。\n故选B。', 1, 'PENDING');

INSERT INTO ti_mu_zhi_shi_dian (ti_mu_id, zhi_shi_dian_id, shi_fou_zhu_yao, pai_xu)
VALUES (1, 3, 1, 1), (2, 6, 1, 1), (3, 9, 1, 1);

INSERT INTO ti_mu_fu_jian (
    ti_mu_id, ti_mu_jie_xi_id, guan_lian_wei_zhi, fu_jian_lei_xing,
    yuan_shi_wen_jian_ming, xiang_dui_lu_jing, nei_rong_ha_xi, dui_xiang_biao_shi,
    zheng_wen_zi_fu_wei_zhi, yuan_shi_ye_ma, fu_jian_shuo_ming, pai_xu, zhuang_tai
)
SELECT 1, 1, 'STANDARD_ANALYSIS', 'FORMULA',
       'q14_2023新课标卷_答案解析_formula_107.png',
       '题库/物理/母题库/images/2023_new_standard/q14_2023新课标卷_答案解析_formula_107.png',
       'a15be0633cd8b36cdc7c68d16a1df43dc050d34661edad049931136e89e15b1f',
       'F107', LOCATE('〔公式对象 F107〕', jie_xi_nei_rong), '11',
       '旧式Equation.DSMT4公式对象预览，需人工核对文本化', 1, 'ACTIVE'
FROM ti_mu_jie_xi WHERE id = 1;

INSERT INTO ti_mu_lai_yuan (
    ti_mu_id, nei_rong_lei_xing, lai_yuan_lei_xing, lai_yuan_ming_cheng, lai_yuan_di_zhi,
    nian_fen, di_qu, shi_juan_ming_cheng, ti_hao, quan_li_zhuang_tai, quan_li_yi_ju
)
SELECT t.id, c.nei_rong_lei_xing, 'REAL_EXAM',
       '2023年高考真题——理综（新课标卷）Word版含解析.doc',
       '题库/理综/2023年高考真题——理综（新课标卷）Word版含解析.doc',
       2023, '全国', '2023年高考真题——理综（新课标卷）',
       CASE t.id WHEN 1 THEN '14' WHEN 2 THEN '7' ELSE '1' END,
       'COPYRIGHT_UNKNOWN', '本地用户提供文件；权利状态尚待人工核验'
FROM ti_mu t
CROSS JOIN (
    SELECT 'QUESTION' AS nei_rong_lei_xing
    UNION ALL SELECT 'ANSWER'
    UNION ALL SELECT 'STANDARD_ANALYSIS'
) c
WHERE t.id IN (1, 2, 3);

INSERT INTO ti_mu_shen_he_ji_lu (
    ti_mu_id, shen_he_dong_zuo, yuan_zhuang_tai, mu_biao_zhuang_tai, shen_he_ren_id, shen_he_yi_jian
)
VALUES (1, 'SUBMITTED', 'DRAFT', 'PENDING', NULL, '最小真实样本导入，等待人工审核'),
       (2, 'SUBMITTED', 'DRAFT', 'PENDING', NULL, '最小真实样本导入，等待人工审核'),
       (3, 'SUBMITTED', 'DRAFT', 'PENDING', NULL, '最小真实样本导入，等待人工审核');
