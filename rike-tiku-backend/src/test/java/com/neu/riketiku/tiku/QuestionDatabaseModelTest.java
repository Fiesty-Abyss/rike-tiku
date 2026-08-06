package com.neu.riketiku.tiku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.riketiku.tiku.entity.KeMu;
import com.neu.riketiku.tiku.entity.TiMu;
import com.neu.riketiku.tiku.entity.TiMuFuJian;
import com.neu.riketiku.tiku.mapper.KeMuMapper;
import com.neu.riketiku.tiku.mapper.TiMuFuJianMapper;
import com.neu.riketiku.tiku.mapper.TiMuMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class QuestionDatabaseModelTest {
    private static final Set<String> BUSINESS_TABLES = Set.of(
        "ke_mu", "zhi_shi_dian", "dao_ru_pi_ci", "ti_mu", "ti_mu_xuan_xiang",
        "ti_mu_jie_xi", "ti_mu_zhi_shi_dian", "ti_mu_fu_jian", "ti_mu_lai_yuan",
        "ti_mu_shen_he_ji_lu", "yong_hu", "jiao_se", "yong_hu_jiao_se",
        "xue_sheng_dang_an", "jiao_shi_dang_an", "ban_ji", "ban_ji_xue_sheng",
        "ren_ke_guan_xi", "lian_xi_hui_hua", "lian_xi_ti_mu", "xue_sheng_da_ti",
        "xue_xi_jie_guo", "cuo_ti_ji_lu"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private KeMuMapper keMuMapper;
    @Autowired
    private TiMuMapper tiMuMapper;
    @Autowired
    private TiMuFuJianMapper tiMuFuJianMapper;

    @Test
    void flywayShouldCreateOnlyApprovedBusinessTables() {
        List<String> tables = jdbcTemplate.queryForList("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            """, String.class);

        assertThat(Set.copyOf(tables)).isEqualTo(BUSINESS_TABLES);
        Integer migrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);
        assertThat(migrations).isEqualTo(7);
    }

    @Test
    void shouldLoadThreePendingRealSamplesThroughMyBatisPlus() {
        List<KeMu> subjects = keMuMapper.selectList(
            new LambdaQueryWrapper<KeMu>().orderByAsc(KeMu::getPaiXu));
        assertThat(subjects).extracting(KeMu::getKeMuDaiMa)
            .containsExactly("PHYSICS", "CHEMISTRY", "BIOLOGY");

        List<TiMu> questions = tiMuMapper.selectList(
            new LambdaQueryWrapper<TiMu>()
                .eq(TiMu::getDaoRuPiCiId, 1L)
                .orderByAsc(TiMu::getId));
        assertThat(questions).hasSize(3);
        assertThat(questions).extracting(TiMu::getZhuangTai).containsOnly("PENDING");
        assertThat(questions).extracting(TiMu::getTiMuLeiXing).containsOnly("SINGLE_CHOICE");
        assertThat(questions).extracting(TiMu::getKeMuId).containsExactly(1L, 2L, 3L);

        Integer optionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu_xuan_xiang WHERE ti_mu_id IN (1, 2, 3)", Integer.class);
        Integer analysisCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu_jie_xi WHERE ti_mu_id IN (1, 2, 3) AND jie_xi_lei_xing='STANDARD' AND zhuang_tai='PENDING'",
            Integer.class);
        Integer sourceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id IN (1, 2, 3) AND quan_li_zhuang_tai='COPYRIGHT_UNKNOWN'",
            Integer.class);
        Integer reviewCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu WHERE ti_mu_id IN (1, 2, 3) AND mu_biao_zhuang_tai='PENDING'",
            Integer.class);

        assertThat(optionCount).isEqualTo(12);
        assertThat(analysisCount).isEqualTo(3);
        assertThat(sourceCount).isEqualTo(9);
        assertThat(reviewCount).isEqualTo(3);
    }

    @Test
    void shouldPreserveStructuredAnswerAndFormulaAttachmentPosition() {
        String physicsAnswer = jdbcTemplate.queryForObject(
            "SELECT JSON_UNQUOTE(JSON_EXTRACT(zheng_que_da_an, '$.optionLabels[0]')) FROM ti_mu WHERE id=1",
            String.class);
        assertThat(physicsAnswer).isEqualTo("A");

        TiMuFuJian attachment = tiMuFuJianMapper.selectOne(
            new LambdaQueryWrapper<TiMuFuJian>().eq(TiMuFuJian::getTiMuId, 1L));
        assertThat(attachment.getGuanLianWeiZhi()).isEqualTo("STANDARD_ANALYSIS");
        assertThat(attachment.getFuJianLeiXing()).isEqualTo("FORMULA");
        assertThat(attachment.getDuiXiangBiaoShi()).isEqualTo("F107");
        assertThat(attachment.getXiangDuiLuJing()).startsWith("题库/");
        assertThat(attachment.getZhengWenZiFuWeiZhi()).isPositive();

        Integer markerPosition = jdbcTemplate.queryForObject(
            "SELECT LOCATE('〔公式对象 F107〕', jie_xi_nei_rong) FROM ti_mu_jie_xi WHERE id=1",
            Integer.class);
        assertThat(attachment.getZhengWenZiFuWeiZhi()).isEqualTo(markerPosition);
    }

    @Test
    @Transactional
    void shouldAllowAutoGradeExceptionAndRejectUnsupportedDifficulty() {
        int inserted = jdbcTemplate.update("""
            INSERT INTO ti_mu (
                ke_mu_id, ti_mu_lei_xing, shi_yong_mo_shi, ti_gan, zheng_que_da_an,
                nan_du, shi_fou_ke_zi_dong_pan_fen, zhuang_tai, nei_rong_ha_xi
            ) VALUES (1, 'FILL_BLANK', 'ONLINE_PRACTICE', '事务内约束测试题',
                JSON_OBJECT('schemaVersion', 1, 'type', 'FILL_BLANK', 'blanks', JSON_ARRAY()),
                2, 0, 'PENDING', REPEAT('f', 64))
            """);
        assertThat(inserted).isEqualTo(1);
        Boolean autoGradable = jdbcTemplate.queryForObject(
            "SELECT shi_fou_ke_zi_dong_pan_fen FROM ti_mu WHERE nei_rong_ha_xi=REPEAT('f', 64)",
            Boolean.class);
        assertThat(autoGradable).isFalse();

        assertThatThrownBy(() -> jdbcTemplate.update("""
            INSERT INTO ti_mu (
                ke_mu_id, ti_mu_lei_xing, shi_yong_mo_shi, ti_gan, zheng_que_da_an,
                nan_du, shi_fou_ke_zi_dong_pan_fen, zhuang_tai, nei_rong_ha_xi
            ) VALUES (1, 'SINGLE_CHOICE', 'ONLINE_PRACTICE', '非法难度测试题',
                JSON_OBJECT('schemaVersion', 1, 'type', 'SINGLE_CHOICE', 'optionLabels', JSON_ARRAY('A')),
                4, 1, 'PENDING', REPEAT('e', 64))
            """))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("ck_ti_mu_nan_du");
    }
}
