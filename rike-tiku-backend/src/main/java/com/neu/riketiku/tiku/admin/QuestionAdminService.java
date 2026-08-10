package com.neu.riketiku.tiku.admin;

import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentContentService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionAdminService {
    private static final Set<String> QUESTION_TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK", "SUBJECTIVE");
    private static final Set<String> USAGE_MODES = Set.of("ONLINE_PRACTICE", "TOPIC_LEARNING");
    private static final Set<String> SOURCE_PARTS = Set.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS");
    private static final Set<String> PUBLISHABLE_RIGHTS = Set.of("AUTHORIZED", "OPEN_LICENSE", "PUBLIC_OFFICIAL", "USER_PROVIDED");
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentContentService attachmentContentService;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionAdminService(JdbcTemplate jdbc, QuestionAttachmentContentService attachmentContentService,
            GuanLiCaoZuoRiZhiFuWu auditLog) {
        this.jdbc = jdbc;
        this.attachmentContentService = attachmentContentService;
        this.auditLog = auditLog;
    }

    @Transactional(readOnly = true)
    public QuestionDtos.Page page(long page, long size, String subject, String type, String mode, Integer difficulty, String status, String keyword, String rights) {
        List<Object> arguments = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE q.yi_shan_chu=0");
        equal(where, arguments, "s.ke_mu_dai_ma", subject);
        equal(where, arguments, "q.ti_mu_lei_xing", type);
        equal(where, arguments, "q.shi_yong_mo_shi", mode);
        if (difficulty != null) { where.append(" AND q.nan_du=?"); arguments.add(difficulty); }
        equal(where, arguments, "q.zhuang_tai", status);
        equal(where, arguments, "src.quan_li_zhuang_tai", rights);
        if (keyword != null && !keyword.isBlank()) { where.append(" AND q.ti_gan LIKE ?"); arguments.add("%" + keyword.trim() + "%"); }
        String joins = " FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id LEFT JOIN ti_mu_lai_yuan src ON src.ti_mu_id=q.id AND src.nei_rong_lei_xing='QUESTION' AND src.yi_shan_chu=0";
        long total = jdbc.queryForObject("SELECT COUNT(*)" + joins + where, arguments.toArray(), Long.class);
        List<Object> queryArguments = new ArrayList<>(arguments); queryArguments.add(size); queryArguments.add((page - 1) * size);
        List<QuestionDtos.Item> records = jdbc.query("SELECT q.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_mu_lei_xing,q.shi_yong_mo_shi,LEFT(q.ti_gan,120),q.nan_du,q.shi_fou_ke_zi_dong_pan_fen,q.zhuang_tai,src.quan_li_zhuang_tai,q.chuang_jian_shi_jian,q.geng_xin_shi_jian" + joins + where + " ORDER BY q.id DESC LIMIT ? OFFSET ?", this::mapItem, queryArguments.toArray());
        return new QuestionDtos.Page(records, total, page, size, (total + size - 1) / size);
    }

    @Transactional(readOnly = true)
    public QuestionDtos.Detail detail(Long id) { return detailInternal(id); }

    @Transactional
    public QuestionDtos.Detail create(QuestionDtos.Save request) {
        return create(request, null);
    }

    @Transactional
    public QuestionDtos.Detail create(QuestionDtos.Save request, Long creatorId) {
        return auditLog.audited("QUESTION", "CREATE", null, "管理员创建题目", () -> createInternal(request, creatorId), result -> result.question().id());
    }

    private QuestionDtos.Detail createInternal(QuestionDtos.Save request, Long creatorId) {
        if (creatorId != null && count("SELECT COUNT(*) FROM yong_hu WHERE id=? AND yi_shan_chu=0", creatorId) == 0) {
            fail("CURRENT_USER_UNAVAILABLE", "当前管理员不可用", HttpStatus.UNAUTHORIZED);
        }
        validateRequest(request); validateSubject(request.subjectId()); validateKnowledgePoints(request.subjectId(), request.knowledgePointIds());
        String hash = calculateContentHash(request); rejectDuplicate(request.subjectId(), hash, 0);
        jdbc.update("INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (?,?,?,?,CAST(? AS JSON),?,?,?,'DRAFT',?)", request.subjectId(), request.questionType(), request.usageMode(), request.stem().trim(), request.correctAnswer(), request.difficulty(), blank(request.difficultyDescription()), request.autoGradable(), hash);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); replaceChildren(id, request); return detailInternal(id);
    }

    @Transactional
    public QuestionDtos.Detail update(Long id, QuestionDtos.Save request) {
        return auditLog.audited("QUESTION", "UPDATE", id, "管理员修改题目内容", () -> updateInternal(id, request));
    }

    @Transactional
    public QuestionDtos.Detail updateSourceRights(Long id, QuestionDtos.SourceRightsUpdate request, Long actorId) {
        return auditLog.audited("QUESTION", "UPDATE_SOURCE_RIGHTS", id, "管理员补充题目来源权利依据",
                () -> updateSourceRightsInternal(id, request), result -> result.question().id());
    }

    private QuestionDtos.Detail updateSourceRightsInternal(Long id, QuestionDtos.SourceRightsUpdate request) {
        String status = request.rightsStatus().trim();
        String basis = request.rightsBasis().trim();
        if (!Set.of("AUTHORIZED", "OPEN_LICENSE", "PUBLIC_OFFICIAL", "USER_PROVIDED", "COPYRIGHT_UNKNOWN", "RESTRICTED").contains(status)) {
            fail("QUESTION_RIGHTS_STATUS_INVALID", "来源权利状态不受支持", HttpStatus.BAD_REQUEST);
        }
        if (PUBLISHABLE_RIGHTS.contains(status) && basis.isBlank()) {
            fail("QUESTION_RIGHTS_BASIS_REQUIRED", "可发布权利状态必须填写权利依据", HttpStatus.BAD_REQUEST);
        }
        requireEditableReviewStatus(id);
        if (count("SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id=? AND yi_shan_chu=0", id) != 3) {
            fail("QUESTION_SOURCE_INCOMPLETE", "题目来源信息不完整", HttpStatus.BAD_REQUEST);
        }
        jdbc.update("UPDATE ti_mu_lai_yuan SET quan_li_zhuang_tai=?,quan_li_yi_ju=? WHERE ti_mu_id=? AND yi_shan_chu=0", status, basis, id);
        return detailInternal(id);
    }

    private QuestionDtos.Detail updateInternal(Long id, QuestionDtos.Save request) {
        requireStatus(id, "DRAFT"); validateRequest(request); validateSubject(request.subjectId()); validateKnowledgePoints(request.subjectId(), request.knowledgePointIds());
        String hash = calculateContentHash(request); rejectDuplicate(request.subjectId(), hash, id);
        jdbc.update("UPDATE ti_mu SET ke_mu_id=?,ti_mu_lei_xing=?,shi_yong_mo_shi=?,ti_gan=?,zheng_que_da_an=CAST(? AS JSON),nan_du=?,nan_du_shuo_ming=?,shi_fou_ke_zi_dong_pan_fen=?,nei_rong_ha_xi=? WHERE id=?", request.subjectId(), request.questionType(), request.usageMode(), request.stem().trim(), request.correctAnswer(), request.difficulty(), blank(request.difficultyDescription()), request.autoGradable(), hash, id);
        replaceChildren(id, request); return detailInternal(id);
    }

    @Transactional
    public QuestionDtos.Detail transition(Long id, String action, String expected, String target, String opinion, Long reviewerId) {
        return auditLog.audited("QUESTION", action, id, "题目审核、发布或状态变更", () -> {
            requireStatus(id, expected);
            if ("REJECTED".equals(action) && blank(opinion) == null) fail("REVIEW_OPINION_REQUIRED", "退回必须填写审核意见", HttpStatus.BAD_REQUEST);
            if ("SUBMITTED".equals(action)) validateComplete(id);
            if ("APPROVED".equals(action)) validatePublishableSources(id);
            jdbc.update("UPDATE ti_mu SET zhuang_tai=? WHERE id=?", target, id);
            jdbc.update("UPDATE ti_mu_jie_xi SET zhuang_tai=? WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND ban_ben_hao=1 AND yi_shan_chu=0", target, id);
            jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu(ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,?,?,?,?,?)", id, action, expected, target, reviewerId, blank(opinion));
            return detailInternal(id);
        });
    }

    @Transactional(readOnly = true)
    public List<QuestionDtos.KnowledgePoint> knowledgePoints(Long subjectId) {
        return jdbc.query("SELECT id,wan_zheng_lu_jing,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0 ORDER BY pai_xu,id", (rs, row) -> new QuestionDtos.KnowledgePoint(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), subjectId);
    }

    private void validateRequest(QuestionDtos.Save request) { validateQuestionType(request); validateOptions(request); validateSources(request.sources()); }
    private void validateQuestionType(QuestionDtos.Save request) {
        if (!QUESTION_TYPES.contains(request.questionType()) || !USAGE_MODES.contains(request.usageMode())) fail("QUESTION_TYPE_INVALID", "题型或使用模式不支持", HttpStatus.BAD_REQUEST);
        if (request.difficulty() < 1 || request.difficulty() > 3) fail("QUESTION_DIFFICULTY_INVALID", "难度必须为1至3", HttpStatus.BAD_REQUEST);
        if ("SUBJECTIVE".equals(request.questionType()) && (!"TOPIC_LEARNING".equals(request.usageMode()) || request.autoGradable())) fail("QUESTION_RULE_INVALID", "主观题必须为专题学习且不可自动判分", HttpStatus.BAD_REQUEST);
        JsonNode answer = readAnswer(request.correctAnswer());
        if (!answer.isObject()) fail("QUESTION_ANSWER_INVALID", "正确答案必须为JSON对象", HttpStatus.BAD_REQUEST);
        if ("FILL_BLANK".equals(request.questionType())) validateFillBlankAnswer(answer);
        if ("SUBJECTIVE".equals(request.questionType()) && !answer.path("schemaVersion").canConvertToInt()) {
            fail("QUESTION_ANSWER_INVALID", "主观题答案必须包含schemaVersion", HttpStatus.BAD_REQUEST);
        }
    }
    private void validateOptions(QuestionDtos.Save request) {
        List<QuestionDtos.Option> options = request.options() == null ? List.of() : request.options();
        if ("SINGLE_CHOICE".equals(request.questionType()) || "MULTIPLE_CHOICE".equals(request.questionType())) {
            if (options.size() < 2) fail("QUESTION_OPTIONS_INVALID", "选择题至少需要两个选项", HttpStatus.BAD_REQUEST);
            Set<String> labels = new HashSet<>(); long correct = 0;
            for (QuestionDtos.Option option : options) { if (blank(option.label()) == null || blank(option.content()) == null || !labels.add(option.label())) fail("QUESTION_OPTIONS_INVALID", "选项标识必须唯一且内容不能为空", HttpStatus.BAD_REQUEST); if (option.correct()) correct++; }
            Set<String> answerLabels = answerLabels(readAnswer(request.correctAnswer()), request.questionType());
            if ("SINGLE_CHOICE".equals(request.questionType()) && (correct != 1 || answerLabels.size() != 1)) fail("QUESTION_ANSWER_INVALID", "单选题必须且只能有一个正确选项", HttpStatus.BAD_REQUEST);
            if ("MULTIPLE_CHOICE".equals(request.questionType()) && correct < 2) fail("QUESTION_ANSWER_INVALID", "多选题至少两个正确选项", HttpStatus.BAD_REQUEST);
            if (!answerLabels.equals(correctLabels(options))) fail("QUESTION_ANSWER_INVALID", "正确答案必须与正确选项一致", HttpStatus.BAD_REQUEST);
        }
    }
    private void validateSources(List<QuestionDtos.Source> sources) {
        if (sources == null || sources.size() != 3) fail("QUESTION_SOURCE_INCOMPLETE", "必须提供题干、答案、标准解析来源", HttpStatus.BAD_REQUEST);
        Set<String> parts = new HashSet<>();
        for (QuestionDtos.Source source : sources) if (blank(source.contentType()) == null || blank(source.sourceType()) == null || blank(source.sourceName()) == null || blank(source.rightsStatus()) == null || !SOURCE_PARTS.contains(source.contentType()) || !parts.add(source.contentType())) fail("QUESTION_SOURCE_INCOMPLETE", "来源字段不完整或部分重复", HttpStatus.BAD_REQUEST);
        if (!parts.equals(SOURCE_PARTS)) fail("QUESTION_SOURCE_INCOMPLETE", "来源必须覆盖题干、答案、标准解析", HttpStatus.BAD_REQUEST);
    }
    private void validateSubject(Long subjectId) { if (count("SELECT COUNT(*) FROM ke_mu WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", subjectId) == 0) fail("SUBJECT_UNAVAILABLE", "科目不存在或已停用", HttpStatus.BAD_REQUEST); }
    private void validateKnowledgePoints(Long subjectId, List<Long> ids) { if (ids == null || ids.isEmpty()) fail("QUESTION_KNOWLEDGE_POINT_REQUIRED", "至少选择一个知识点", HttpStatus.BAD_REQUEST); if (new HashSet<>(ids).size() != ids.size() || count("SELECT COUNT(*) FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0 AND id IN (" + placeholders(ids.size()) + ")", join(subjectId, ids)) != ids.size()) fail("QUESTION_KNOWLEDGE_POINT_INVALID", "知识点不存在、已停用或不属于当前科目", HttpStatus.BAD_REQUEST); }
    private void replaceChildren(Long id, QuestionDtos.Save request) {
        jdbc.update("DELETE FROM ti_mu_xuan_xiang WHERE ti_mu_id=?", id); jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id=?", id); jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", id); jdbc.update("DELETE FROM ti_mu_lai_yuan WHERE ti_mu_id=?", id);
        int order = 1; for (QuestionDtos.Option option : request.options() == null ? List.<QuestionDtos.Option>of() : request.options()) jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)", id, option.label(), option.content(), option.correct(), order++);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'DRAFT')", id, request.standardAnalysis());
        order = 1; for (Long pointId : request.knowledgePointIds()) jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,?,?)", id, pointId, order == 1, order++);
        for (QuestionDtos.Source source : request.sources()) jdbc.update("INSERT INTO ti_mu_lai_yuan(ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,lai_yuan_di_zhi,nian_fen,di_qu,shi_juan_ming_cheng,ti_hao,quan_li_zhuang_tai,quan_li_yi_ju) VALUES (?,?,?,?,?,?,?,?,?,?,?)", id, source.contentType(), source.sourceType(), source.sourceName(), blank(source.sourceAddress()), source.year(), blank(source.region()), blank(source.paperName()), blank(source.questionNumber()), source.rightsStatus(), blank(source.rightsBasis()));
    }
    private void validateComplete(Long id) { if (count("SELECT COUNT(*) FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND yi_shan_chu=0", id) == 0 || count("SELECT COUNT(*) FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=? AND yi_shan_chu=0", id) == 0 || count("SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id=? AND yi_shan_chu=0", id) != 3) fail("QUESTION_INCOMPLETE", "提交审核前必须补全解析、知识点和来源", HttpStatus.BAD_REQUEST); }
    private void validatePublishableSources(Long id) { validateComplete(id); if (count("SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id=? AND quan_li_zhuang_tai NOT IN ('AUTHORIZED','OPEN_LICENSE','PUBLIC_OFFICIAL','USER_PROVIDED') AND yi_shan_chu=0", id) > 0) fail("QUESTION_RIGHTS_UNAVAILABLE", "来源权利状态不允许发布", HttpStatus.CONFLICT); }
    private QuestionDtos.Detail detailInternal(Long id) {
        List<QuestionDtos.Item> items = jdbc.query("SELECT q.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,q.ti_mu_lei_xing,q.shi_yong_mo_shi,LEFT(q.ti_gan,120),q.nan_du,q.shi_fou_ke_zi_dong_pan_fen,q.zhuang_tai,(SELECT quan_li_zhuang_tai FROM ti_mu_lai_yuan WHERE ti_mu_id=q.id AND nei_rong_lei_xing='QUESTION'),q.chuang_jian_shi_jian,q.geng_xin_shi_jian FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.id=? AND q.yi_shan_chu=0", this::mapItem, id);
        if (items.isEmpty()) fail("QUESTION_NOT_FOUND", "题目不存在", HttpStatus.NOT_FOUND);
        QuestionDtos.Item item = items.getFirst();
        List<QuestionDtos.Option> options = jdbc.query("SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu", (rs, row) -> new QuestionDtos.Option(rs.getString(1), rs.getString(2), rs.getBoolean(3)), id);
        String answer = jdbc.queryForObject("SELECT zheng_que_da_an FROM ti_mu WHERE id=?", String.class, id);
        String analysis = jdbc.query("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND yi_shan_chu=0 ORDER BY ban_ben_hao DESC", rs -> rs.next() ? rs.getString(1) : "", id);
        List<QuestionDtos.KnowledgePoint> points = jdbc.query("SELECT k.id,k.wan_zheng_lu_jing,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing FROM ti_mu_zhi_shi_dian x JOIN zhi_shi_dian k ON k.id=x.zhi_shi_dian_id WHERE x.ti_mu_id=? AND x.yi_shan_chu=0", (rs, row) -> new QuestionDtos.KnowledgePoint(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), id);
        List<QuestionDtos.Source> sources = jdbc.query("SELECT nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,quan_li_zhuang_tai,lai_yuan_di_zhi,nian_fen,di_qu,shi_juan_ming_cheng,ti_hao,quan_li_yi_ju FROM ti_mu_lai_yuan WHERE ti_mu_id=? AND yi_shan_chu=0", (rs, row) -> new QuestionDtos.Source(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), (Integer) rs.getObject(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10)), id);
        List<QuestionDtos.Attachment> attachments = jdbc.query("SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,zhuang_tai,xiang_dui_lu_jing,nei_rong_ha_xi FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu", (rs, row) -> {
            long attachmentId = rs.getLong(1);
            String renderStatus = attachmentContentService.renderStatus(rs.getString(7), rs.getString(8), rs.getString(3), rs.getString(6));
            return new QuestionDtos.Attachment(attachmentId, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), renderStatus,
                    "AVAILABLE".equals(renderStatus) ? "/api/v1/admin/question-attachments/" + attachmentId + "/content" : null);
        }, id);
        List<QuestionDtos.Review> reviews = jdbc.query("SELECT id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian,chuang_jian_shi_jian FROM ti_mu_shen_he_ji_lu WHERE ti_mu_id=? ORDER BY id", (rs, row) -> new QuestionDtos.Review(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), (Long) rs.getObject(5), rs.getString(6), rs.getObject(7, LocalDateTime.class)), id);
        String stem = jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?", String.class, id);
        return new QuestionDtos.Detail(item, stem, answer, options, analysis, points, sources, attachments, reviews, allowedActions(item.status()));
    }
    private List<String> allowedActions(String status) {
        return switch (status) { case "DRAFT" -> List.of("SUBMIT"); case "PENDING" -> List.of("APPROVE", "RETURN"); case "PUBLISHED" -> List.of("DISABLE"); case "DISABLED" -> List.of("REPUBLISH"); default -> List.of(); };
    }
    private void requireStatus(Long id, String expected) { String actual = jdbc.query("SELECT zhuang_tai FROM ti_mu WHERE id=? AND yi_shan_chu=0", rs -> rs.next() ? rs.getString(1) : null, id); if (actual == null) fail("QUESTION_NOT_FOUND", "题目不存在", HttpStatus.NOT_FOUND); if (!expected.equals(actual)) fail("QUESTION_STATUS_INVALID", "当前题目状态不允许此操作", HttpStatus.CONFLICT); }
    private void requireEditableReviewStatus(Long id) {
        String actual = jdbc.query("SELECT zhuang_tai FROM ti_mu WHERE id=? AND yi_shan_chu=0", rs -> rs.next() ? rs.getString(1) : null, id);
        if (actual == null) fail("QUESTION_NOT_FOUND", "题目不存在", HttpStatus.NOT_FOUND);
        if (!Set.of("DRAFT", "PENDING").contains(actual)) fail("QUESTION_STATUS_INVALID", "当前题目状态不允许修改来源权利", HttpStatus.CONFLICT);
    }
    private void rejectDuplicate(Long subjectId, String hash, long excludedId) { if (count("SELECT COUNT(*) FROM ti_mu WHERE ke_mu_id=? AND nei_rong_ha_xi=? AND id<>? AND yi_shan_chu=0", subjectId, hash, excludedId) > 0) fail("QUESTION_DUPLICATE", "存在完全重复题目", HttpStatus.CONFLICT); }
    private String calculateContentHash(QuestionDtos.Save request) {
        try {
            StringBuilder value = new StringBuilder(request.stem().replaceAll("\\s+", ""));
            for (QuestionDtos.Option option : request.options() == null ? List.<QuestionDtos.Option>of() : request.options()) {
                value.append('|').append(option.label().trim()).append(':').append(option.content().replaceAll("\\s+", ""));
            }
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算内容哈希", exception);
        }
    }
    private JsonNode readAnswer(String answer) {
        try {
            return objectMapper.readTree(answer);
        } catch (Exception exception) {
            fail("QUESTION_ANSWER_INVALID", "正确答案必须为合法JSON", HttpStatus.BAD_REQUEST);
            return objectMapper.createObjectNode();
        }
    }
    private Set<String> answerLabels(JsonNode answer, String questionType) {
        JsonNode labels = "SINGLE_CHOICE".equals(questionType) ? answer.path("optionLabels") : answer.path("optionLabels");
        if (!labels.isArray()) fail("QUESTION_ANSWER_INVALID", "选择题答案必须包含optionLabels数组", HttpStatus.BAD_REQUEST);
        Set<String> values = new HashSet<>();
        labels.forEach(node -> values.add(node.asText()));
        return values;
    }
    private Set<String> correctLabels(List<QuestionDtos.Option> options) {
        Set<String> labels = new HashSet<>();
        options.stream().filter(QuestionDtos.Option::correct).forEach(option -> labels.add(option.label()));
        return labels;
    }
    private void validateFillBlankAnswer(JsonNode answer) {
        JsonNode blanks = answer.path("blanks");
        if (!blanks.isArray() || blanks.isEmpty()) fail("QUESTION_ANSWER_INVALID", "填空题答案必须包含至少一个空位", HttpStatus.BAD_REQUEST);
        for (JsonNode blank : blanks) {
            JsonNode accepted = blank.path("acceptedAnswers");
            boolean hasBlankAnswer = false;
            if (accepted.isArray()) {
                for (JsonNode candidate : accepted) {
                    if (candidate.asText().isBlank()) {
                        hasBlankAnswer = true;
                    }
                }
            }
            if (!accepted.isArray() || accepted.isEmpty() || hasBlankAnswer) {
                fail("QUESTION_ANSWER_INVALID", "每个填空空位至少需要一个可接受答案", HttpStatus.BAD_REQUEST);
            }
        }
    }
    private QuestionDtos.Item mapItem(java.sql.ResultSet rs, int row) throws java.sql.SQLException { return new QuestionDtos.Item(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getBoolean(8), rs.getString(9), rs.getString(10), rs.getObject(11, LocalDateTime.class), rs.getObject(12, LocalDateTime.class)); }
    private long count(String sql, Object... args) { Long value = jdbc.queryForObject(sql, args, Long.class); return value == null ? 0 : value; }
    private void equal(StringBuilder where, List<Object> args, String field, String value) { if (value != null && !value.isBlank()) { where.append(" AND ").append(field).append("=?"); args.add(value.trim()); } }
    private Object[] join(Long subjectId, List<Long> ids) { List<Object> values = new ArrayList<>(); values.add(subjectId); values.addAll(ids); return values.toArray(); }
    private String placeholders(int count) { return String.join(",", java.util.Collections.nCopies(count, "?")); }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void fail(String code, String message, HttpStatus status) { throw new RenZhengYeWuYiChang(code, message, status); }
}
