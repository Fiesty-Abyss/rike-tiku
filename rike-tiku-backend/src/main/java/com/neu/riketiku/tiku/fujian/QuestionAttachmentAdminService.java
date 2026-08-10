package com.neu.riketiku.tiku.fujian;

import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.QuestionContentHashService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QuestionAttachmentAdminService {
    private static final Pattern IMAGE_MARKER = Pattern.compile("〔图片对象 (I\\d+)〕");
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentStorage storage;
    private final QuestionAttachmentContentService contentService;
    private final QuestionContentHashService contentHashService;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;

    public QuestionAttachmentAdminService(JdbcTemplate jdbc, QuestionAttachmentStorage storage,
            QuestionAttachmentContentService contentService, QuestionContentHashService contentHashService,
            GuanLiCaoZuoRiZhiFuWu auditLog) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.contentService = contentService;
        this.contentHashService = contentHashService;
        this.auditLog = auditLog;
    }

    @Transactional
    public QuestionDtos.Attachment upload(long questionId, String position, MultipartFile file) {
        return auditLog.audited("QUESTION", "ATTACHMENT_UPLOAD", questionId, "管理员上传题目图片附件",
                () -> uploadInternal(questionId, position, file), result -> result.id());
    }

    @Transactional
    public QuestionDtos.Attachment replace(long questionId, long attachmentId, MultipartFile file) {
        return auditLog.audited("QUESTION", "ATTACHMENT_REPLACE", questionId, "管理员替换题目图片附件",
                () -> replaceInternal(questionId, attachmentId, file), result -> result.id());
    }

    @Transactional
    public void delete(long questionId, long attachmentId) {
        auditLog.audited("QUESTION", "ATTACHMENT_DELETE", questionId, "管理员删除题目图片附件",
                () -> deleteInternal(questionId, attachmentId));
    }

    private QuestionDtos.Attachment uploadInternal(long questionId, String position, MultipartFile file) {
        AttachmentContext context = context(questionId, position);
        String marker = nextMarker(questionId);
        String markerText = markerText(marker);
        String content = context.content();
        String newContent = content == null || content.isBlank() ? markerText : content + "\n" + markerText;
        QuestionAttachmentStorage.StoredImage stored = null;
        try {
            stored = store(file);
            cleanupOnRollback(stored.relativePath());
            if ("QUESTION".equals(position)) {
                updateStem(questionId, newContent);
            } else {
                jdbc.update("UPDATE ti_mu_jie_xi SET jie_xi_nei_rong=? WHERE id=?", newContent, context.analysisId());
            }
            jdbc.update("""
                    INSERT INTO ti_mu_fu_jian
                        (ti_mu_id,ti_mu_jie_xi_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,
                         xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,pai_xu,zhuang_tai)
                    VALUES (?,?,?,'IMAGE',?,?,?,?,?,?, 'ACTIVE')
                    """, questionId, context.analysisId(), position, fileName(file), stored.relativePath(),
                    stored.hash(), marker, newContent.indexOf(markerText) + 1, nextOrder(questionId, position));
            Long newAttachmentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return attachment(newAttachmentId);
        } catch (RuntimeException exception) {
            cleanupImmediately(stored);
            throw exception;
        }
    }

    private QuestionDtos.Attachment replaceInternal(long questionId, long attachmentId, MultipartFile file) {
        context(questionId, null);
        Attachment existing = attachmentRow(questionId, attachmentId);
        ensureEditablePosition(existing.position());
        QuestionAttachmentStorage.StoredImage stored = null;
        try {
            stored = store(file);
            cleanupOnRollback(stored.relativePath());
            jdbc.update("UPDATE ti_mu_fu_jian SET yuan_shi_wen_jian_ming=?,xiang_dui_lu_jing=?,nei_rong_ha_xi=?,zhuang_tai='ACTIVE',yi_shan_chu=0 WHERE id=? AND ti_mu_id=?",
                    fileName(file), stored.relativePath(), stored.hash(), attachmentId, questionId);
            cleanupAfterCommit(existing.relativePath());
            return attachment(attachmentId);
        } catch (RuntimeException exception) {
            cleanupImmediately(stored);
            throw exception;
        }
    }

    private void deleteInternal(long questionId, long attachmentId) {
        context(questionId, null);
        Attachment existing = attachmentRow(questionId, attachmentId);
        ensureEditablePosition(existing.position());
        String marker = existing.objectMarker() == null ? null : "〔图片对象 " + existing.objectMarker() + "〕";
        if (marker != null) {
            if ("QUESTION".equals(existing.position())) {
                removeStemMarker(questionId, marker);
            } else {
                jdbc.update("UPDATE ti_mu_jie_xi SET jie_xi_nei_rong=REPLACE(jie_xi_nei_rong,?, '') WHERE id=?",
                        marker, existing.analysisId());
            }
        }
        jdbc.update("UPDATE ti_mu_fu_jian SET zhuang_tai='DISABLED',yi_shan_chu=1 WHERE id=? AND ti_mu_id=?",
                attachmentId, questionId);
        cleanupAfterCommit(existing.relativePath());
    }

    private void removeStemMarker(long questionId, String marker) {
        String stem = jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?", String.class, questionId);
        updateStem(questionId, stem.replace(marker, ""));
    }

    private void updateStem(long questionId, String stem) {
        List<QuestionContentHashService.OptionContent> options = jdbc.query(
                "SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu",
                (rs, row) -> new QuestionContentHashService.OptionContent(rs.getString(1), rs.getString(2)), questionId);
        String hash = contentHashService.calculate(stem, options);
        jdbc.update("UPDATE ti_mu SET ti_gan=?,nei_rong_ha_xi=? WHERE id=?", stem, hash, questionId);
    }

    private String markerText(String marker) {
        return "〔图片对象 " + marker + "〕";
    }

    private AttachmentContext context(long questionId, String requestedPosition) {
        QuestionRow question = jdbc.query("SELECT zhuang_tai,ti_gan FROM ti_mu WHERE id=? AND yi_shan_chu=0 FOR UPDATE",
                (rs, row) -> new QuestionRow(rs.getString(1), rs.getString(2)), questionId)
                .stream().findFirst().orElseThrow(() -> error("QUESTION_NOT_FOUND", "题目不存在", HttpStatus.NOT_FOUND));
        if (!"DRAFT".equals(question.status())) {
            throw error("QUESTION_NOT_EDITABLE", "只有草稿题目可以上传、替换或删除图片", HttpStatus.CONFLICT);
        }
        if (requestedPosition == null) return new AttachmentContext(question.stem(), null);
        if (!"QUESTION".equals(requestedPosition) && !"STANDARD_ANALYSIS".equals(requestedPosition)) {
            throw error("ATTACHMENT_POSITION_INVALID", "图片只能关联题干或标准解析", HttpStatus.BAD_REQUEST);
        }
        if ("QUESTION".equals(requestedPosition)) return new AttachmentContext(question.stem(), null);
        return jdbc.query("SELECT id,jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND yi_shan_chu=0 ORDER BY ban_ben_hao DESC LIMIT 1 FOR UPDATE",
                (rs, row) -> new AttachmentContext(rs.getString(2), rs.getLong(1)), questionId)
                .stream().findFirst().orElseThrow(() -> error("QUESTION_ANALYSIS_MISSING", "题目缺少标准解析", HttpStatus.CONFLICT));
    }

    private Attachment attachmentRow(long questionId, long attachmentId) {
        return jdbc.query("SELECT ti_mu_jie_xi_id,guan_lian_wei_zhi,xiang_dui_lu_jing,dui_xiang_biao_shi FROM ti_mu_fu_jian WHERE id=? AND ti_mu_id=? AND fu_jian_lei_xing='IMAGE' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                (rs, row) -> new Attachment(rs.getObject(1, Long.class), rs.getString(2), rs.getString(3), rs.getString(4)), attachmentId, questionId)
                .stream().findFirst().orElseThrow(() -> error("ATTACHMENT_NOT_FOUND", "题目图片附件不存在", HttpStatus.NOT_FOUND));
    }

    private QuestionDtos.Attachment attachment(long attachmentId) {
        return jdbc.query("SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,zhuang_tai,xiang_dui_lu_jing,nei_rong_ha_xi FROM ti_mu_fu_jian WHERE id=?",
                (rs, row) -> {
                    String renderStatus = contentService.renderStatus(rs.getString(7), rs.getString(8), rs.getString(3), rs.getString(6));
                    return new QuestionDtos.Attachment(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), renderStatus,
                            "AVAILABLE".equals(renderStatus) ? "/api/v1/admin/question-attachments/" + rs.getLong(1) + "/content" : null);
                }, attachmentId).stream().findFirst().orElseThrow(() -> error("ATTACHMENT_NOT_FOUND", "题目图片附件不存在", HttpStatus.NOT_FOUND));
    }

    private String nextMarker(long questionId) {
        int max = 0;
        for (String marker : jdbc.queryForList("SELECT dui_xiang_biao_shi FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0 AND dui_xiang_biao_shi IS NOT NULL", String.class, questionId)) {
            Matcher matcher = IMAGE_MARKER.matcher("〔图片对象 " + marker + "〕");
            if (matcher.matches()) max = Math.max(max, Integer.parseInt(matcher.group(1).substring(1)));
        }
        return "I%03d".formatted(max + 1);
    }

    private int nextOrder(long questionId, String position) {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(pai_xu),0) FROM ti_mu_fu_jian WHERE ti_mu_id=? AND guan_lian_wei_zhi=? AND yi_shan_chu=0", Integer.class, questionId, position);
        return (max == null ? 0 : max) + 1;
    }

    private QuestionAttachmentStorage.StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw error("ATTACHMENT_FILE_INVALID", "请选择 PNG 或 JPEG 图片", HttpStatus.UNPROCESSABLE_ENTITY);
        try {
            return storage.store(fileName(file), file.getBytes());
        } catch (java.io.IOException exception) {
            throw error("ATTACHMENT_FILE_INVALID", "图片文件无法读取", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String fileName(MultipartFile file) {
        String original = file == null ? null : file.getOriginalFilename();
        String name;
        if (original == null || original.isBlank()) {
            name = "upload.png";
        } else {
            try {
                name = Path.of(original).getFileName().toString();
            } catch (InvalidPathException exception) {
                throw error("ATTACHMENT_FILE_INVALID", "图片文件名不合法", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            throw error("ATTACHMENT_FILE_INVALID", "图片文件名不合法", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    private void cleanupImmediately(QuestionAttachmentStorage.StoredImage stored) {
        if (stored != null) {
            try { cleanupIfUnreferenced(stored.relativePath(), null); } catch (RuntimeException ignored) { }
        }
    }

    private void cleanupOnRollback(String relativePath) {
        deferCleanup(relativePath, TransactionSynchronization.STATUS_ROLLED_BACK);
    }

    private void cleanupAfterCommit(String relativePath) {
        deferCleanup(relativePath, TransactionSynchronization.STATUS_COMMITTED);
    }

    private void deferCleanup(String relativePath, int targetStatus) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (targetStatus == TransactionSynchronization.STATUS_COMMITTED) cleanupIfUnreferenced(relativePath, null);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == targetStatus) {
                    try { cleanupIfUnreferenced(relativePath, null); } catch (RuntimeException ignored) { }
                }
            }
        });
    }

    private void cleanupIfUnreferenced(String relativePath, Long excludedAttachmentId) {
        String sql = "SELECT COUNT(*) FROM ti_mu_fu_jian WHERE xiang_dui_lu_jing=? AND yi_shan_chu=0 AND zhuang_tai='ACTIVE'";
        Object[] arguments = new Object[] { relativePath };
        if (excludedAttachmentId != null) {
            sql += " AND id<>?";
            arguments = new Object[] { relativePath, excludedAttachmentId };
        }
        Long references = jdbc.queryForObject(sql, Long.class, arguments);
        if (references == null || references == 0) storage.delete(relativePath);
    }

    private void ensureEditablePosition(String position) {
        if (!"QUESTION".equals(position) && !"STANDARD_ANALYSIS".equals(position)) {
            throw error("ATTACHMENT_POSITION_INVALID", "图片只能关联题干或标准解析", HttpStatus.BAD_REQUEST);
        }
    }

    private RenZhengYeWuYiChang error(String code, String message, HttpStatus status) {
        return new RenZhengYeWuYiChang(code, message, status);
    }

    private record QuestionRow(String status, String stem) { }
    private record AttachmentContext(String content, Long analysisId) { }
    private record Attachment(Long analysisId, String position, String relativePath, String objectMarker) { }
}
