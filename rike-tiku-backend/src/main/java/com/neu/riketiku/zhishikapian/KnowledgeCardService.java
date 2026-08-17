package com.neu.riketiku.zhishikapian;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeCardService {
    private static final Set<String> TYPES=Set.of("POINT","FORMULA","CHEMICAL_EQUATION","SECONDARY_CONCLUSION","INSTRUMENT","MNEMONIC","TABLE","NOTE");
    private static final Set<String> RIGHTS=Set.of("PROJECT_AUTHORED","PUBLIC_DOMAIN","AUTHORIZED","USER_PROVIDED");
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentStorage storage;
    public KnowledgeCardService(JdbcTemplate jdbc,QuestionAttachmentStorage storage){this.jdbc=jdbc;this.storage=storage;}

    @Transactional
    public KnowledgeCardDtos.Card create(long user,long scopeId,KnowledgeCardDtos.Save request){Scope scope=scope(user,scopeId);validate(request,scope.subjectId());String status=request.aiDraft()?"PENDING":"PUBLISHED";jdbc.update("""
            INSERT INTO gao_pin_kao_dian(ren_ke_guan_xi_id,zhi_shi_dian_id,zi_liao_lei_xing,biao_ti,nei_rong,ke_xue_nei_rong,latex_nei_rong,shi_yong_tiao_jian,han_yi_tui_dao,chang_jian_wu_qu,li_zi,ji_yi_kou_jue,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,chuang_jian_ren_yong_hu_id,pai_xu,zhuang_tai)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,scopeId,request.knowledgePointIds().getFirst(),request.type(),request.title().trim(),request.content().trim(),request.content().trim(),trim(request.latex()),trim(request.applicableConditions()),trim(request.derivation()),trim(request.commonMistake()),trim(request.example()),trim(request.mnemonic()),trim(request.sourceName()),trim(request.sourceUrl()),request.rightsStatus(),user,request.sortOrder(),status);long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);int order=1;for(long point:request.knowledgePointIds())jdbc.update("INSERT INTO gao_pin_kao_dian_zhi_shi_dian(gao_pin_kao_dian_id,zhi_shi_dian_id,pai_xu) VALUES (?,?,?)",id,point,order++);if(request.aiDraft())jdbc.update("INSERT INTO gao_pin_kao_dian_shen_he_ji_lu(gao_pin_kao_dian_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_yong_hu_id,shen_he_yi_jian) VALUES (?,'SUBMIT','PENDING','PENDING',?,'AI整理草稿待人工审核')",id,user);return teacherCard(user,id);}

    @Transactional
    public KnowledgeCardDtos.Card review(long user,long id,KnowledgeCardDtos.Review request){teacherCard(user,id);String current=jdbc.queryForObject("SELECT zhuang_tai FROM gao_pin_kao_dian WHERE id=? FOR UPDATE",String.class,id);String target=switch(request.action()){case "APPROVE"->"PUBLISHED";case "REJECT"->"DISABLED";case "DISABLE"->"DISABLED";default->throw error("KNOWLEDGE_CARD_REVIEW_INVALID","审核动作不合法",HttpStatus.BAD_REQUEST);};if("APPROVE".equals(request.action())&&!"PENDING".equals(current))throw error("KNOWLEDGE_CARD_REVIEW_STATE_INVALID","只能通过待审核卡片",HttpStatus.CONFLICT);jdbc.update("UPDATE gao_pin_kao_dian SET zhuang_tai=? WHERE id=?",target,id);jdbc.update("INSERT INTO gao_pin_kao_dian_shen_he_ji_lu(gao_pin_kao_dian_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_yong_hu_id,shen_he_yi_jian) VALUES (?,?,?,?,?,?)",id,request.action(),current,target,user,trim(request.comment()));return teacherCard(user,id);}

    @Transactional(readOnly=true)
    public List<KnowledgeCardDtos.Card> teacherList(long user,long scopeId){scope(user,scopeId);return cards("WHERE h.ren_ke_guan_xi_id=? AND h.yi_shan_chu=0",null,scopeId);}

    @Transactional(readOnly=true)
    public List<KnowledgeCardDtos.Card> studentList(long user,Long subjectId,Long pointId,String type,Boolean favorite,String mastery){long student=student(user);List<Object>args=new ArrayList<>();StringBuilder where=new StringBuilder("""
            WHERE h.zhuang_tai='PUBLISHED' AND h.yi_shan_chu=0 AND bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
            """);args.add(student);if(subjectId!=null){where.append(" AND r.ke_mu_id=?");args.add(subjectId);}if(pointId!=null){where.append(" AND EXISTS(SELECT 1 FROM gao_pin_kao_dian_zhi_shi_dian hp WHERE hp.gao_pin_kao_dian_id=h.id AND hp.zhi_shi_dian_id=?)");args.add(pointId);}if(type!=null&&!type.isBlank()){where.append(" AND h.zi_liao_lei_xing=?");args.add(type.trim());}if(favorite!=null){where.append(" AND COALESCE(st.shi_fou_shou_cang,0)=?");args.add(favorite);}if(mastery!=null&&!mastery.isBlank()){where.append(" AND COALESCE(st.zhang_wo_zhuang_tai,'LEARNING')=?");args.add(mastery.trim());}return cards(where.toString(),student,args.toArray());}

    @Transactional
    public KnowledgeCardDtos.Card state(long user,long cardId,KnowledgeCardDtos.State request){long student=student(user);visible(student,cardId);if(!Set.of("LEARNING","MASTERED").contains(request.mastery()))throw error("KNOWLEDGE_CARD_MASTERY_INVALID","掌握状态不合法",HttpStatus.BAD_REQUEST);jdbc.update("""
            INSERT INTO xue_sheng_zhi_shi_ka_pian_zhuang_tai(xue_sheng_id,gao_pin_kao_dian_id,shi_fou_shou_cang,zhang_wo_zhuang_tai) VALUES (?,?,?,?)
            ON DUPLICATE KEY UPDATE shi_fou_shou_cang=VALUES(shi_fou_shou_cang),zhang_wo_zhuang_tai=VALUES(zhang_wo_zhuang_tai)
            """,student,cardId,request.favorite(),request.mastery());return studentCard(student,cardId);}

    @Transactional(readOnly=true)
    public List<KnowledgeCardDtos.Frequency> frequency(long user,long cardId){long student=student(user);visible(student,cardId);long point=jdbc.queryForObject("SELECT zhi_shi_dian_id FROM gao_pin_kao_dian WHERE id=?",Long.class,cardId);List<KnowledgeCardDtos.Frequency> result=new ArrayList<>();for(int years:List.of(3,5)){MapCount count=jdbc.query("""
            SELECT COUNT(DISTINCT q.id),COUNT(DISTINCT CONCAT(COALESCE(s.shi_juan_ming_cheng,''),'|',COALESCE(s.nian_fen,0),'|',COALESCE(s.di_qu,'')))
            FROM ti_mu q JOIN ti_mu_zhi_shi_dian qp ON qp.ti_mu_id=q.id AND qp.zhi_shi_dian_id=? AND qp.yi_shan_chu=0
            JOIN ti_mu_lai_yuan s ON s.ti_mu_id=q.id AND s.nei_rong_lei_xing='QUESTION' AND s.lai_yuan_lei_xing='REAL_EXAM'
              AND s.quan_li_zhuang_tai IN ('AUTHORIZED','OPEN_LICENSE','PUBLIC_OFFICIAL') AND s.nian_fen IS NOT NULL
              AND s.nian_fen>=YEAR(CURRENT_DATE)-?
            WHERE q.ti_gan NOT LIKE '【演示】%' AND q.ti_gan NOT LIKE '【专题演示】%'
            """,rs->{rs.next();return new MapCount(rs.getLong(1),rs.getLong(2));},point,years);result.add(new KnowledgeCardDtos.Frequency("最近"+years+"年",count.occurrences,count.papers,count.occurrences<3?"当前真题样本不足，以下为课程核心知识点":null));}return result;}

    @Transactional
    public KnowledgeCardDtos.Attachment upload(long user,long cardId,MultipartFile file){teacherCard(user,cardId);if(file==null||file.isEmpty())throw error("KNOWLEDGE_CARD_ATTACHMENT_REQUIRED","请选择图片",HttpStatus.BAD_REQUEST);QuestionAttachmentStorage.StoredImage image;try{image=storage.store(file.getOriginalFilename(),file.getBytes());}catch(java.io.IOException exception){throw error("KNOWLEDGE_CARD_ATTACHMENT_INVALID","图片读取失败",HttpStatus.UNPROCESSABLE_ENTITY);}if(!Set.of("image/png","image/jpeg").contains(image.mime())){storage.delete(image.relativePath());throw error("KNOWLEDGE_CARD_ATTACHMENT_INVALID","知识卡片图片仅支持 PNG 或 JPEG",HttpStatus.UNPROCESSABLE_ENTITY);}try{jdbc.update("INSERT INTO gao_pin_kao_dian_fu_jian(gao_pin_kao_dian_id,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,mime_lei_xing,nei_rong_ha_xi,wen_jian_da_xiao,pai_xu) VALUES (?,?,?,?,?,?,COALESCE((SELECT MAX(x.pai_xu)+1 FROM gao_pin_kao_dian_fu_jian x WHERE x.gao_pin_kao_dian_id=?),1))",cardId,file.getOriginalFilename(),image.relativePath(),image.mime(),image.hash(),image.bytes().length,cardId);long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);return new KnowledgeCardDtos.Attachment(id,file.getOriginalFilename(),image.mime(),image.bytes().length,"/api/v1/student/knowledge-cards/"+cardId+"/attachments/"+id+"/content");}catch(RuntimeException exception){storage.delete(image.relativePath());throw exception;}}

    @Transactional(readOnly=true)
    public QuestionAttachmentStorage.StoredImage content(long user,long cardId,long attachmentId,boolean teacher){if(teacher)teacherCard(user,cardId);else visible(student(user),cardId);AttachmentFile row=jdbc.query("SELECT xiang_dui_lu_jing,nei_rong_ha_xi,mime_lei_xing,zhuang_tai FROM gao_pin_kao_dian_fu_jian WHERE id=? AND gao_pin_kao_dian_id=? AND yi_shan_chu=0",(rs,n)->new AttachmentFile(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)),attachmentId,cardId).stream().findFirst().orElseThrow(()->error("KNOWLEDGE_CARD_ATTACHMENT_NOT_FOUND","图片不存在或无权访问",HttpStatus.NOT_FOUND));if(!"ACTIVE".equals(row.status()))throw error("KNOWLEDGE_CARD_ATTACHMENT_NOT_FOUND","图片不存在或无权访问",HttpStatus.NOT_FOUND);return storage.read(row.path(),row.hash());}

    private KnowledgeCardDtos.Card teacherCard(long user,long id){long scopeId=jdbc.queryForObject("SELECT ren_ke_guan_xi_id FROM gao_pin_kao_dian WHERE id=? AND yi_shan_chu=0",Long.class,id);scope(user,scopeId);return cards("WHERE h.id=? AND h.yi_shan_chu=0",null,id).getFirst();}
    private KnowledgeCardDtos.Card studentCard(long student,long id){visible(student,id);return cards("WHERE h.id=? AND h.yi_shan_chu=0",student,id).getFirst();}
    private void visible(long student,long id){Long n=jdbc.queryForObject("""
            SELECT COUNT(*) FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id WHERE h.id=? AND h.zhuang_tai='PUBLISHED' AND h.yi_shan_chu=0 AND bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
            """,Long.class,id,student);if(n==0)throw error("KNOWLEDGE_CARD_NOT_FOUND","卡片不存在或不属于当前班级",HttpStatus.NOT_FOUND);}
    private List<KnowledgeCardDtos.Card> cards(String where,Long student,Object...args){String classJoin=student==null?"":"JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id";String stateJoin=student==null?"LEFT JOIN xue_sheng_zhi_shi_ka_pian_zhuang_tai st ON 1=0":"LEFT JOIN xue_sheng_zhi_shi_ka_pian_zhuang_tai st ON st.gao_pin_kao_dian_id=h.id AND st.xue_sheng_id="+student;return jdbc.query("""
            SELECT h.id,r.ke_mu_id,k.ke_mu_dai_ma,k.ke_mu_ming_cheng,r.id,b.ban_ji_ming_cheng,h.zi_liao_lei_xing,h.biao_ti,h.nei_rong,h.latex_nei_rong,h.shi_yong_tiao_jian,h.han_yi_tui_dao,h.chang_jian_wu_qu,h.li_zi,h.ji_yi_kou_jue,h.lai_yuan_ming_cheng,h.lai_yuan_di_zhi,h.quan_li_zhuang_tai,h.zhuang_tai,h.pai_xu,COALESCE(st.shi_fou_shou_cang,0),COALESCE(st.zhang_wo_zhuang_tai,'LEARNING')
            FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ke_mu k ON k.id=r.ke_mu_id JOIN ban_ji b ON b.id=r.ban_ji_id
            """+classJoin+" "+stateJoin+" "+where+" ORDER BY h.pai_xu,h.id",(rs,row)->new KnowledgeCardDtos.Card(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getLong(5),rs.getString(6),rs.getString(7),rs.getString(8),points(rs.getLong(1)),rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12),rs.getString(13),rs.getString(14),rs.getString(15),rs.getString(16),rs.getString(17),rs.getString(18),rs.getString(19),rs.getInt(20),rs.getBoolean(21),rs.getString(22),attachments(rs.getLong(1))),args);}
    private List<KnowledgeCardDtos.Point>points(long id){return jdbc.query("SELECT p.id,p.zhi_shi_dian_ming_cheng,p.wan_zheng_lu_jing FROM gao_pin_kao_dian_zhi_shi_dian hp JOIN zhi_shi_dian p ON p.id=hp.zhi_shi_dian_id WHERE hp.gao_pin_kao_dian_id=? ORDER BY hp.pai_xu",(rs,row)->new KnowledgeCardDtos.Point(rs.getLong(1),rs.getString(2),rs.getString(3)),id);}
    private List<KnowledgeCardDtos.Attachment>attachments(long id){return jdbc.query("SELECT id,yuan_shi_wen_jian_ming,mime_lei_xing,wen_jian_da_xiao FROM gao_pin_kao_dian_fu_jian WHERE gao_pin_kao_dian_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0 ORDER BY pai_xu",(rs,row)->new KnowledgeCardDtos.Attachment(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),"/api/v1/student/knowledge-cards/"+id+"/attachments/"+rs.getLong(1)+"/content"),id);}
    private void validate(KnowledgeCardDtos.Save r,long subject){if(!TYPES.contains(r.type()))throw error("KNOWLEDGE_CARD_TYPE_INVALID","卡片类型不合法",HttpStatus.BAD_REQUEST);if(!RIGHTS.contains(r.rightsStatus()))throw error("KNOWLEDGE_CARD_RIGHTS_INVALID","来源权利状态不合法",HttpStatus.BAD_REQUEST);long count=jdbc.queryForObject("SELECT COUNT(*) FROM zhi_shi_dian WHERE id IN ("+String.join(",",java.util.Collections.nCopies(r.knowledgePointIds().size(),"?"))+") AND ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",Long.class,concat(r.knowledgePointIds(),subject));if(count!=r.knowledgePointIds().stream().distinct().count())throw error("KNOWLEDGE_CARD_POINT_INVALID","知识点必须属于任教学科",HttpStatus.BAD_REQUEST);}
    private Scope scope(long user,long id){return jdbc.query("SELECT r.id,r.ke_mu_id FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=? AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE'",(rs,row)->new Scope(rs.getLong(1),rs.getLong(2)),id,user).stream().findFirst().orElseThrow(()->error("KNOWLEDGE_CARD_SCOPE_FORBIDDEN","任课范围不属于当前教师",HttpStatus.FORBIDDEN));}
    private long student(long user){return jdbc.query("SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",(rs,row)->rs.getLong(1),user).stream().findFirst().orElseThrow(()->error("STUDENT_PROFILE_REQUIRED","学生档案不可用",HttpStatus.FORBIDDEN));}
    private Object[] concat(List<Long> ids,long subject){List<Object>all=new ArrayList<>(ids);all.add(subject);return all.toArray();}
    private String trim(String value){return value==null?null:value.trim();}
    private RenZhengYeWuYiChang error(String code,String message,HttpStatus status){return new RenZhengYeWuYiChang(code,message,status);}
    private record Scope(long id,long subjectId){}private record MapCount(long occurrences,long papers){}private record AttachmentFile(String path,String hash,String mime,String status){}
}
