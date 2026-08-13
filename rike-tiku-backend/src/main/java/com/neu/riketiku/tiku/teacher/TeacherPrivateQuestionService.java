package com.neu.riketiku.tiku.teacher;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherPrivateQuestionService{
 private final JdbcTemplate jdbc;private final QuestionAdminService questions;
 public TeacherPrivateQuestionService(JdbcTemplate jdbc,QuestionAdminService questions){this.jdbc=jdbc;this.questions=questions;}
 @Transactional(readOnly=true) public List<Item> list(long userId){return jdbc.query("""
  SELECT q.id,q.ren_ke_guan_xi_id,b.ban_ji_ming_cheng,k.ke_mu_ming_cheng,q.ti_mu_lei_xing,q.ti_gan,q.zhuang_tai
  FROM ti_mu q JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id
  JOIN ban_ji b ON b.id=r.ban_ji_id JOIN ke_mu k ON k.id=r.ke_mu_id
  WHERE j.yong_hu_id=? AND q.chuang_jian_ren_id=? AND q.ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE' AND q.yi_shan_chu=0 ORDER BY q.id DESC
  """,(rs,n)->new Item(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)),userId,userId);}
 @Transactional public Item create(long userId,long scopeId,QuestionDtos.Save save){authorize(userId,scopeId,save.subjectId());var detail=questions.create(save,userId);jdbc.update("UPDATE ti_mu SET ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE',ren_ke_guan_xi_id=? WHERE id=?",scopeId,detail.question().id());return one(userId,detail.question().id());}
 @Transactional public Item publish(long userId,long id){Item item=one(userId,id);if(!"DRAFT".equals(item.status()))throw fail("PRIVATE_QUESTION_STATUS_INVALID","只有草稿可发布到班级",HttpStatus.CONFLICT);jdbc.update("UPDATE ti_mu SET zhuang_tai='PUBLISHED' WHERE id=?",id);jdbc.update("UPDATE ti_mu_jie_xi SET zhuang_tai='PUBLISHED' WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'",id);return one(userId,id);}
 private Item one(long userId,long id){return list(userId).stream().filter(x->x.id()==id).findFirst().orElseThrow(()->fail("PRIVATE_QUESTION_NOT_FOUND","题目不存在",HttpStatus.NOT_FOUND));}
 private void authorize(long userId,long scopeId,long subjectId){Long n=jdbc.queryForObject("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=? AND r.ke_mu_id=? AND r.zhuang_tai='ACTIVE' AND j.yong_hu_id=?",Long.class,scopeId,subjectId);if(n==null||n!=1)throw fail("PRIVATE_SCOPE_FORBIDDEN","任课范围无效",HttpStatus.FORBIDDEN);}
 private RenZhengYeWuYiChang fail(String c,String m,HttpStatus s){return new RenZhengYeWuYiChang(c,m,s);}
 public record Item(long id,long teachingAssignmentId,String className,String subjectName,String questionType,String stem,String status){}
}
