package com.neu.riketiku.shijuan;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.QuestionDisplayTextNormalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaperService {
    private final JdbcTemplate jdbc;
    private final QuestionDisplayTextNormalizer textNormalizer;

    public PaperService(JdbcTemplate jdbc, QuestionDisplayTextNormalizer textNormalizer) {
        this.jdbc = jdbc;
        this.textNormalizer = textNormalizer;
    }

    @Transactional
    public PaperDtos.Paper save(long userId, PaperDtos.Save request) {
        long teacher = teacher(userId, request.subjectId());
        if (!Set.of("MANUAL", "RULE").contains(request.mode())
                || request.items().stream().map(PaperDtos.ItemInput::questionId).distinct().count() != request.items().size()) {
            bad("PAPER_ITEMS_INVALID", "试卷题目不合法");
        }
        validateQuestions(userId, request.subjectId(), request.items().stream().map(PaperDtos.ItemInput::questionId).toList());
        jdbc.update("INSERT INTO shi_juan(chuang_jian_jiao_shi_id,ke_mu_id,shi_juan_ming_cheng,zu_juan_mo_shi) VALUES (?,?,?,?)",
                teacher, request.subjectId(), request.name().trim(), request.mode());
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        int order = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (PaperDtos.ItemInput item : request.items()) {
            jdbc.update("INSERT INTO shi_juan_ti_mu(shi_juan_id,ti_mu_id,ti_mu_shun_xu,fen_zhi) VALUES (?,?,?,?)",
                    id, item.questionId(), order++, item.score());
            total = total.add(item.score());
        }
        jdbc.update("UPDATE shi_juan SET zong_fen=?,zhuang_tai='READY' WHERE id=?", total, id);
        return detail(userId, id);
    }

    public PaperDtos.Paper rule(long userId, PaperDtos.Rule request) {
        teacher(userId, request.subjectId());
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT q.id FROM ti_mu q");
        if (request.knowledgePointIds() != null && !request.knowledgePointIds().isEmpty()) {
            sql.append(" JOIN ti_mu_zhi_shi_dian qp ON qp.ti_mu_id=q.id AND qp.zhi_shi_dian_id IN (")
                    .append(String.join(",", Collections.nCopies(request.knowledgePointIds().size(), "?"))).append(")");
            args.addAll(request.knowledgePointIds());
        }
        sql.append(" WHERE q.ke_mu_id=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 AND q.shi_fou_ke_zi_dong_pan_fen=1")
                .append(" AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (SELECT 1 FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=q.ren_ke_guan_xi_id AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE'))");
        args.add(request.subjectId());
        args.add(userId);
        if (request.questionTypes() != null && !request.questionTypes().isEmpty()) {
            sql.append(" AND q.ti_mu_lei_xing IN (").append(String.join(",", Collections.nCopies(request.questionTypes().size(), "?"))).append(")");
            args.addAll(request.questionTypes());
        }
        if (request.difficulties() != null && !request.difficulties().isEmpty()) {
            sql.append(" AND q.nan_du IN (").append(String.join(",", Collections.nCopies(request.difficulties().size(), "?"))).append(")");
            args.addAll(request.difficulties());
        }
        sql.append(" ORDER BY RAND() LIMIT ?");
        args.add(request.count());
        List<Long> ids = jdbc.query(sql.toString(), (rs, row) -> rs.getLong(1), args.toArray());
        if (ids.size() < request.count()) {
            throw new RenZhengYeWuYiChang("PAPER_POOL_INSUFFICIENT", "符合规则的已发布题目还缺 " + (request.count() - ids.size()) + " 道", HttpStatus.CONFLICT);
        }
        BigDecimal each = request.totalScore().divide(BigDecimal.valueOf(request.count()), 2, RoundingMode.DOWN);
        BigDecimal last = request.totalScore().subtract(each.multiply(BigDecimal.valueOf(request.count() - 1)));
        List<PaperDtos.ItemInput> items = new ArrayList<>();
        for (int index = 0; index < ids.size(); index++) {
            items.add(new PaperDtos.ItemInput(ids.get(index), index == ids.size() - 1 ? last : each));
        }
        return save(userId, new PaperDtos.Save(request.subjectId(), request.name(), "RULE", items));
    }

    public PaperDtos.Paper random(long userId, PaperDtos.Rule request) {
        return rule(userId, new PaperDtos.Rule(request.subjectId(), request.name(), List.of(), request.questionTypes(),
                request.difficulties(), request.count(), request.totalScore()));
    }

    @Transactional(readOnly = true)
    public List<PaperDtos.ListItem> list(long userId) {
        return jdbc.query("SELECT p.id,p.ke_mu_id,p.shi_juan_ming_cheng,p.zu_juan_mo_shi,p.zong_fen,p.zhuang_tai,COUNT(i.id) FROM shi_juan p JOIN jiao_shi_dang_an j ON j.id=p.chuang_jian_jiao_shi_id LEFT JOIN shi_juan_ti_mu i ON i.shi_juan_id=p.id WHERE j.yong_hu_id=? AND p.yi_shan_chu=0 GROUP BY p.id ORDER BY p.id DESC",
                (rs, row) -> new PaperDtos.ListItem(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5), rs.getString(6), rs.getInt(7)), userId);
    }

    @Transactional(readOnly = true)
    public List<PaperDtos.KnowledgePoint> knowledgePoints(long userId, long subjectId) {
        teacher(userId, subjectId);
        return jdbc.query("SELECT id,wan_zheng_lu_jing FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0 ORDER BY pai_xu,id",
                (rs, row) -> new PaperDtos.KnowledgePoint(rs.getLong(1), rs.getString(2)), subjectId);
    }

    @Transactional(readOnly = true)
    public List<PaperDtos.QuestionOption> questions(long userId, long subjectId, Long pointId, String type,
                                                     Integer difficulty, String keyword) {
        return questions(userId, subjectId, null, pointId, type, difficulty, keyword);
    }

    @Transactional(readOnly = true)
    public List<PaperDtos.QuestionOption> questions(long userId, long subjectId, Long teachingScopeId, Long pointId, String type,
                                                     Integer difficulty, String keyword) {
        teacher(userId, subjectId);
        if (teachingScopeId != null) teacherScope(userId, teachingScopeId, subjectId);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT q.id,q.ti_mu_lei_xing,q.ti_gan,q.nan_du,q.shi_yong_mo_shi,q.zhuan_ti_lei_xing FROM ti_mu q");
        if (pointId != null) {
            sql.append(" JOIN ti_mu_zhi_shi_dian qp ON qp.ti_mu_id=q.id AND qp.zhi_shi_dian_id=? AND qp.yi_shan_chu=0");
        }
        sql.append(" WHERE q.ke_mu_id=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0")
                .append(" AND (q.ke_jian_fan_wei='GLOBAL' OR ")
                .append(teachingScopeId == null
                        ? "EXISTS (SELECT 1 FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=q.ren_ke_guan_xi_id AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE')"
                        : "q.ren_ke_guan_xi_id=?").append(")");
        if (pointId != null) args.add(pointId);
        args.add(subjectId);
        if (teachingScopeId == null) args.add(userId); else args.add(teachingScopeId);
        if (type != null && !type.isBlank()) {
            sql.append(" AND q.ti_mu_lei_xing=?");
            args.add(type.trim());
        }
        if (difficulty != null) {
            sql.append(" AND q.nan_du=?");
            args.add(difficulty);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND q.ti_gan LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY q.id DESC LIMIT 200");
        return jdbc.query(sql.toString(), (rs, row) -> {
            long questionId = rs.getLong(1);
            return new PaperDtos.QuestionOption(questionId, rs.getString(2), textNormalizer.normalize(rs.getString(3)),
                    rs.getInt(4), rs.getString(5), rs.getString(6), points(questionId), searchAttachments(questionId));
        }, args.toArray());
    }

    @Transactional(readOnly = true)
    public PaperDtos.Paper detail(long userId, long id) {
        Base paper = base(userId, id);
        List<PaperDtos.Question> questions = jdbc.query("""
                SELECT q.id,i.ti_mu_shun_xu,i.fen_zhi,q.ti_mu_lei_xing,q.ti_gan,q.nan_du,q.shi_yong_mo_shi,q.zhuan_ti_lei_xing,
                       CAST(q.zheng_que_da_an AS CHAR),a.jie_xi_nei_rong
                FROM shi_juan_ti_mu i JOIN ti_mu q ON q.id=i.ti_mu_id
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD'
                WHERE i.shi_juan_id=? ORDER BY i.ti_mu_shun_xu
                """, (rs, row) -> {
            long questionId = rs.getLong(1);
            return new PaperDtos.Question(questionId, rs.getInt(2), rs.getBigDecimal(3), rs.getString(4),
                    textNormalizer.normalize(rs.getString(5)), rs.getInt(6), rs.getString(7), rs.getString(8),
                    options(questionId), rs.getString(9), rs.getString(10), points(questionId),
                    attachments(questionId, "QUESTION", id), attachments(questionId, "STANDARD_ANALYSIS", id));
        }, id);
        return new PaperDtos.Paper(paper.id(), paper.subjectId(), paper.subjectName(), paper.name(), paper.mode(),
                paper.score(), paper.status(), questions);
    }

    @Transactional
    public void softDelete(long userId, long paperId) {
        base(userId, paperId);
        Long active = jdbc.queryForObject("SELECT COUNT(*) FROM shi_juan_fa_bu WHERE shi_juan_id=? AND zhuang_tai IN ('PUBLISHED','CLOSED')", Long.class, paperId);
        if (active != null && active > 0) {
            throw new RenZhengYeWuYiChang("PAPER_DELETE_ACTIVE_RELEASE", "该试卷仍发布在班级中，请先撤回全部班级发布。", HttpStatus.CONFLICT);
        }
        jdbc.update("UPDATE shi_juan SET yi_shan_chu=1 WHERE id=?", paperId);
    }

    @Transactional(readOnly = true)
    public PaperAttachmentContent teacherAttachment(long userId, long paperId, long attachmentId) {
        base(userId, paperId);
        return jdbc.query("""
                SELECT f.xiang_dui_lu_jing,f.nei_rong_ha_xi,f.fu_jian_lei_xing,f.zhuang_tai
                FROM ti_mu_fu_jian f JOIN shi_juan_ti_mu i ON i.ti_mu_id=f.ti_mu_id
                WHERE i.shi_juan_id=? AND f.id=? AND f.yi_shan_chu=0
                """, (rs, row) -> new PaperAttachmentContent(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                paperId, attachmentId).stream().findFirst()
                .orElseThrow(() -> new RenZhengYeWuYiChang("PAPER_ATTACHMENT_NOT_FOUND", "试卷附件不存在或无权访问", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PaperAttachmentContent teacherQuestionAttachment(long userId, long questionId, long attachmentId) {
        return jdbc.query("""
                SELECT f.xiang_dui_lu_jing,f.nei_rong_ha_xi,f.fu_jian_lei_xing,f.zhuang_tai
                FROM ti_mu_fu_jian f JOIN ti_mu q ON q.id=f.ti_mu_id
                WHERE q.id=? AND f.id=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 AND f.yi_shan_chu=0
                  AND EXISTS (SELECT 1 FROM jiao_shi_dang_an j JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id
                    WHERE j.yong_hu_id=? AND j.zhuang_tai='ACTIVE' AND r.ke_mu_id=q.ke_mu_id AND r.zhuang_tai='ACTIVE'
                      AND (q.ke_jian_fan_wei='GLOBAL' OR r.id=q.ren_ke_guan_xi_id))
                """, (rs, row) -> new PaperAttachmentContent(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                questionId, attachmentId, userId).stream().findFirst()
                .orElseThrow(() -> new RenZhengYeWuYiChang("PAPER_ATTACHMENT_NOT_FOUND", "试卷附件不存在或无权访问", HttpStatus.NOT_FOUND));
    }

    private Base base(long userId, long paperId) {
        return jdbc.query("SELECT p.id,p.ke_mu_id,k.ke_mu_ming_cheng,p.shi_juan_ming_cheng,p.zu_juan_mo_shi,p.zong_fen,p.zhuang_tai FROM shi_juan p JOIN jiao_shi_dang_an j ON j.id=p.chuang_jian_jiao_shi_id JOIN ke_mu k ON k.id=p.ke_mu_id WHERE p.id=? AND j.yong_hu_id=? AND p.yi_shan_chu=0",
                (rs, row) -> new Base(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getBigDecimal(6), rs.getString(7)), paperId, userId).stream().findFirst()
                .orElseThrow(() -> new RenZhengYeWuYiChang("PAPER_NOT_FOUND", "试卷不存在或无权访问", HttpStatus.NOT_FOUND));
    }

    private long teacher(long userId, long subjectId) {
        return jdbc.query("SELECT DISTINCT j.id FROM jiao_shi_dang_an j JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id WHERE j.yong_hu_id=? AND j.zhuang_tai='ACTIVE' AND j.yi_shan_chu=0 AND r.ke_mu_id=? AND r.zhuang_tai='ACTIVE'",
                (rs, row) -> rs.getLong(1), userId, subjectId).stream().findFirst()
                .orElseThrow(() -> new RenZhengYeWuYiChang("PAPER_SUBJECT_FORBIDDEN", "无权使用该学科组卷", HttpStatus.FORBIDDEN));
    }

    private void teacherScope(long userId, long teachingScopeId, long subjectId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=? AND j.yong_hu_id=? AND r.ke_mu_id=? AND r.zhuang_tai='ACTIVE'", Long.class, teachingScopeId, userId, subjectId);
        if (count == null || count == 0) {
            throw new RenZhengYeWuYiChang("PAPER_SCOPE_FORBIDDEN", "无权使用该任课范围组卷", HttpStatus.FORBIDDEN);
        }
    }

    private void validateQuestions(long userId, long subjectId, List<Long> ids) {
        if (ids.isEmpty()) bad("PAPER_ITEMS_INVALID", "试卷不能为空");
        String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(subjectId);
        args.add(userId);
        args.addAll(ids);
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q WHERE q.ke_mu_id=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                  AND (q.ke_jian_fan_wei='GLOBAL' OR EXISTS (SELECT 1 FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=q.ren_ke_guan_xi_id AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE'))
                  AND q.id IN (%s)
                """.formatted(marks), Long.class, args.toArray());
        if (count != ids.size()) bad("PAPER_QUESTION_FORBIDDEN", "试卷包含未发布、跨科或越权私有题");
    }

    private List<PaperDtos.Option> options(long questionId) {
        return jdbc.query("SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu",
                (rs, row) -> new PaperDtos.Option(rs.getString(1), rs.getString(2)), questionId);
    }

    private List<String> points(long questionId) {
        return jdbc.query("SELECT p.zhi_shi_dian_ming_cheng FROM ti_mu_zhi_shi_dian qp JOIN zhi_shi_dian p ON p.id=qp.zhi_shi_dian_id WHERE qp.ti_mu_id=? AND qp.yi_shan_chu=0 ORDER BY qp.pai_xu",
                (rs, row) -> rs.getString(1), questionId);
    }

    private List<PaperDtos.Attachment> attachments(long questionId, String position, long paperId) {
        return jdbc.query("""
                SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,
                       COALESCE(dui_xiang_biao_shi,''),pai_xu
                FROM ti_mu_fu_jian WHERE ti_mu_id=? AND guan_lian_wei_zhi=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY pai_xu,id
                """, (rs, row) -> new PaperDtos.Attachment(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7),
                "/api/v1/teacher/papers/" + paperId + "/attachments/" + rs.getLong(1) + "/content"), questionId, position);
    }

    private List<PaperDtos.Attachment> searchAttachments(long questionId) {
        return jdbc.query("""
                SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,
                       COALESCE(dui_xiang_biao_shi,''),pai_xu
                FROM ti_mu_fu_jian WHERE ti_mu_id=? AND guan_lian_wei_zhi='QUESTION' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY pai_xu,id
                """, (rs, row) -> new PaperDtos.Attachment(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7),
                "/api/v1/teacher/papers/questions/" + questionId + "/attachments/" + rs.getLong(1) + "/content"), questionId);
    }

    private void bad(String code, String message) {
        throw new RenZhengYeWuYiChang(code, message, HttpStatus.BAD_REQUEST);
    }

    public record PaperAttachmentContent(String relativePath, String hash, String type, String status) { }
    private record Base(long id, long subjectId, String subjectName, String name, String mode, BigDecimal score, String status) { }
}
