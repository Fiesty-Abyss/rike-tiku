package com.neu.riketiku.sixin;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.sixin.dto.SiXinDtos.ContactResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.ConversationCreateRequest;
import com.neu.riketiku.sixin.dto.SiXinDtos.ConversationResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessagePageResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessageResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessageSendRequest;
import com.neu.riketiku.sixin.dto.SiXinDtos.ReadResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiXinFuWu {
    private final JdbcTemplate jdbc;

    public SiXinFuWu(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> contacts(RenZhengYongHu principal) {
        Actor actor = requireActor(principal.id());
        if (actor.teacherId != null) {
            return jdbc.query("""
                    SELECT r.id,p.id,p.xing_ming,b.ban_ji_ming_cheng,k.ke_mu_ming_cheng
                    FROM ren_ke_guan_xi r
                    JOIN ban_ji b ON b.id=r.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                    JOIN ke_mu k ON k.id=r.ke_mu_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                    JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id AND bx.shi_fou_zhu_ban_ji=1
                        AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                    JOIN xue_sheng_dang_an p ON p.id=bx.xue_sheng_id AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0
                    JOIN yong_hu u ON u.id=p.yong_hu_id AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                    WHERE r.jiao_shi_id=? AND r.zhuang_tai='ACTIVE'
                    ORDER BY b.ban_ji_ming_cheng,k.pai_xu,p.xue_hao,p.id
                    """, (rs, row) -> contact(rs), actor.teacherId);
        }
        return jdbc.query("""
                SELECT r.id,NULL,t.xing_ming,b.ban_ji_ming_cheng,k.ke_mu_ming_cheng
                FROM ban_ji_xue_sheng bx
                JOIN ban_ji b ON b.id=bx.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                JOIN ren_ke_guan_xi r ON r.ban_ji_id=bx.ban_ji_id AND r.zhuang_tai='ACTIVE'
                JOIN ke_mu k ON k.id=r.ke_mu_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id AND t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0
                JOIN yong_hu u ON u.id=t.yong_hu_id AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0
                WHERE bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1
                  AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                ORDER BY k.pai_xu,r.id
                """, (rs, row) -> contact(rs), actor.studentId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(RenZhengYongHu principal) {
        Actor actor = requireActor(principal.id());
        return jdbc.query(conversationSql() + """
                WHERE h.yi_shan_chu=0 AND (t.yong_hu_id=? OR p.yong_hu_id=?)
                ORDER BY h.zui_hou_xiao_xi_shi_jian IS NULL,h.zui_hou_xiao_xi_shi_jian DESC,h.id DESC
                """, (rs, row) -> conversation(rs, principal.id()), principal.id(),principal.id(),principal.id(), principal.id(), principal.id());
    }

    @Transactional
    public ConversationResponse create(RenZhengYongHu principal, ConversationCreateRequest request) {
        Actor actor = requireActor(principal.id());
        long studentId;
        if (actor.teacherId != null) {
            if (request.studentId() == null) {
                fail("MESSAGE_STUDENT_REQUIRED", "教师发起会话时必须选择学生", HttpStatus.BAD_REQUEST);
            }
            studentId = request.studentId();
            requireTeacherRelationship(actor.teacherId, request.teachingAssignmentId(), studentId);
        } else {
            studentId = actor.studentId;
            if (request.studentId() != null && request.studentId() != studentId) {
                fail("MESSAGE_PARTICIPANT_FORBIDDEN", "不能为其他学生建立会话", HttpStatus.FORBIDDEN);
            }
            requireStudentRelationship(studentId, request.teachingAssignmentId());
        }
        Long existing = jdbc.query("""
                SELECT id FROM si_xin_hui_hua
                WHERE ren_ke_guan_xi_id=? AND xue_sheng_id=? AND yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, request.teachingAssignmentId(), studentId);
        if (existing == null) {
            try {
                jdbc.update("INSERT INTO si_xin_hui_hua(ren_ke_guan_xi_id,xue_sheng_id) VALUES (?,?)",
                        request.teachingAssignmentId(), studentId);
                existing = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            } catch (DuplicateKeyException ignored) {
                existing = jdbc.queryForObject("SELECT id FROM si_xin_hui_hua WHERE ren_ke_guan_xi_id=? AND xue_sheng_id=?",
                        Long.class, request.teachingAssignmentId(), studentId);
            }
        }
        return requireConversation(principal.id(), existing).response(principal.id());
    }

    @Transactional(readOnly = true)
    public MessagePageResponse messages(RenZhengYongHu principal, long conversationId) {
        Conversation conversation = requireConversation(principal.id(), conversationId);
        List<MessageResponse> messages = jdbc.query("""
                SELECT m.id,m.fa_song_ren_yong_hu_id,
                       CASE WHEN m.fa_song_ren_yong_hu_id=t.yong_hu_id THEN t.xing_ming ELSE p.xing_ming END,
                       CASE WHEN m.che_hui_shi_jian IS NULL THEN m.nei_rong ELSE '消息已撤回' END,m.yi_du,m.fa_song_shi_jian,m.yi_du_shi_jian,
                       m.che_hui_shi_jian,
                       (m.fa_song_ren_yong_hu_id=? AND m.che_hui_shi_jian IS NULL AND TIMESTAMPDIFF(SECOND,m.fa_song_shi_jian,CURRENT_TIMESTAMP(3)) BETWEEN 0 AND 300)
                FROM si_xin_xiao_xi m
                JOIN si_xin_hui_hua h ON h.id=m.hui_hua_id
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id
                JOIN xue_sheng_dang_an p ON p.id=h.xue_sheng_id
                WHERE m.hui_hua_id=? AND m.yi_shan_chu=0
                  AND ((m.fa_song_ren_yong_hu_id=? AND m.fa_song_zhe_yi_cang=0) OR (m.fa_song_ren_yong_hu_id<>? AND m.jie_shou_zhe_yi_cang=0))
                ORDER BY m.fa_song_shi_jian,m.id
                """, (rs, row) -> new MessageResponse(rs.getLong(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), rs.getLong(2) == principal.id(), rs.getBoolean(5),
                rs.getTimestamp(6).toLocalDateTime(), rs.getTimestamp(7) == null ? null : rs.getTimestamp(7).toLocalDateTime(),
                rs.getTimestamp(8)!=null,rs.getBoolean(9)),
                principal.id(),conversationId,principal.id(),principal.id());
        return new MessagePageResponse(conversation.response(principal.id()), messages);
    }

    @Transactional
    public MessageResponse send(RenZhengYongHu principal, long conversationId, MessageSendRequest request) {
        Conversation conversation = requireConversation(principal.id(), conversationId);
        if (!conversation.canSend) {
            fail("MESSAGE_RELATIONSHIP_INACTIVE", "当前教学关系已失效，只能查看历史消息", HttpStatus.CONFLICT);
        }
        String content = request.content().trim();
        if (content.isEmpty()) {
            fail("MESSAGE_CONTENT_EMPTY", "消息内容不能为空", HttpStatus.BAD_REQUEST);
        }
        jdbc.update("INSERT INTO si_xin_xiao_xi(hui_hua_id,fa_song_ren_yong_hu_id,nei_rong) VALUES (?,?,?)",
                conversationId, principal.id(), content);
        Long messageId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE si_xin_hui_hua SET zui_hou_xiao_xi_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?", conversationId);
        return jdbc.query("""
                SELECT m.id,m.fa_song_ren_yong_hu_id,
                       CASE WHEN m.fa_song_ren_yong_hu_id=? THEN ? ELSE ? END,
                       m.nei_rong,m.yi_du,m.fa_song_shi_jian,m.yi_du_shi_jian
                FROM si_xin_xiao_xi m WHERE m.id=?
                """, rs -> {
            rs.next();
            return new MessageResponse(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), true,
                    rs.getBoolean(5), rs.getTimestamp(6).toLocalDateTime(), null,false,true);
        }, principal.id(), conversation.senderName(principal.id()), conversation.peerName(principal.id()), messageId);
    }

    @Transactional
    public ReadResponse read(RenZhengYongHu principal, long conversationId) {
        requireConversation(principal.id(), conversationId);
        int count = jdbc.update("""
                UPDATE si_xin_xiao_xi SET yi_du=1,yi_du_shi_jian=CURRENT_TIMESTAMP(3)
                WHERE hui_hua_id=? AND fa_song_ren_yong_hu_id<>? AND yi_du=0 AND yi_shan_chu=0
                """, conversationId, principal.id());
        return new ReadResponse(count);
    }

    @Transactional
    public MessageResponse recall(RenZhengYongHu principal,long conversationId,long messageId){
        Conversation conversation=requireConversation(principal.id(),conversationId);
        MessageAction row=messageForAction(conversationId,messageId);
        if(row.senderId()!=principal.id())fail("MESSAGE_RECALL_FORBIDDEN","只能撤回本人发送的消息",HttpStatus.FORBIDDEN);
        if(row.recalledAt()!=null)fail("MESSAGE_ALREADY_RECALLED","消息已经撤回",HttpStatus.CONFLICT);
        if(row.ageSeconds()<0||row.ageSeconds()>300)fail("MESSAGE_RECALL_WINDOW_EXPIRED","消息发送超过 5 分钟，不能撤回",HttpStatus.CONFLICT);
        jdbc.update("UPDATE si_xin_xiao_xi SET che_hui_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=? AND che_hui_shi_jian IS NULL",messageId);
        return new MessageResponse(messageId,principal.id(),conversation.senderName(principal.id()),"消息已撤回",true,row.read(),row.sentAt(),row.readAt(),true,false);
    }

    @Transactional
    public void hide(RenZhengYongHu principal,long conversationId,long messageId){
        requireConversation(principal.id(),conversationId);MessageAction row=messageForAction(conversationId,messageId);
        String column=row.senderId()==principal.id()?"fa_song_zhe_yi_cang":"jie_shou_zhe_yi_cang";
        jdbc.update("UPDATE si_xin_xiao_xi SET "+column+"=1 WHERE id=?",messageId);
    }

    private MessageAction messageForAction(long conversationId,long messageId){return jdbc.query("""
            SELECT fa_song_ren_yong_hu_id,che_hui_shi_jian,TIMESTAMPDIFF(SECOND,fa_song_shi_jian,CURRENT_TIMESTAMP(3)),yi_du,fa_song_shi_jian,yi_du_shi_jian
            FROM si_xin_xiao_xi WHERE id=? AND hui_hua_id=? AND yi_shan_chu=0 FOR UPDATE
            """,(rs,row)->new MessageAction(rs.getLong(1),rs.getTimestamp(2)==null?null:rs.getTimestamp(2).toLocalDateTime(),rs.getLong(3),rs.getBoolean(4),rs.getTimestamp(5).toLocalDateTime(),rs.getTimestamp(6)==null?null:rs.getTimestamp(6).toLocalDateTime()),messageId,conversationId).stream().findFirst().orElseThrow(()->new RenZhengYeWuYiChang("MESSAGE_NOT_FOUND","消息不存在",HttpStatus.NOT_FOUND));}

    private Actor requireActor(long userId) {
        Long teacherId = jdbc.query("""
                SELECT id FROM jiao_shi_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, userId);
        Long studentId = jdbc.query("""
                SELECT id FROM xue_sheng_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, rs -> rs.next() ? rs.getLong(1) : null, userId);
        if (teacherId == null && studentId == null) {
            fail("MESSAGE_ROLE_FORBIDDEN", "只有有效教师或学生可以使用私信", HttpStatus.FORBIDDEN);
        }
        return new Actor(teacherId, studentId);
    }

    private void requireTeacherRelationship(long teacherId, long scopeId, long studentId) {
        if (count("""
                SELECT COUNT(*) FROM ren_ke_guan_xi r
                JOIN ban_ji b ON b.id=r.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                JOIN ke_mu k ON k.id=r.ke_mu_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id AND bx.xue_sheng_id=?
                    AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                JOIN xue_sheng_dang_an p ON p.id=bx.xue_sheng_id AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0
                WHERE r.id=? AND r.jiao_shi_id=? AND r.zhuang_tai='ACTIVE'
                """, studentId, scopeId, teacherId) == 0) {
            fail("MESSAGE_RELATIONSHIP_FORBIDDEN", "该学生不在当前教师的有效任课班级中", HttpStatus.FORBIDDEN);
        }
    }

    private void requireStudentRelationship(long studentId, long scopeId) {
        if (count("""
                SELECT COUNT(*) FROM ban_ji_xue_sheng bx
                JOIN ren_ke_guan_xi r ON r.ban_ji_id=bx.ban_ji_id AND r.id=? AND r.zhuang_tai='ACTIVE'
                JOIN ban_ji b ON b.id=r.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                JOIN ke_mu k ON k.id=r.ke_mu_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id AND t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0
                WHERE bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1
                  AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                """, scopeId, studentId) == 0) {
            fail("MESSAGE_RELATIONSHIP_FORBIDDEN", "只能联系当前主班级的有效任课教师", HttpStatus.FORBIDDEN);
        }
    }

    private Conversation requireConversation(long userId, long conversationId) {
        Conversation conversation = jdbc.query(conversationSql() + " WHERE h.id=? AND h.yi_shan_chu=0",
                rs -> rs.next() ? conversationData(rs) : null, userId,userId,userId, conversationId);
        if (conversation == null) {
            fail("MESSAGE_CONVERSATION_NOT_FOUND", "会话不存在", HttpStatus.NOT_FOUND);
        }
        if (conversation.teacherUserId != userId && conversation.studentUserId != userId) {
            fail("MESSAGE_CONVERSATION_FORBIDDEN", "无权访问该会话", HttpStatus.FORBIDDEN);
        }
        return conversation;
    }

    private String conversationSql() {
        return """
                SELECT h.id,h.ren_ke_guan_xi_id,h.xue_sheng_id,t.yong_hu_id,p.yong_hu_id,
                       t.xing_ming,p.xing_ming,b.ban_ji_ming_cheng,k.ke_mu_ming_cheng,
                       (SELECT CASE WHEN m.che_hui_shi_jian IS NULL THEN m.nei_rong ELSE '消息已撤回' END FROM si_xin_xiao_xi m
                         WHERE m.hui_hua_id=h.id AND m.yi_shan_chu=0
                           AND ((m.fa_song_ren_yong_hu_id=? AND m.fa_song_zhe_yi_cang=0) OR (m.fa_song_ren_yong_hu_id<>? AND m.jie_shou_zhe_yi_cang=0))
                         ORDER BY m.fa_song_shi_jian DESC,m.id DESC LIMIT 1),
                       h.zui_hou_xiao_xi_shi_jian,
                       (SELECT COUNT(*) FROM si_xin_xiao_xi m WHERE m.hui_hua_id=h.id AND m.yi_du=0 AND m.yi_shan_chu=0
                         AND m.che_hui_shi_jian IS NULL AND m.jie_shou_zhe_yi_cang=0 AND m.fa_song_ren_yong_hu_id<>?),
                       CASE WHEN h.zhuang_tai='ACTIVE' AND r.zhuang_tai='ACTIVE'
                         AND t.zhuang_tai='ACTIVE' AND t.yi_shan_chu=0 AND tu.zhang_hao_zhuang_tai='ENABLED' AND tu.yi_shan_chu=0
                         AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0 AND pu.zhang_hao_zhuang_tai='ENABLED' AND pu.yi_shan_chu=0
                         AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                         AND EXISTS (SELECT 1 FROM ban_ji_xue_sheng bx WHERE bx.xue_sheng_id=h.xue_sheng_id
                           AND bx.ban_ji_id=r.ban_ji_id AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL)
                       THEN 1 ELSE 0 END
                FROM si_xin_hui_hua h
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id JOIN yong_hu tu ON tu.id=t.yong_hu_id
                JOIN xue_sheng_dang_an p ON p.id=h.xue_sheng_id JOIN yong_hu pu ON pu.id=p.yong_hu_id
                JOIN ban_ji b ON b.id=r.ban_ji_id JOIN ke_mu k ON k.id=r.ke_mu_id
                """;
    }

    private ConversationResponse conversation(ResultSet rs, long userId) throws SQLException {
        return conversationData(rs).response(userId);
    }

    private Conversation conversationData(ResultSet rs) throws SQLException {
        return new Conversation(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                rs.getTimestamp(11) == null ? null : rs.getTimestamp(11).toLocalDateTime(), rs.getLong(12), rs.getBoolean(13));
    }

    private ContactResponse contact(ResultSet rs) throws SQLException {
        Long studentId = rs.getObject(2) == null ? null : rs.getLong(2);
        return new ContactResponse(rs.getLong(1), studentId, rs.getString(3), rs.getString(4), rs.getString(5));
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private void fail(String code, String message, HttpStatus status) {
        throw new RenZhengYeWuYiChang(code, message, status);
    }

    private record Actor(Long teacherId, Long studentId) {
    }
    private record MessageAction(long senderId,java.time.LocalDateTime recalledAt,long ageSeconds,boolean read,java.time.LocalDateTime sentAt,java.time.LocalDateTime readAt){}

    private record Conversation(long id, long scopeId, long studentId, long teacherUserId, long studentUserId,
            String teacherName, String studentName, String className, String subjectName, String latestMessage,
            java.time.LocalDateTime latestTime, long unreadCount, boolean canSend) {
        ConversationResponse response(long userId) {
            return new ConversationResponse(id, scopeId, studentId, peerName(userId), className, subjectName,
                    latestMessage, latestTime, unreadCount, canSend);
        }

        String peerName(long userId) {
            return userId == teacherUserId ? studentName : teacherName;
        }

        String senderName(long userId) {
            return userId == teacherUserId ? teacherName : studentName;
        }
    }
}
