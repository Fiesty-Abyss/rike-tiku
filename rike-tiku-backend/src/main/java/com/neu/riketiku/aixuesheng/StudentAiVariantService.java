package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.aishengcheng.AiQuestionGenerationDtos;
import com.neu.riketiku.aishengcheng.AiQuestionGenerationService;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.xueshenglianxi.ObjectiveAnswerGrader;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class StudentAiVariantService {
    private final JdbcTemplate jdbc;private final AiQuestionGenerationService generation;private final ObjectiveAnswerGrader grader;private final ObjectMapper mapper=new ObjectMapper();
    public StudentAiVariantService(JdbcTemplate jdbc,AiQuestionGenerationService generation,ObjectiveAnswerGrader grader){this.jdbc=jdbc;this.generation=generation;this.grader=grader;}
    public StudentAiVariantDtos.Variant generate(long userId,long factId,Integer targetDifficulty,String requestedMode){
        Fact fact=fact(userId,factId);List<Long> points=jdbc.query("SELECT zhi_shi_dian_id FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu",(rs,row)->rs.getLong(1),fact.questionId());
        if(points.isEmpty())throw error("AI_VARIANT_KNOWLEDGE_MISSING","当前题缺少可用知识点",HttpStatus.CONFLICT);
        int difficulty=targetDifficulty==null?fact.difficulty():targetDifficulty;
        if(difficulty<1||difficulty>5)throw error("AI_VARIANT_DIFFICULTY_INVALID","目标难度必须是 1 到 5",HttpStatus.BAD_REQUEST);
        String mode=requestedMode==null||requestedMode.isBlank()?"COMBINED":requestedMode.trim().toUpperCase(java.util.Locale.ROOT);
        var command=new AiQuestionGenerationDtos.Generate(fact.questionId(),fact.type(),points,difficulty,mode,1);
        final AiQuestionGenerationDtos.Task task;
        try{task=generation.generate(userId,"STUDENT",command,(taskId,questions)->{
            if(questions.size()!=1)throw error("AI_INVALID_RESPONSE","AI 未返回完整的单题结构",HttpStatus.SERVICE_UNAVAILABLE);
            jdbc.update("INSERT INTO ai_xue_sheng_bian_shi_shi_li(xue_sheng_id,xue_sheng_da_ti_id,mu_ti_mu_id,ai_sheng_cheng_ren_wu_id,ti_mu_id) VALUES (?,?,?,?,?)",fact.studentId(),factId,fact.questionId(),taskId,questions.getFirst());
        });}catch(RenZhengYeWuYiChang exception){throw translateGenerationFailure(exception);}
        if(task.candidates()==null||task.candidates().size()!=1)throw error("AI_INVALID_RESPONSE","AI 未返回完整的单题结构，请稍后重试",HttpStatus.SERVICE_UNAVAILABLE);
        long id=jdbc.queryForObject("SELECT id FROM ai_xue_sheng_bian_shi_shi_li WHERE ai_sheng_cheng_ren_wu_id=?",Long.class,task.id());return detail(userId,id);
    }
    public StudentAiVariantDtos.Variant generate(long userId,long factId,Integer targetDifficulty){return generate(userId,factId,targetDifficulty,"COMBINED");}
    @Transactional(readOnly=true) public StudentAiVariantDtos.Variant detail(long userId,long id){return row(userId,id);}
    @Transactional public StudentAiVariantDtos.Variant answer(long userId,long id,JsonNode answer){Row row=require(userId,id,true);if(!"READY".equals(row.status()))throw error("AI_VARIANT_ALREADY_ANSWERED","变式题已作答或已丢弃",HttpStatus.CONFLICT);boolean correct=grader.grade(row.type(),row.correctAnswerJson(),row.optionsJson(),answer);jdbc.update("UPDATE ai_xue_sheng_bian_shi_shi_li SET zhuang_tai='ANSWERED',xue_sheng_da_an=CAST(? AS JSON),shi_fou_zheng_que=?,ti_jiao_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?",mapper.writeValueAsString(answer),correct,id);return detail(userId,id);}
    @Transactional public StudentAiVariantDtos.Variant submit(long userId,long id){Row row=require(userId,id,true);if(!"ANSWERED".equals(row.status()))throw error("AI_VARIANT_NOT_ANSWERED","请先完成作答",HttpStatus.CONFLICT);jdbc.update("UPDATE ai_xue_sheng_bian_shi_shi_li SET zhuang_tai='SUBMITTED_FOR_REVIEW',shen_he_ti_jiao_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?",id);return detail(userId,id);}
    @Transactional public void discard(long userId,long id){Row row=require(userId,id,true);if("SUBMITTED_FOR_REVIEW".equals(row.status()))throw error("AI_VARIANT_SUBMITTED","已提交审核的变式题不能丢弃",HttpStatus.CONFLICT);jdbc.update("UPDATE ai_xue_sheng_bian_shi_shi_li SET zhuang_tai='DISCARDED' WHERE id=?",id);}
    private Fact fact(long userId,long id){return jdbc.query("""
      SELECT da.xue_sheng_id,lt.ti_mu_id,lt.ti_mu_lei_xing,lt.nan_du_kuai_zhao FROM xue_sheng_da_ti da
      JOIN xue_sheng_dang_an xs ON xs.id=da.xue_sheng_id JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
      JOIN lian_xi_hui_hua lh ON lh.id=lt.lian_xi_hui_hua_id WHERE da.id=? AND xs.yong_hu_id=? AND lh.zhuang_tai='SUBMITTED'
      """,(rs,row)->new Fact(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getInt(4)),id,userId).stream().findFirst().orElseThrow(()->error("AI_VARIANT_FACT_NOT_FOUND","答题事实不存在或无权访问",HttpStatus.NOT_FOUND));}
    private Row require(long userId,long id,boolean lock){String sql="""
      SELECT v.id,v.xue_sheng_da_ti_id,v.mu_ti_mu_id,v.ti_mu_id,v.zhuang_tai,g.bian_shi_fang_shi,q.ti_mu_lei_xing,q.ti_gan,q.nan_du,
       CAST((SELECT JSON_ARRAYAGG(JSON_OBJECT('label',ordered.xuan_xiang_biao_shi,'content',ordered.xuan_xiang_nei_rong)) FROM
         (SELECT o.xuan_xiang_biao_shi,o.xuan_xiang_nei_rong FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND o.yi_shan_chu=0 ORDER BY o.pai_xu) ordered) AS CHAR),
       CAST(q.zheng_que_da_an AS CHAR),CAST(v.xue_sheng_da_an AS CHAR),v.shi_fou_zheng_que,a.jie_xi_nei_rong,e.shen_he_jie_guo,
       CASE WHEN e.ju_jue_yuan_yin IS NULL THEN 'ACCEPT'
            WHEN e.ju_jue_yuan_yin LIKE 'NOVELTY_WARN_%' THEN 'WARN' ELSE 'REJECT' END,
       e.xin_ying_du_fen_shu,e.xiang_si_du_fen_shu,e.ju_jue_yuan_yin
      FROM ai_xue_sheng_bian_shi_shi_li v JOIN xue_sheng_dang_an xs ON xs.id=v.xue_sheng_id JOIN ti_mu q ON q.id=v.ti_mu_id JOIN ai_sheng_cheng_ren_wu g ON g.id=v.ai_sheng_cheng_ren_wu_id
      JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' JOIN ai_hou_xuan_ti_zhi_liang_ping_jia e ON e.ti_mu_id=q.id
      WHERE v.id=? AND xs.yong_hu_id=?"""+(lock?" FOR UPDATE":"");return jdbc.query(sql,(rs,n)->new Row(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getInt(9),rs.getString(10),rs.getString(11),rs.getString(12),rs.getObject(13,Boolean.class),rs.getString(14),rs.getString(15),rs.getString(16),rs.getObject(17,Double.class),rs.getObject(18,Double.class),rs.getString(19)),id,userId).stream().findFirst().orElseThrow(()->error("AI_VARIANT_NOT_FOUND","变式题不存在或无权访问",HttpStatus.NOT_FOUND));}
    private StudentAiVariantDtos.Variant row(long userId,long id){Row r=require(userId,id,false);boolean revealed=!"READY".equals(r.status());return new StudentAiVariantDtos.Variant(r.id(),r.factId(),r.motherId(),r.questionId(),r.status(),r.variationMode(),r.type(),r.stem(),r.difficulty(),readOptions(r.optionsJson()),r.studentAnswerJson()==null?null:mapper.readTree(r.studentAnswerJson()),r.correct(),revealed?mapper.readTree(r.correctAnswerJson()):null,revealed?r.analysis():null,r.reviewStatus(),r.noveltyDecision(),r.noveltyScore(),r.similarityScore(),r.noveltyReason());}
    private List<StudentAiVariantDtos.Option> readOptions(String json){if(json==null)return List.of();return mapper.readValue(json,new TypeReference<List<StudentAiVariantDtos.Option>>(){});}
    private RenZhengYeWuYiChang error(String c,String m,HttpStatus s){return new RenZhengYeWuYiChang(c,m,s);}
    private RenZhengYeWuYiChang translateGenerationFailure(RenZhengYeWuYiChang exception){
        String code=exception.getCode();
        if("AI_DISABLED".equals(code)||"AI_CONFIGURATION_ERROR".equals(code))return error("AI_PROVIDER_DISABLED","AI Provider 尚未启用",HttpStatus.SERVICE_UNAVAILABLE);
        if("AI_AUTHENTICATION_ERROR".equals(code))return error(code,"AI Provider 认证失败，请联系管理员",HttpStatus.SERVICE_UNAVAILABLE);
        if("AI_RATE_LIMITED".equals(code))return error(code,"AI 请求过于频繁，请稍后再试",HttpStatus.TOO_MANY_REQUESTS);
        if("AI_TIMEOUT".equals(code))return error(code,"AI 生成超时，请稍后重试",HttpStatus.GATEWAY_TIMEOUT);
        if(code.startsWith("AI_CANDIDATE_")){String detail=exception.getMessage()==null?"":exception.getMessage();String message=code.contains("EMPTY")?"AI 返回内容为空":code.contains("OPTIONS")?"AI 返回的选项不完整":code.contains("ANSWER_OPTION")?"AI 返回的答案与选项不一致":detail.contains("VARIATION_DIMENSIONS_INSUFFICIENT")?"变化维度与所选变化方式不匹配，系统已拒绝保存":detail.contains("DIFFICULTY_COMPLEXITY_MISMATCH")?"目标难度缺少足够的复杂度变化，系统已拒绝保存":code.contains("SIMILARITY")&&detail.contains("ONLY_ENTITY_OR_NUMBER_CHANGED")?"这次候选只改变了数字或情境名称，系统已拒绝保存":code.contains("SIMILARITY")&&detail.contains("OPTIONS_ONLY_REORDERED")?"这次候选仅调整了选项顺序，系统已拒绝保存":code.contains("SIMILARITY")&&detail.contains("ANALYSIS_COPIED")?"这次候选的标准解析与母题过于接近，系统已拒绝保存":code.contains("SIMILARITY")?"系统已经自动调整一次，但候选仍与原题过于接近，请更换变化方式":"AI 返回的题目字段未通过校验";return error(code,message,HttpStatus.SERVICE_UNAVAILABLE);}
        if("AI_PENDING_LIMIT_REACHED".equals(code))return exception;
        return error("AI_VARIANT_GENERATION_FAILED","AI 变式生成失败，未创建练习实例",HttpStatus.SERVICE_UNAVAILABLE);
    }
    private record Fact(long studentId,long questionId,String type,int difficulty){} private record Row(long id,long factId,long motherId,long questionId,String status,String variationMode,String type,String stem,int difficulty,String optionsJson,String correctAnswerJson,String studentAnswerJson,Boolean correct,String analysis,String reviewStatus,String noveltyDecision,Double noveltyScore,Double similarityScore,String noveltyReason){}
}
