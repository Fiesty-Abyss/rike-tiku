package com.neu.riketiku.tiku.fujian;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionAttachmentContentService {
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentStorage storage;

    public QuestionAttachmentContentService(JdbcTemplate jdbc, QuestionAttachmentStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public QuestionAttachmentStorage.StoredImage admin(long attachmentId) {
        return read(attachmentId, null);
    }

    @Transactional(readOnly = true)
    public QuestionAttachmentStorage.StoredImage practice(long userId, long sessionId, long attachmentId) {
        String status = jdbc.query("SELECT h.zhuang_tai FROM lian_xi_hui_hua h JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE h.id=? AND s.yong_hu_id=? AND s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0",
                (rs, row) -> rs.getString(1), sessionId, userId).stream().findFirst().orElseThrow(() -> missing());
        String position = jdbc.query("SELECT f.guan_lian_wei_zhi FROM ti_mu_fu_jian f JOIN lian_xi_ti_mu q ON q.ti_mu_id=f.ti_mu_id WHERE q.lian_xi_hui_hua_id=? AND f.id=? AND f.yi_shan_chu=0",
                (rs, row) -> rs.getString(1), sessionId, attachmentId).stream().findFirst().orElseThrow(() -> missing());
        if ("ANSWER".equals(position) || ("CREATED".equals(status) && "STANDARD_ANALYSIS".equals(position))) throw missing();
        return read(attachmentId, position);
    }

    @Transactional(readOnly = true)
    public QuestionAttachmentStorage.StoredImage wrongQuestion(long userId, long questionId, long attachmentId) {
        int owned = jdbc.queryForObject("SELECT COUNT(*) FROM cuo_ti_ji_lu c JOIN xue_sheng_dang_an s ON s.id=c.xue_sheng_id WHERE c.ti_mu_id=? AND s.yong_hu_id=? AND s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0", Integer.class, questionId, userId);
        if (owned != 1) throw missing();
        return read(attachmentId, null, questionId);
    }

    @Transactional(readOnly = true)
    public String renderStatus(String relativePath, String hash, String type, String status) {
        return storage.renderStatus(relativePath, hash, type, status);
    }

    private QuestionAttachmentStorage.StoredImage read(long attachmentId, String requiredPosition) {
        return read(attachmentId, requiredPosition, null);
    }

    private QuestionAttachmentStorage.StoredImage read(long attachmentId, String requiredPosition, Long requiredQuestionId) {
        String sql = "SELECT xiang_dui_lu_jing,nei_rong_ha_xi,fu_jian_lei_xing,zhuang_tai,guan_lian_wei_zhi FROM ti_mu_fu_jian WHERE id=? AND yi_shan_chu=0";
        java.util.List<Object> args = new java.util.ArrayList<>(java.util.List.of(attachmentId));
        if (requiredQuestionId != null) {
            sql += " AND ti_mu_id=?";
            args.add(requiredQuestionId);
        }
        Attachment row = jdbc.query(sql,
                (rs, index) -> new Attachment(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), args.toArray())
                .stream().findFirst().orElseThrow(() -> missing());
        if (!"IMAGE".equals(row.type()) || !"ACTIVE".equals(row.status()) || "ANSWER".equals(row.position()) || (requiredPosition != null && !requiredPosition.equals(row.position()))) throw missing();
        return storage.read(row.path(), row.hash());
    }

    private RenZhengYeWuYiChang missing() { return new RenZhengYeWuYiChang("ATTACHMENT_NOT_FOUND", "附件不存在、不可用或无访问权限", HttpStatus.NOT_FOUND); }
    private record Attachment(String path, String hash, String type, String status, String position) { }
}
