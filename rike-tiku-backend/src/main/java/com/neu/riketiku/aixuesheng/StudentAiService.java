package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderException;
import com.neu.riketiku.ai.vision.VisionContextService;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class StudentAiService {
    public static final int MAX_ROUNDS = 10;
    private static final int RECENT_MESSAGES = 12;
    private static final int HISTORY_CHAR_BUDGET = 6000;
    private static final int MAX_ASSISTANT_CHARS = 2000;
    private static final Pattern SECRET_OR_OVERRIDE = Pattern.compile(
            "(?i)(api\\s*key|system\\s*prompt|系统提示词|数据库密码|忽略.{0,12}(规则|指令)|不要按标准答案|改成我的答案正确|告诉我.{0,8}(密钥|密码))");
    private static final Pattern PRODUCT_IDENTITY = Pattern.compile("(你是谁|你叫什么|你的名字|你是干什么的)");
    private static final Pattern MODEL_IDENTITY = Pattern.compile(
            "(?i)(你(是|是不是|用|使用).{0,8}(deepseek|glm|什么模型|哪个模型|什么\\s*api)|底层.{0,8}(模型|provider)|模型(代码|id)|api地址)");
    private static final Pattern PROVIDER_DISCLOSURE = Pattern.compile(
            "(?i)(deepseek|glm[-_ ]?4|provider\\s*code|model\\s*(code|id)|api\\.deepseek|open\\.bigmodel|api\\s*key|token\\s*(usage|count))");
    private final JdbcTemplate jdbc;
    private final StudentAiProviderClient provider;
    private final StudentAiPromptFactory prompts;
    private final StudentAiAnalysisParser parser;
    private final VisionContextService visionContexts;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentAiService(JdbcTemplate jdbc, StudentAiProviderClient provider,
                            StudentAiPromptFactory prompts, StudentAiAnalysisParser parser,
                            VisionContextService visionContexts) {
        this.jdbc = jdbc;
        this.provider = provider;
        this.prompts = prompts;
        this.parser = parser;
        this.visionContexts = visionContexts;
    }

    @Transactional(readOnly = true)
    public StudentAiDtos.Analysis analysis(Long userId, Long answerFactId) {
        StudentAiFact fact = requireFact(userId, answerFactId);
        return findAnalysis(fact, false);
    }

    @Transactional(noRollbackFor = RenZhengYeWuYiChang.class)
    public StudentAiDtos.Analysis generateAnalysis(Long userId, Long answerFactId) {
        StudentAiFact fact = requireFact(userId, answerFactId);
        if (fact.correct()) {
            throw failure("AI_ANALYSIS_ONLY_FOR_WRONG_ANSWER", "答对的题目无需生成错因分析", HttpStatus.CONFLICT);
        }
        String hash = factHash(fact);
        StudentAiDtos.Analysis existing = findAnalysis(fact, true);
        if ("SUCCESS".equals(existing.status()) && hash.equals(findAnalysisHash(fact.answerFactId()))) {
            return withCached(existing);
        }
        int inserted = jdbc.update("""
                INSERT IGNORE INTO ai_cuo_ti_fen_xi
                  (xue_sheng_da_ti_id,xue_sheng_id,prompt_ban_ben,shu_ru_shi_shi_ha_xi,zhuang_tai)
                VALUES (?,?,?,?, 'GENERATING')
                """, fact.answerFactId(), fact.studentId(), StudentAiPromptFactory.PROMPT_VERSION, hash);
        if (inserted == 0) {
            AnalysisLock locked = jdbc.query("""
                    SELECT zhuang_tai,shu_ru_shi_shi_ha_xi FROM ai_cuo_ti_fen_xi
                    WHERE xue_sheng_da_ti_id=? AND xue_sheng_id=? FOR UPDATE
                    """, (rs, n) -> new AnalysisLock(rs.getString(1), rs.getString(2)),
                    fact.answerFactId(), fact.studentId()).stream().findFirst().orElseThrow(this::notFound);
            if ("SUCCESS".equals(locked.status()) && hash.equals(locked.hash())) {
                return withCached(findAnalysis(fact, false));
            }
            jdbc.update("""
                    UPDATE ai_cuo_ti_fen_xi SET prompt_ban_ben=?,shu_ru_shi_shi_ha_xi=?,zhuang_tai='GENERATING',
                      cuo_wu_lei_xing=NULL,cuo_wu_yuan_yin=NULL,zheng_que_si_lu=NULL,chang_jian_cuo_wu=NULL,
                      fu_xi_jian_yi=NULL,provider_dai_ma=NULL,model_dai_ma=NULL,cuo_wu_dai_ma=NULL
                    WHERE xue_sheng_da_ti_id=? AND xue_sheng_id=?
                    """, StudentAiPromptFactory.PROMPT_VERSION, hash, fact.answerFactId(), fact.studentId());
        }
        try {
            String visionContext = visionContext(fact.questionId());
            AiModelResult result = provider.generate(prompts.analysis(fact, false, visionContext));
            StudentAiAnalysisParser.ParsedAnalysis parsed;
            try {
                parsed = parser.parse(result.content());
            } catch (StudentAiAnalysisParser.InvalidAnalysisException firstInvalid) {
                AiModelResult corrected = provider.generate(prompts.analysis(fact, true, visionContext));
                try {
                    parsed = parser.parse(corrected.content());
                    result = corrected;
                } catch (StudentAiAnalysisParser.InvalidAnalysisException secondInvalid) {
                    markAnalysisFailed(fact.answerFactId(), "INVALID_RESPONSE");
                    throw failure("AI_INVALID_RESPONSE", "AI 暂时无法生成有效分析，STANDARD 解析不受影响", HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
            jdbc.update("""
                    UPDATE ai_cuo_ti_fen_xi SET cuo_wu_lei_xing=?,cuo_wu_yuan_yin=?,zheng_que_si_lu=?,
                      chang_jian_cuo_wu=?,fu_xi_jian_yi=?,provider_dai_ma=?,model_dai_ma=?,zhuang_tai='SUCCESS',cuo_wu_dai_ma=NULL
                    WHERE xue_sheng_da_ti_id=? AND xue_sheng_id=?
                    """, parsed.errorType().name(), parsed.errorReason(), parsed.correctThinking(),
                    objectMapper.writeValueAsString(parsed.commonMistakes()),
                    objectMapper.writeValueAsString(parsed.reviewSuggestions()),
                    safeCode(result.providerCode()), safeCode(result.modelCode()), fact.answerFactId(), fact.studentId());
            return findAnalysis(fact, false);
        } catch (AiProviderException exception) {
            markAnalysisFailed(fact.answerFactId(), exception.errorType().name());
            throw providerFailure(exception);
        }
    }

    @Transactional
    public StudentAiDtos.Conversation createConversation(Long userId, Long answerFactId) {
        StudentAiFact fact = requireFact(userId, answerFactId);
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_hui_hua (xue_sheng_id,xue_sheng_da_ti_id,lian_xi_ti_mu_id,zhuang_tai,lei_ji_lun_shu)
                    VALUES (?,?,?,'ACTIVE',0)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, fact.studentId());
            statement.setLong(2, fact.answerFactId());
            statement.setLong(3, fact.practiceQuestionId());
            return statement;
        }, holder);
        return conversation(userId, holder.getKey().longValue());
    }

    @Transactional(readOnly = true)
    public StudentAiDtos.Conversation conversation(Long userId, Long conversationId) {
        ConversationRow row = requireConversation(userId, conversationId, false);
        return toConversation(row, allMessages(row.id()));
    }

    @Transactional
    public StudentAiDtos.Conversation sendMessage(Long userId, Long conversationId, String rawContent) {
        ConversationRow row = requireConversation(userId, conversationId, true);
        if (!"ACTIVE".equals(row.status()) || row.rounds() >= MAX_ROUNDS) {
            throw failure("AI_CONVERSATION_LIMIT_REACHED", "本会话已达到 10 轮上限，请开启新会话", HttpStatus.CONFLICT);
        }
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 500) {
            throw failure("AI_MESSAGE_INVALID", "追问内容需为 1 至 500 字", HttpStatus.BAD_REQUEST);
        }
        StudentAiFact fact = requireFact(userId, row.answerFactId());
        String assistant = guardedReply(content);
        if (assistant == null) {
            try {
                AiModelResult result = provider.generate(prompts.tutor(fact, recentMessages(row.id()), content,
                        visionContext(fact.questionId())));
                assistant = safeAssistant(result.content());
            } catch (AiProviderException exception) {
                throw providerFailure(exception);
            }
        }
        int firstSequence = row.rounds() * 2 + 1;
        jdbc.update("INSERT INTO ai_xiao_xi (ai_hui_hua_id,fa_yan_jiao_se,nei_rong,xu_hao) VALUES (?,'USER',?,?)",
                row.id(), content, firstSequence);
        jdbc.update("INSERT INTO ai_xiao_xi (ai_hui_hua_id,fa_yan_jiao_se,nei_rong,xu_hao) VALUES (?,'ASSISTANT',?,?)",
                row.id(), assistant, firstSequence + 1);
        int rounds = row.rounds() + 1;
        jdbc.update("UPDATE ai_hui_hua SET lei_ji_lun_shu=?,zhuang_tai=? WHERE id=? AND xue_sheng_id=?",
                rounds, rounds >= MAX_ROUNDS ? "LIMIT_REACHED" : "ACTIVE", row.id(), row.studentId());
        return conversation(userId, row.id());
    }

    private StudentAiFact requireFact(Long userId, Long answerFactId) {
        if (userId == null || answerFactId == null) throw notFound();
        return jdbc.query("""
                SELECT da.id,da.xue_sheng_id,lt.id,lt.ti_mu_id,k.ke_mu_dai_ma,lt.ti_mu_lei_xing,lt.ti_gan_kuai_zhao,
                  CAST(lt.xuan_xiang_kuai_zhao AS CHAR),CAST(da.xue_sheng_da_an AS CHAR),
                  CAST(lt.zheng_que_da_an_kuai_zhao AS CHAR),lt.biao_zhun_jie_xi_kuai_zhao,
                  CAST(lt.zhi_shi_dian_kuai_zhao AS CHAR),da.shi_fou_zheng_que
                FROM xue_sheng_da_ti da
                JOIN xue_sheng_dang_an xs ON xs.id=da.xue_sheng_id
                JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                JOIN lian_xi_hui_hua lh ON lh.id=lt.lian_xi_hui_hua_id AND lh.xue_sheng_id=da.xue_sheng_id
                JOIN ke_mu k ON k.id=lh.ke_mu_id
                WHERE da.id=? AND xs.yong_hu_id=? AND xs.zhuang_tai='ACTIVE' AND xs.yi_shan_chu=0 AND lh.zhuang_tai='SUBMITTED'
                """, (rs, n) -> new StudentAiFact(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getString(10), rs.getString(11), rs.getString(12), rs.getBoolean(13)), answerFactId, userId)
                .stream().findFirst().orElseThrow(this::notFound);
    }

    private StudentAiDtos.Analysis findAnalysis(StudentAiFact fact, boolean forCache) {
        return jdbc.query("""
                SELECT zhuang_tai,cuo_wu_lei_xing,cuo_wu_yuan_yin,zheng_que_si_lu,
                  CAST(chang_jian_cuo_wu AS CHAR),CAST(fu_xi_jian_yi AS CHAR),cuo_wu_dai_ma,
                  chuang_jian_shi_jian,geng_xin_shi_jian
                FROM ai_cuo_ti_fen_xi WHERE xue_sheng_da_ti_id=? AND xue_sheng_id=?
                """, (rs, n) -> new StudentAiDtos.Analysis(fact.answerFactId(), rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getString(4), readStringList(rs.getString(5)), readStringList(rs.getString(6)),
                false, rs.getString(7), rs.getObject(8, LocalDateTime.class), rs.getObject(9, LocalDateTime.class)),
                fact.answerFactId(), fact.studentId()).stream().findFirst()
                .orElseGet(() -> new StudentAiDtos.Analysis(fact.answerFactId(), "NOT_GENERATED", null, null,
                        null, List.of(), List.of(), false, null, null, null));
    }

    private String findAnalysisHash(long factId) {
        return jdbc.query("SELECT shu_ru_shi_shi_ha_xi FROM ai_cuo_ti_fen_xi WHERE xue_sheng_da_ti_id=?",
                (rs, n) -> rs.getString(1), factId).stream().findFirst().orElse(null);
    }

    private StudentAiDtos.Analysis withCached(StudentAiDtos.Analysis value) {
        return new StudentAiDtos.Analysis(value.answerFactId(), value.status(), value.errorType(), value.errorReason(),
                value.correctThinking(), value.commonMistakes(), value.reviewSuggestions(), true, value.errorCode(),
                value.createdAt(), value.updatedAt());
    }

    private void markAnalysisFailed(long factId, String code) {
        jdbc.update("UPDATE ai_cuo_ti_fen_xi SET zhuang_tai='FAILED',cuo_wu_dai_ma=? WHERE xue_sheng_da_ti_id=?",
                safeCode(code), factId);
    }

    private ConversationRow requireConversation(Long userId, Long id, boolean lock) {
        if (userId == null || id == null) throw notFound();
        String sql = """
                SELECT h.id,h.xue_sheng_id,h.xue_sheng_da_ti_id,lt.ti_mu_id,h.zhuang_tai,h.lei_ji_lun_shu
                FROM ai_hui_hua h
                JOIN xue_sheng_dang_an xs ON xs.id=h.xue_sheng_id
                JOIN lian_xi_ti_mu lt ON lt.id=h.lian_xi_ti_mu_id
                WHERE h.id=? AND xs.yong_hu_id=? AND xs.zhuang_tai='ACTIVE' AND xs.yi_shan_chu=0
                """ + (lock ? " FOR UPDATE" : "");
        return jdbc.query(sql, (rs, n) -> new ConversationRow(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getString(5), rs.getInt(6)), id, userId)
                .stream().findFirst().orElseThrow(this::notFound);
    }

    private List<StudentAiDtos.Message> allMessages(long conversationId) {
        return jdbc.query("""
                SELECT id,fa_yan_jiao_se,nei_rong,xu_hao,chuang_jian_shi_jian
                FROM ai_xiao_xi WHERE ai_hui_hua_id=? ORDER BY xu_hao
                """, (rs, n) -> new StudentAiDtos.Message(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getInt(4), rs.getObject(5, LocalDateTime.class)), conversationId);
    }

    private List<StudentAiDtos.Message> recentMessages(long conversationId) {
        List<StudentAiDtos.Message> descending = jdbc.query("""
                SELECT id,fa_yan_jiao_se,nei_rong,xu_hao,chuang_jian_shi_jian
                FROM ai_xiao_xi WHERE ai_hui_hua_id=? ORDER BY xu_hao DESC LIMIT ?
                """, (rs, n) -> new StudentAiDtos.Message(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getInt(4), rs.getObject(5, LocalDateTime.class)), conversationId, RECENT_MESSAGES);
        Collections.reverse(descending);
        List<StudentAiDtos.Message> budgeted = new ArrayList<>();
        int characters = 0;
        for (int index = descending.size() - 1; index >= 0; index--) {
            StudentAiDtos.Message message = descending.get(index);
            if (characters + message.content().length() > HISTORY_CHAR_BUDGET) break;
            budgeted.add(message);
            characters += message.content().length();
        }
        Collections.reverse(budgeted);
        return List.copyOf(budgeted);
    }

    private StudentAiDtos.Conversation toConversation(ConversationRow row, List<StudentAiDtos.Message> messages) {
        return new StudentAiDtos.Conversation(row.id(), row.answerFactId(), row.questionId(), row.status(), row.rounds(),
                MAX_ROUNDS, Math.max(0, MAX_ROUNDS - row.rounds()), messages);
    }

    private String guardedReply(String content) {
        if (MODEL_IDENTITY.matcher(content).find()) {
            return "底层模型由系统统一管理，我会以 RIKE 理科学习助手的身份为你提供学习帮助。";
        }
        if (PRODUCT_IDENTITY.matcher(content).find()) {
            return "我是 RIKE 理科学习助手，主要围绕当前物理、化学、生物题目为你提供讲解、错因分析和学习建议。";
        }
        if (SECRET_OR_OVERRIDE.matcher(content).find()) {
            return "我不能披露内部提示词、密钥或改变系统 STANDARD 事实。我们可以继续讨论这道题的知识点与解题步骤。";
        }
        String lower = content.toLowerCase(Locale.ROOT);
        if (lower.contains("写小说") || lower.contains("股票") || lower.contains("天气") || lower.contains("娱乐新闻")) {
            return "这个会话只围绕当前物化生题目答疑。请告诉我你对本题哪个步骤或知识点还不理解。";
        }
        return null;
    }

    private String visionContext(long questionId) {
        VisionContextService.Resolution resolution = visionContexts.resolve(questionId, false);
        if (!resolution.used()) return null;
        return resolution.available() ? resolution.contextJson()
                : "UNAVAILABLE: 视觉上下文不可用，不得猜测未看到的图片内容。";
    }

    private String safeAssistant(String content) {
        if (content == null || content.isBlank()) {
            throw new AiProviderException(com.neu.riketiku.ai.provider.AiProviderErrorType.INVALID_RESPONSE,
                    "AI provider returned an empty response");
        }
        String value = content.trim();
        if (PROVIDER_DISCLOSURE.matcher(value).find()) {
            return "底层模型由系统统一管理。我会以 RIKE 理科学习助手的身份，继续围绕当前题目提供学习帮助。";
        }
        return value.length() <= MAX_ASSISTANT_CHARS ? value : value.substring(0, MAX_ASSISTANT_CHARS);
    }

    private String factHash(StudentAiFact fact) {
        try {
            String source = String.join("\u001f", StudentAiPromptFactory.PROMPT_VERSION, fact.subjectCode(), fact.questionType(),
                    fact.stem(), String.valueOf(fact.optionsJson()), fact.studentAnswerJson(), fact.correctAnswerJson(),
                    fact.standardAnalysis(), fact.knowledgePointsJson());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash immutable AI facts", exception);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() { }); }
        catch (Exception ignored) { return List.of(); }
    }

    private String safeCode(String value) {
        if (value == null) return null;
        String clean = value.replaceAll("[^A-Za-z0-9_.:-]", "_");
        return clean.substring(0, Math.min(clean.length(), 64));
    }

    private RenZhengYeWuYiChang providerFailure(AiProviderException exception) {
        HttpStatus status = exception.errorType() == com.neu.riketiku.ai.provider.AiProviderErrorType.RATE_LIMITED
                ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE;
        return failure("AI_" + exception.errorType().name(), "AI 暂不可用，STANDARD 解析和学习记录不受影响", status);
    }

    private RenZhengYeWuYiChang notFound() {
        return failure("STUDENT_AI_RESOURCE_NOT_FOUND", "AI 学习资源不存在或无权访问", HttpStatus.NOT_FOUND);
    }

    private RenZhengYeWuYiChang failure(String code, String message, HttpStatus status) {
        return new RenZhengYeWuYiChang(code, message, status);
    }

    private record ConversationRow(long id, long studentId, long answerFactId, long questionId,
                                   String status, int rounds) { }
    private record AnalysisLock(String status, String hash) { }
}
