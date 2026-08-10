package com.neu.riketiku.tiku.fujian;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class QuestionAttachmentTransactionIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "rike-tiku-attachment-transaction-" + UUID.randomUUID());

    @Autowired private JdbcTemplate jdbc;
    @Autowired private QuestionAttachmentAdminService attachments;
    @Autowired private PlatformTransactionManager transactionManager;
    private final List<Long> questionIds = new ArrayList<>();

    @DynamicPropertySource
    static void attachmentProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.attachment.storage-root", () -> STORAGE_ROOT.toString());
    }

    @AfterEach
    void cleanup() throws Exception {
        for (Long questionId : questionIds) {
            jdbc.update("DELETE FROM guan_li_cao_zuo_ri_zhi WHERE ye_wu_dui_xiang_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu_fu_jian WHERE ti_mu_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu WHERE id=?", questionId);
        }
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    @Test
    void rollbackUploadRemovesNewFileAndAttachmentRow() {
        long questionId = question();
        String relativePath = new TransactionTemplate(transactionManager).execute(status -> {
            attachments.upload(questionId, "QUESTION", image("first.png", 4));
            String path = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0",
                    String.class, questionId);
            status.setRollbackOnly();
            return path;
        });

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0", Integer.class, questionId))
                .isZero();
        assertThat(STORAGE_ROOT.resolve(relativePath)).doesNotExist();
    }

    @Test
    void rollbackReplaceKeepsOldFileAndCleansNewFile() {
        long questionId = question();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> attachments.upload(questionId, "QUESTION", image("first.png", 4)));
        long attachmentId = jdbc.queryForObject("SELECT id FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0", Long.class, questionId);
        String oldPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId);
        assertThat(STORAGE_ROOT.resolve(oldPath)).exists();

        String newPath = new TransactionTemplate(transactionManager).execute(status -> {
            attachments.replace(questionId, attachmentId, image("second.png", 8));
            String path = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId);
            status.setRollbackOnly();
            return path;
        });

        assertThat(jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId)).isEqualTo(oldPath);
        assertThat(STORAGE_ROOT.resolve(oldPath)).exists();
        assertThat(STORAGE_ROOT.resolve(newPath)).doesNotExist();
    }

    @Test
    void rollbackDeleteKeepsDatabaseRowAndOldFile() {
        long questionId = question();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> attachments.upload(questionId, "QUESTION", image("first.png", 4)));
        long attachmentId = jdbc.queryForObject("SELECT id FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0", Long.class, questionId);
        String oldPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            attachments.delete(questionId, attachmentId);
            status.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_fu_jian WHERE id=? AND yi_shan_chu=0 AND zhuang_tai='ACTIVE'", Integer.class, attachmentId))
                .isEqualTo(1);
        assertThat(STORAGE_ROOT.resolve(oldPath)).exists();
    }

    private long question() {
        jdbc.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (1,'SINGLE_CHOICE','ONLINE_PRACTICE','事务附件题干','{"schemaVersion":1,"type":"SINGLE_CHOICE","optionLabels":["A"]}',1,1,'DRAFT',?)
                """, UUID.randomUUID().toString().replace("-", ""));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','事务附件解析',1,'DRAFT')", id);
        questionIds.add(id);
        return id;
    }

    private MockMultipartFile image(String name, int size) {
        try {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", output);
                return new MockMultipartFile("file", name, "image/png", output.toByteArray());
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
