package com.neu.riketiku.aishengcheng;

import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderException;
import com.neu.riketiku.ai.vision.AiVisionException;
import com.neu.riketiku.ai.vision.VisionContextService;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import com.neu.riketiku.tiku.admin.QuestionContentHashService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiQuestionGenerationService {
    private static final Set<String> TYPES=Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","FILL_BLANK");
    private static final Set<String> MODES=Set.of("NUMERIC_CONDITION","SCENARIO","KNOWLEDGE_ANGLE","DISTRACTOR","COMBINED");
    private static final double SIMILARITY_THRESHOLD=.72;
    private final JdbcTemplate jdbc;
    private final AiProviderService provider;
    private final AiRuntimeConfigurationService configurations;
    private final VisionContextService visionContexts;
    private final AiQuestionGenerationPromptFactory prompts;
    private final AiCandidateParser parser;
    private final QuestionContentHashService hashes;
    private final QuestionAdminService questions;
    private final TransactionTemplate transactions;
    private final ObjectMapper mapper=new ObjectMapper();

    public AiQuestionGenerationService(JdbcTemplate jdbc,AiProviderService provider,
            AiRuntimeConfigurationService configurations,VisionContextService visionContexts,
            AiQuestionGenerationPromptFactory prompts,AiCandidateParser parser,
            QuestionContentHashService hashes,QuestionAdminService questions,
            PlatformTransactionManager transactionManager){
        this.jdbc=jdbc;this.provider=provider;this.configurations=configurations;this.visionContexts=visionContexts;
        this.prompts=prompts;this.parser=parser;this.hashes=hashes;this.questions=questions;
        this.transactions=new TransactionTemplate(transactionManager);
    }

    public AiQuestionGenerationDtos.Task generate(long actorId,String role,AiQuestionGenerationDtos.Generate command){
        String actorRole=role.toUpperCase(Locale.ROOT); validateCommand(command);
        AiQuestionGenerationPromptFactory.Mother mother=mother(command.motherQuestionId());
        authorize(actorId,actorRole,mother.subjectId()); validatePoints(mother.subjectId(),command.knowledgePointIds());
        long pending=count("""
                SELECT COUNT(DISTINCT q.id) FROM ti_mu q JOIN ti_mu_lai_yuan s ON s.ti_mu_id=q.id
                WHERE q.fu_ti_mu_id=? AND q.zhuang_tai='PENDING' AND q.yi_shan_chu=0
                  AND s.lai_yuan_lei_xing='AI_GENERATED' AND s.yi_shan_chu=0
                """,mother.id());
        if(pending+command.count()>6) fail("AI_PENDING_LIMIT_REACHED","同一母题最多保留 6 道待审核 AI 候选题",HttpStatus.CONFLICT);
        String requestHash=requestHash(command);
        Long taskId=insertTask(actorId,actorRole,command,requestHash);
        long started=System.nanoTime();
        try{
            VisionContextService.Resolution vision=visionContexts.resolve(mother.id(),true);
            AiRuntimeConfig runtime=configurations.text();
            int maxTokens=Math.max(64,Math.min(3000,runtime.maxTokens()));
            AiModelResult result=provider.generate(prompts.request(mother,command,vision.contextJson(),maxTokens));
            List<AiCandidateParser.Candidate> candidates=parser.parse(result.content(),command.count(),command.questionType(),
                    command.targetDifficulty(),new HashSet<>(command.knowledgePointIds()));
            List<Prepared> prepared=prepare(mother,candidates);
            long latency=elapsed(started);
            transactions.executeWithoutResult(status->{
                persist(taskId,actorId,prepared,vision.used());
                jdbc.update("""
                        UPDATE ai_sheng_cheng_ren_wu SET provider_dai_ma=?,model_dai_ma=?,zhuang_tai='SUCCESS',
                          yi_sheng_cheng_shu_liang=?,shi_fou_shi_yong_shi_jue=?,hao_shi_hao_miao=?,wan_cheng_shi_jian=CURRENT_TIMESTAMP(3)
                        WHERE id=?
                        """,safe(result.providerCode()),safe(result.modelCode()),prepared.size(),vision.used(),latency,taskId);
            });
            return task(taskId,actorId,actorRole);
        }catch(AiCandidateParser.InvalidCandidateException exception){markFailed(taskId,"INVALID_CANDIDATE_JSON",started);throw failEx("AI_CANDIDATE_INVALID","模型返回的候选题未通过严格校验",HttpStatus.SERVICE_UNAVAILABLE);}
        catch(AiVisionException exception){markFailed(taskId,safe(exception.getMessage()),started);throw failEx("AI_VISION_UNAVAILABLE","母题依赖图片且视觉上下文不可用，未生成候选题",HttpStatus.SERVICE_UNAVAILABLE);}
        catch(AiProviderException exception){markFailed(taskId,exception.errorType().name(),started);throw failEx("AI_"+exception.errorType().name(),"AI 候选题生成暂不可用",HttpStatus.SERVICE_UNAVAILABLE);}
        catch(RenZhengYeWuYiChang exception){markFailed(taskId,exception.getCode(),started);throw exception;}
        catch(RuntimeException exception){markFailed(taskId,"GENERATION_FAILED",started);throw failEx("AI_GENERATION_FAILED","AI 候选题生成失败，未创建题目",HttpStatus.SERVICE_UNAVAILABLE);}
    }

    @Transactional(readOnly=true)
    public List<AiQuestionGenerationDtos.Task> tasks(long actorId,String role){
        String normalized=role.toUpperCase(Locale.ROOT);
        List<Long> ids="ADMIN".equals(normalized)?jdbc.query("SELECT id FROM ai_sheng_cheng_ren_wu ORDER BY id DESC",(rs,row)->rs.getLong(1))
                :jdbc.query("""
                    SELECT g.id FROM ai_sheng_cheng_ren_wu g JOIN ti_mu q ON q.id=g.mu_ti_mu_id
                    JOIN jiao_shi_dang_an j ON j.yong_hu_id=? JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id AND r.ke_mu_id=q.ke_mu_id
                    WHERE r.zhuang_tai='ACTIVE' ORDER BY g.id DESC
                    """,(rs,row)->rs.getLong(1),actorId);
        return ids.stream().map(id->task(id,actorId,normalized)).toList();
    }

    @Transactional(readOnly=true)
    public AiQuestionGenerationDtos.Task task(long id,long actorId,String role){
        TaskRow row=jdbc.query("""
                SELECT g.id,g.mu_ti_mu_id,g.chuang_jian_ren_id,g.chuang_jian_ren_jiao_se,g.mu_biao_ti_xing,
                  CAST(g.zhi_shi_dian_ids AS CHAR),g.mu_biao_nan_du,g.bian_shi_fang_shi,g.sheng_cheng_shu_liang,
                  g.qing_qiu_ha_xi,g.provider_dai_ma,g.model_dai_ma,g.prompt_ban_ben,g.zhuang_tai,
                  g.yi_sheng_cheng_shu_liang,g.shi_fou_shi_yong_shi_jue,g.shi_bai_dai_ma,g.hao_shi_hao_miao,
                  g.chuang_jian_shi_jian,g.wan_cheng_shi_jian,q.ke_mu_id
                FROM ai_sheng_cheng_ren_wu g JOIN ti_mu q ON q.id=g.mu_ti_mu_id WHERE g.id=?
                """,(rs,n)->new TaskRow(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),
                readLongs(rs.getString(6)),rs.getInt(7),rs.getString(8),rs.getInt(9),rs.getString(10),rs.getString(11),
                rs.getString(12),rs.getString(13),rs.getString(14),rs.getInt(15),rs.getBoolean(16),rs.getString(17),
                rs.getObject(18,Long.class),rs.getObject(19,LocalDateTime.class),rs.getObject(20,LocalDateTime.class),rs.getLong(21)),id)
                .stream().findFirst().orElseThrow(()->failEx("AI_GENERATION_TASK_NOT_FOUND","生成任务不存在",HttpStatus.NOT_FOUND));
        authorize(actorId,role.toUpperCase(Locale.ROOT),row.subjectId());
        List<AiQuestionGenerationDtos.Candidate> candidates=candidates(id);
        return new AiQuestionGenerationDtos.Task(row.id(),row.motherId(),row.creatorId(),row.creatorRole(),row.type(),row.points(),
                row.difficulty(),row.mode(),row.count(),row.hash(),row.provider(),row.model(),row.promptVersion(),row.status(),
                row.generated(),row.vision(),row.failure(),row.latency(),row.createdAt(),row.finishedAt(),candidates);
    }

    @Transactional
    public AiQuestionGenerationDtos.Candidate review(long actorId,String role,long questionId,AiQuestionGenerationDtos.Review request){
        CandidateAuth auth=candidateAuth(questionId); authorize(actorId,role.toUpperCase(Locale.ROOT),auth.subjectId());
        String decision=request.reviewResult().trim().toUpperCase(Locale.ROOT);
        if(!Set.of("APPROVED","REJECTED").contains(decision))fail("AI_REVIEW_RESULT_INVALID","审核结果不受支持",HttpStatus.BAD_REQUEST);
        if("REJECTED".equals(decision)&&(request.reviewComment()==null||request.reviewComment().isBlank()))fail("REVIEW_OPINION_REQUIRED","驳回必须填写审核意见",HttpStatus.BAD_REQUEST);
        jdbc.update("""
                UPDATE ai_hou_xuan_ti_zhi_liang_ping_jia SET xue_ke_zheng_que_xing=?,da_an_zheng_que_xing=?,
                  ke_jie_xing=?,zhi_shi_yi_zhi_xing=?,nan_du_pi_pei=?,shen_he_jie_guo=?,shen_he_hao_shi_fen_zhong=?,
                  shen_he_ren_id=?,shen_he_ping_lun=? WHERE ti_mu_id=?
                """,request.subjectCorrectness(),request.answerCorrectness(),request.solvability(),request.knowledgeConsistency(),
                request.difficultyMatch(),decision,request.reviewMinutes(),actorId,blank(request.reviewComment()),questionId);
        if("APPROVED".equals(decision))questions.transition(questionId,"APPROVED","PENDING","PUBLISHED",blank(request.reviewComment()),actorId);
        else questions.transition(questionId,"REJECTED","PENDING","DRAFT",request.reviewComment().trim(),actorId);
        return candidate(questionId);
    }

    @Transactional(readOnly=true)
    public AiQuestionGenerationDtos.Stats stats(long actorId,String role){
        if(!"ADMIN".equals(role.toUpperCase(Locale.ROOT)))fail("AI_STATS_ADMIN_ONLY","仅管理员可查看全局 AI 质量统计",HttpStatus.FORBIDDEN);
        return jdbc.queryForObject("""
                SELECT COUNT(*),SUM(zhuang_tai='SUCCESS'),SUM(zhuang_tai='FAILED'),COALESCE(SUM(sheng_cheng_shu_liang),0),
                  COALESCE(SUM(yi_sheng_cheng_shu_liang),0),AVG(hao_shi_hao_miao) FROM ai_sheng_cheng_ren_wu
                """,(rs,row)->new AiQuestionGenerationDtos.Stats(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),
                count("SELECT COUNT(*) FROM ai_hou_xuan_ti_zhi_liang_ping_jia WHERE chong_fu_ti_shi='SUSPECTED_DUPLICATE'"),
                count("SELECT COUNT(*) FROM ai_hou_xuan_ti_zhi_liang_ping_jia WHERE shen_he_jie_guo='APPROVED'"),
                count("SELECT COUNT(*) FROM ai_hou_xuan_ti_zhi_liang_ping_jia WHERE shen_he_jie_guo='REJECTED'"),
                rs.getObject(6,Double.class),jdbc.queryForObject("SELECT AVG(shen_he_hao_shi_fen_zhong) FROM ai_hou_xuan_ti_zhi_liang_ping_jia",Double.class)));
    }

    @Transactional(readOnly=true)
    public List<AiQuestionGenerationDtos.MotherOption> mothers(long actorId,String role){
        String normalized=role.toUpperCase(Locale.ROOT);
        if("ADMIN".equals(normalized))return jdbc.query("""
                SELECT q.id,q.ke_mu_id,s.ke_mu_dai_ma,LEFT(q.ti_gan,160),q.ti_mu_lei_xing,q.nan_du
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 ORDER BY q.id DESC LIMIT 200
                """,(rs,row)->new AiQuestionGenerationDtos.MotherOption(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6)));
        return jdbc.query("""
                SELECT DISTINCT q.id,q.ke_mu_id,s.ke_mu_dai_ma,LEFT(q.ti_gan,160),q.ti_mu_lei_xing,q.nan_du
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id JOIN jiao_shi_dang_an j ON j.yong_hu_id=?
                JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id AND r.ke_mu_id=q.ke_mu_id AND r.zhuang_tai='ACTIVE'
                WHERE q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0 ORDER BY q.id DESC LIMIT 200
                """,(rs,row)->new AiQuestionGenerationDtos.MotherOption(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6)),actorId);
    }

    @Transactional(readOnly=true)
    public List<AiQuestionGenerationDtos.KnowledgePointOption> knowledgePoints(long actorId,String role,long subjectId){
        authorize(actorId,role.toUpperCase(Locale.ROOT),subjectId);
        return jdbc.query("""
                SELECT id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing
                FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY wan_zheng_lu_jing,id
                """,(rs,row)->new AiQuestionGenerationDtos.KnowledgePointOption(rs.getLong(1),rs.getString(2),rs.getString(3)),subjectId);
    }

    private List<Prepared> prepare(AiQuestionGenerationPromptFactory.Mother mother,List<AiCandidateParser.Candidate> candidates){
        List<Prepared> values=new ArrayList<>();Set<String> batch=new HashSet<>();
        List<String> existing=jdbc.query("SELECT ti_gan FROM ti_mu WHERE ke_mu_id=? AND yi_shan_chu=0",(rs,row)->rs.getString(1),mother.subjectId());
        for(AiCandidateParser.Candidate candidate:candidates){
            List<QuestionContentHashService.OptionContent> opts=candidate.options().stream().map(o->new QuestionContentHashService.OptionContent(o.label(),o.content())).toList();
            String hash=hashes.calculate(candidate.stem(),opts);
            if(!batch.add(hash))fail("AI_BATCH_DUPLICATE","同批候选题存在完全重复内容，整批未写入",HttpStatus.CONFLICT);
            if(count("SELECT COUNT(*) FROM ti_mu WHERE ke_mu_id=? AND nei_rong_ha_xi=? AND yi_shan_chu=0",mother.subjectId(),hash)>0)
                fail("QUESTION_DUPLICATE","候选题与现有题目完全重复，整批未写入",HttpStatus.CONFLICT);
            boolean suspected=existing.stream().anyMatch(stem->jaccard(candidate.stem(),stem)>=SIMILARITY_THRESHOLD);
            values.add(new Prepared(candidate,hash,suspected));
        }
        return List.copyOf(values);
    }

    private void persist(long taskId,long actorId,List<Prepared> prepared,boolean visionUsed){
        for(Prepared value:prepared){
            AiCandidateParser.Candidate c=value.candidate();
            List<QuestionDtos.Option> options=c.options().stream().map(o->new QuestionDtos.Option(o.label(),o.content(),o.correct())).toList();
            List<QuestionDtos.Source> sources=List.of("QUESTION","ANSWER","STANDARD_ANALYSIS").stream()
                    .map(part->new QuestionDtos.Source(part,"AI_GENERATED","RIKE AI 候选变式题","USER_PROVIDED",null,null,null,null,null,"仅作为本地毕设候选内容，人工审核后发布")).toList();
            long subjectId=jdbc.queryForObject("SELECT ke_mu_id FROM ti_mu WHERE id=(SELECT mu_ti_mu_id FROM ai_sheng_cheng_ren_wu WHERE id=?)",Long.class,taskId);
            QuestionDtos.Save save=new QuestionDtos.Save(subjectId,c.questionType(),"ONLINE_PRACTICE",c.stem(),c.correctAnswer(),c.difficulty(),
                    "AI 候选难度，待人工评价",true,options,c.standardAnalysis(),c.knowledgePointIds(),sources);
            long questionId=questions.create(save,actorId).question().id();
            jdbc.update("UPDATE ti_mu SET fu_ti_mu_id=? WHERE id=?",jdbc.queryForObject("SELECT mu_ti_mu_id FROM ai_sheng_cheng_ren_wu WHERE id=?",Long.class,taskId),questionId);
            questions.transition(questionId,"SUBMITTED","DRAFT","PENDING","AI 候选题进入人工审核",actorId);
            jdbc.update("""
                    INSERT INTO ai_hou_xuan_ti_zhi_liang_ping_jia
                      (ai_sheng_cheng_ren_wu_id,ti_mu_id,bian_shi_zhai_yao,chong_fu_ti_shi,shi_fou_shi_yong_shi_jue)
                    VALUES (?,?,?,?,?)
                    """,taskId,questionId,c.variationSummary(),value.suspected()?"SUSPECTED_DUPLICATE":"NONE",visionUsed);
        }
    }

    private AiQuestionGenerationPromptFactory.Mother mother(long id){
        MotherBase base=jdbc.query("""
                SELECT q.id,q.ke_mu_id,s.ke_mu_dai_ma,q.ti_mu_lei_xing,q.shi_yong_mo_shi,q.ti_gan,
                  CAST(q.zheng_que_da_an AS CHAR),a.jie_xi_nei_rong
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0
                WHERE q.id=? AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                """,(rs,row)->new MotherBase(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)),id)
                .stream().findFirst().orElseThrow(()->failEx("AI_MOTHER_QUESTION_UNAVAILABLE","只能从已发布且有 STANDARD 解析的母题生成",HttpStatus.CONFLICT));
        String options=mapper.writeValueAsString(jdbc.query("SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu",
                (rs,row)->java.util.Map.of("label",rs.getString(1),"content",rs.getString(2),"correct",rs.getBoolean(3)),id));
        return new AiQuestionGenerationPromptFactory.Mother(base.id(),base.subjectId(),base.subjectCode(),base.type(),base.usageMode(),base.stem(),options,base.answer(),base.analysis());
    }

    private Long insertTask(long actorId,String role,AiQuestionGenerationDtos.Generate c,String hash){
        try{jdbc.update("""
                INSERT INTO ai_sheng_cheng_ren_wu(mu_ti_mu_id,chuang_jian_ren_id,chuang_jian_ren_jiao_se,mu_biao_ti_xing,
                  zhi_shi_dian_ids,mu_biao_nan_du,bian_shi_fang_shi,sheng_cheng_shu_liang,qing_qiu_ha_xi,prompt_ban_ben,zhuang_tai)
                VALUES (?,?,?,?,CAST(? AS JSON),?,?,?,?,?,'GENERATING')
                """,c.motherQuestionId(),actorId,role,c.questionType(),mapper.writeValueAsString(c.knowledgePointIds()),c.targetDifficulty(),c.variationMode(),c.count(),hash,AiQuestionGenerationPromptFactory.PROMPT_VERSION);
            return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        }catch(DuplicateKeyException exception){
            RetryableTask existing=jdbc.query("""
                    SELECT g.id,g.zhuang_tai,g.yi_sheng_cheng_shu_liang,
                      (SELECT COUNT(*) FROM ai_hou_xuan_ti_zhi_liang_ping_jia v WHERE v.ai_sheng_cheng_ren_wu_id=g.id)
                    FROM ai_sheng_cheng_ren_wu g WHERE g.qing_qiu_ha_xi=?
                    """,(rs,row)->new RetryableTask(rs.getLong(1),rs.getString(2),rs.getInt(3),rs.getInt(4)),hash)
                    .stream().findFirst().orElseThrow(()->failEx("DUPLICATE_GENERATION_REQUEST","相同有效生成请求已存在",HttpStatus.CONFLICT));
            if(!"FAILED".equals(existing.status())||existing.generated()!=0||existing.candidates()!=0)
                throw failEx("DUPLICATE_GENERATION_REQUEST","相同有效生成请求已存在",HttpStatus.CONFLICT);
            jdbc.update("""
                    UPDATE ai_sheng_cheng_ren_wu SET chuang_jian_ren_id=?,chuang_jian_ren_jiao_se=?,provider_dai_ma=NULL,
                      model_dai_ma=NULL,zhuang_tai='GENERATING',yi_sheng_cheng_shu_liang=0,shi_fou_shi_yong_shi_jue=0,
                      shi_bai_dai_ma=NULL,hao_shi_hao_miao=NULL,wan_cheng_shi_jian=NULL WHERE id=?
                    """,actorId,role,existing.id());
            return existing.id();
        }
    }
    private void markFailed(long id,String code,long started){jdbc.update("UPDATE ai_sheng_cheng_ren_wu SET zhuang_tai='FAILED',shi_bai_dai_ma=?,hao_shi_hao_miao=?,wan_cheng_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?",safe(code),elapsed(started),id);}
    private void validateCommand(AiQuestionGenerationDtos.Generate c){if(!TYPES.contains(c.questionType())||!MODES.contains(c.variationMode())||c.count()<1||c.count()>3||c.targetDifficulty()<1||c.targetDifficulty()>5)fail("AI_GENERATION_REQUEST_INVALID","生成参数不合法",HttpStatus.BAD_REQUEST);}
    private void validatePoints(long subjectId,List<Long> ids){if(ids==null||ids.isEmpty()||new HashSet<>(ids).size()!=ids.size())fail("AI_KNOWLEDGE_POINT_INVALID","知识点不合法",HttpStatus.BAD_REQUEST);String marks=String.join(",",java.util.Collections.nCopies(ids.size(),"?"));List<Object> args=new ArrayList<>();args.add(subjectId);args.addAll(ids);if(count("SELECT COUNT(*) FROM zhi_shi_dian WHERE ke_mu_id=? AND id IN ("+marks+") AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",args.toArray())!=ids.size())fail("AI_KNOWLEDGE_POINT_INVALID","知识点不存在或不属于母题科目",HttpStatus.BAD_REQUEST);}
    private void authorize(long actorId,String role,long subjectId){if("ADMIN".equals(role))return;if(!"TEACHER".equals(role)||count("""
            SELECT COUNT(*) FROM jiao_shi_dang_an j JOIN ren_ke_guan_xi r ON r.jiao_shi_id=j.id
            WHERE j.yong_hu_id=? AND j.zhuang_tai='ACTIVE' AND j.yi_shan_chu=0 AND r.ke_mu_id=? AND r.zhuang_tai='ACTIVE'
            """,actorId,subjectId)==0)fail("AI_GENERATION_FORBIDDEN","无权操作该科目候选题",HttpStatus.FORBIDDEN);}
    private CandidateAuth candidateAuth(long id){return jdbc.query("SELECT q.ke_mu_id,v.ai_sheng_cheng_ren_wu_id FROM ai_hou_xuan_ti_zhi_liang_ping_jia v JOIN ti_mu q ON q.id=v.ti_mu_id WHERE q.id=?",(rs,row)->new CandidateAuth(rs.getLong(1),rs.getLong(2)),id).stream().findFirst().orElseThrow(()->failEx("AI_CANDIDATE_NOT_FOUND","AI 候选题不存在",HttpStatus.NOT_FOUND));}
    private List<AiQuestionGenerationDtos.Candidate> candidates(long taskId){return jdbc.query("SELECT ti_mu_id FROM ai_hou_xuan_ti_zhi_liang_ping_jia WHERE ai_sheng_cheng_ren_wu_id=? ORDER BY id",(rs,row)->candidate(rs.getLong(1)),taskId);}
    private AiQuestionGenerationDtos.Candidate candidate(long id){
        CandidateBase b=jdbc.query("""
                SELECT q.id,v.ai_sheng_cheng_ren_wu_id,q.ti_gan,q.ti_mu_lei_xing,q.nan_du,q.zhuang_tai,
                  v.bian_shi_zhai_yao,v.chong_fu_ti_shi,v.shi_fou_shi_yong_shi_jue,g.provider_dai_ma,g.model_dai_ma,
                  CAST(q.zheng_que_da_an AS CHAR),a.jie_xi_nei_rong,v.xue_ke_zheng_que_xing,v.da_an_zheng_que_xing,
                  v.ke_jie_xing,v.zhi_shi_yi_zhi_xing,v.nan_du_pi_pei,v.shen_he_jie_guo,v.shen_he_hao_shi_fen_zhong,
                  v.shen_he_ren_id,v.shen_he_ping_lun
                FROM ai_hou_xuan_ti_zhi_liang_ping_jia v JOIN ti_mu q ON q.id=v.ti_mu_id
                JOIN ai_sheng_cheng_ren_wu g ON g.id=v.ai_sheng_cheng_ren_wu_id
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.yi_shan_chu=0 WHERE q.id=?
                """,(rs,row)->new CandidateBase(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getBoolean(9),rs.getString(10),rs.getString(11),rs.getString(12),rs.getString(13),new AiQuestionGenerationDtos.Quality(rs.getObject(14,Integer.class),rs.getObject(15,Integer.class),rs.getObject(16,Integer.class),rs.getObject(17,Integer.class),rs.getObject(18,Integer.class),rs.getString(19),rs.getObject(20,Integer.class),rs.getObject(21,Long.class),rs.getString(22))),id).stream().findFirst().orElseThrow();
        List<AiQuestionGenerationDtos.KnowledgePoint> points=jdbc.query("SELECT p.id,p.zhi_shi_dian_ming_cheng FROM ti_mu_zhi_shi_dian qp JOIN zhi_shi_dian p ON p.id=qp.zhi_shi_dian_id WHERE qp.ti_mu_id=? AND qp.yi_shan_chu=0 ORDER BY qp.pai_xu",(rs,row)->new AiQuestionGenerationDtos.KnowledgePoint(rs.getLong(1),rs.getString(2)),id);
        return new AiQuestionGenerationDtos.Candidate(b.id(),b.taskId(),b.stem(),b.type(),b.difficulty(),b.status(),b.summary(),b.warning(),b.vision(),b.provider(),b.model(),b.answer(),b.analysis(),points,b.quality());
    }
    private String requestHash(AiQuestionGenerationDtos.Generate c){List<Long> points=c.knowledgePointIds().stream().sorted().toList();return sha(c.motherQuestionId()+"|"+c.questionType()+"|"+points+"|"+c.targetDifficulty()+"|"+c.variationMode()+"|"+AiQuestionGenerationPromptFactory.PROMPT_VERSION);}
    private double jaccard(String a,String b){Set<String>x=ngrams(a),y=ngrams(b);if(x.isEmpty()||y.isEmpty())return 0;Set<String>intersection=new HashSet<>(x);intersection.retainAll(y);Set<String>union=new HashSet<>(x);union.addAll(y);return (double)intersection.size()/union.size();}
    private Set<String> ngrams(String value){String n=value==null?"":value.replaceAll("[\\s\\p{Punct}]","").toLowerCase(Locale.ROOT);Set<String>r=new LinkedHashSet<>();for(int i=0;i+3<=n.length();i++)r.add(n.substring(i,i+3));return r;}
    private String sha(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private List<Long> readLongs(String json){try{return mapper.readValue(json,new TypeReference<List<Long>>(){});}catch(Exception e){return List.of();}}
    private long count(String sql,Object...args){Long value=jdbc.queryForObject(sql,Long.class,args);return value==null?0:value;}
    private long elapsed(long started){return(System.nanoTime()-started)/1_000_000;}
    private String safe(String value){if(value==null)return null;String clean=value.replaceAll("[^A-Za-z0-9_.:-]","_");return clean.substring(0,Math.min(64,clean.length()));}
    private String blank(String v){return v==null||v.isBlank()?null:v.trim();}
    private void fail(String code,String message,HttpStatus status){throw failEx(code,message,status);}
    private RenZhengYeWuYiChang failEx(String code,String message,HttpStatus status){return new RenZhengYeWuYiChang(code,message,status);}
    private record Prepared(AiCandidateParser.Candidate candidate,String hash,boolean suspected){ }
    private record MotherBase(long id,long subjectId,String subjectCode,String type,String usageMode,String stem,String answer,String analysis){ }
    private record CandidateAuth(long subjectId,long taskId){ }
    private record RetryableTask(long id,String status,int generated,int candidates){ }
    private record TaskRow(long id,long motherId,long creatorId,String creatorRole,String type,List<Long> points,int difficulty,String mode,int count,String hash,String provider,String model,String promptVersion,String status,int generated,boolean vision,String failure,Long latency,LocalDateTime createdAt,LocalDateTime finishedAt,long subjectId){ }
    private record CandidateBase(long id,long taskId,String stem,String type,int difficulty,String status,String summary,String warning,boolean vision,String provider,String model,String answer,String analysis,AiQuestionGenerationDtos.Quality quality){ }
}
