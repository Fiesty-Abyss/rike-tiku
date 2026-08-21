package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.jiaoshi.dto.*;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.zhanghao.AdminDefaultPasswordPolicy;
import com.neu.riketiku.zhanghao.entity.YongHu;
import com.neu.riketiku.zhanghao.mapper.YongHuMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JiaoShiGuanLiFuWu {
    private static final Set<String> SUBJECT_CODES = Set.of("PHYSICS", "CHEMISTRY", "BIOLOGY");
    private final JdbcTemplate jdbcTemplate;
    private final YongHuMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;
    private final AdminDefaultPasswordPolicy defaultPasswordPolicy;

    public JiaoShiGuanLiFuWu(JdbcTemplate jdbcTemplate, YongHuMapper userMapper,
            PasswordEncoder passwordEncoder,
            GuanLiCaoZuoRiZhiFuWu auditLog, AdminDefaultPasswordPolicy defaultPasswordPolicy) {
        this.jdbcTemplate = jdbcTemplate; this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
        this.defaultPasswordPolicy = defaultPasswordPolicy;
    }

    @Transactional(readOnly = true)
    public JiaoShiFenYeXiangYing page(long page, long size, String employeeNumber, String name, String username,
            String accountStatus, String profileStatus) {
        List<Object> args = new ArrayList<>();
        String where = filters(args, employeeNumber, name, username, accountStatus, profileStatus);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jiao_shi_dang_an p JOIN yong_hu u ON u.id=p.yong_hu_id " + where, args.toArray(), Long.class);
        args.add(size); args.add((page - 1) * size);
        List<JiaoShiXiangYing> records = jdbcTemplate.query("""
                SELECT p.id,p.gong_hao,p.xing_ming,p.xian_shi_zhi_wu,u.yong_hu_ming,u.zhang_hao_zhuang_tai,p.zhuang_tai
                FROM jiao_shi_dang_an p JOIN yong_hu u ON u.id=p.yong_hu_id """ + where
                + " ORDER BY p.id DESC LIMIT ? OFFSET ?", teacherMapper(), args.toArray());
        return new JiaoShiFenYeXiangYing(records, total, page, size, (total + size - 1) / size);
    }

    @Transactional(readOnly = true)
    public JiaoShiXiangQingXiangYing get(Long teacherId) {
        JiaoShiXiangYing teacher = findTeacher(teacherId);
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT r.jiao_se_dai_ma FROM yong_hu_jiao_se ur JOIN jiao_se r ON r.id=ur.jiao_se_id
                JOIN jiao_shi_dang_an p ON p.yong_hu_id=ur.yong_hu_id
                WHERE p.id=? AND ur.zhuang_tai='ACTIVE' AND r.zhuang_tai='ACTIVE' AND r.yi_shan_chu=0
                ORDER BY r.id""", String.class, teacherId);
        return new JiaoShiXiangQingXiangYing(teacher, roles, assignments(teacherId));
    }

    @Transactional
    public JiaoShiChuangJianXiangYing create(JiaoShiChuangJianQingQiu request) {
        return auditLog.audited("TEACHER", "CREATE", null, "管理员创建教师账号和档案", () -> createInternal(request), result -> result.teacher().id());
    }

    private JiaoShiChuangJianXiangYing createInternal(JiaoShiChuangJianQingQiu request) {
        String employeeNumber = trim(request.employeeNumber()); String username = trim(request.username());
        if (exists("SELECT COUNT(*) FROM jiao_shi_dang_an WHERE gong_hao=?", employeeNumber)) fail("TEACHER_NUMBER_EXISTS", "工号已存在", HttpStatus.CONFLICT);
        if (exists("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming=?", username)) fail("USERNAME_EXISTS", "用户名已存在", HttpStatus.CONFLICT);
        Long roleId = jdbcTemplate.query("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='TEACHER' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", rs -> rs.next() ? rs.getLong(1) : null);
        if (roleId == null) fail("TEACHER_ROLE_UNAVAILABLE", "TEACHER角色不存在或已停用", HttpStatus.CONFLICT);
        String password = request.initialPassword() == null || request.initialPassword().isBlank() ? defaultPasswordPolicy.password() : request.initialPassword();
        validatePassword(password);
        try {
            YongHu user = new YongHu(); user.setYongHuMing(username); user.setMiMaZhaiYao(passwordEncoder.encode(password));
            user.setZhangHaoZhuangTai(request.accountStatus()); user.setShiFouShouCiDengLu(false); userMapper.insert(user);
            jdbcTemplate.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id,zhuang_tai) VALUES (?,?,'ACTIVE')", user.getId(), roleId);
            jdbcTemplate.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming,xian_shi_zhi_wu,zhuang_tai) VALUES (?,?,?,?, 'ACTIVE')",
                    user.getId(), employeeNumber, trim(request.name()), emptyToNull(request.displayPosition()));
            Long teacherId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            return new JiaoShiChuangJianXiangYing(findTeacher(teacherId), password);
        } catch (DataIntegrityViolationException exception) {
            fail("TEACHER_CREATE_CONFLICT", "教师信息与当前数据库状态冲突", HttpStatus.CONFLICT);
            throw exception;
        }
    }

    @Transactional
    public JiaoShiXiangYing update(Long teacherId, JiaoShiXiuGaiQingQiu request) {
        return update(teacherId, request, -1L);
    }

    @Transactional
    public JiaoShiXiangYing update(Long teacherId, JiaoShiXiuGaiQingQiu request, long actor) {
        return auditLog.audited("TEACHER", "UPDATE", teacherId, "管理员修改教师档案或账号状态", () -> updateInternal(teacherId, request, actor));
    }

    private JiaoShiXiangYing updateInternal(Long teacherId, JiaoShiXiuGaiQingQiu request, long actor) {
        JiaoShiXiangYing teacher = findTeacher(teacherId);
        Long targetUser = jdbcTemplate.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, teacherId);
        if (targetUser != null && targetUser == actor && (!"ENABLED".equals(request.accountStatus()) || !"ACTIVE".equals(request.profileStatus()))) {
            fail("ADMIN_SELF_DISABLE_FORBIDDEN", "当前管理员不能在当前会话中停用自己的账号或教师档案", HttpStatus.CONFLICT);
        }
        jdbcTemplate.update("UPDATE yong_hu SET zhang_hao_zhuang_tai=? WHERE id=(SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?)", request.accountStatus(), teacherId);
        jdbcTemplate.update("UPDATE jiao_shi_dang_an SET xing_ming=?,xian_shi_zhi_wu=?,zhuang_tai=? WHERE id=?",
                trim(request.name()), emptyToNull(request.displayPosition()), request.profileStatus(), teacherId);
        return findTeacher(teacherId);
    }

    @Transactional
    public JiaoShiMiMaChongZhiXiangYing resetPassword(Long teacherId) {
        return auditLog.audited("TEACHER", "RESET_PASSWORD", teacherId,
                "管理员恢复教师默认密码（不记录密码）", () -> resetPasswordInternal(teacherId));
    }

    private JiaoShiMiMaChongZhiXiangYing resetPasswordInternal(Long teacherId) {
        JiaoShiXiangYing teacher = findTeacher(teacherId);
        String password = defaultPasswordPolicy.password();
        jdbcTemplate.update("""
                UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0,mi_ma_xiu_gai_shi_jian=NULL
                WHERE yong_hu_ming=? AND yi_shan_chu=0
                """, passwordEncoder.encode(password), teacher.username());
        return new JiaoShiMiMaChongZhiXiangYing(1, password, false);
    }

    @Transactional
    public JiaoShiMiMaChongZhiXiangYing resetPasswords(List<Long> requestedIds) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(requestedIds));
        String summary = "管理员批量恢复教师默认密码；数量=" + ids.size() + "；目标业务ID=" + ids;
        return auditLog.audited("TEACHER", "BATCH_RESET_PASSWORD", null, summary,
                () -> resetPasswordsInternal(ids));
    }

    private JiaoShiMiMaChongZhiXiangYing resetPasswordsInternal(List<Long> ids) {
        List<JiaoShiXiangYing> teachers = ids.stream().map(this::findTeacher).toList();
        String password = defaultPasswordPolicy.password();
        for (JiaoShiXiangYing teacher : teachers) {
            jdbcTemplate.update("""
                    UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0,mi_ma_xiu_gai_shi_jian=NULL
                    WHERE yong_hu_ming=? AND yi_shan_chu=0
                    """, passwordEncoder.encode(password), teacher.username());
        }
        return new JiaoShiMiMaChongZhiXiangYing(teachers.size(), password, false);
    }

    @Transactional(readOnly = true)
    public List<KeMuXiangYing> subjects() {
        return jdbcTemplate.query("SELECT id,ke_mu_dai_ma,ke_mu_ming_cheng FROM ke_mu WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0 ORDER BY pai_xu,id",
                (rs, row) -> new KeMuXiangYing(rs.getLong(1), rs.getString(2), rs.getString(3))).stream()
                .filter(subject -> SUBJECT_CODES.contains(subject.subjectCode())).toList();
    }

    @Transactional(readOnly = true)
    public List<RenKeXiangYing> assignments(Long teacherId) { findTeacher(teacherId); return assignmentsInternal(teacherId); }

    @Transactional
    public RenKeXiangYing createAssignment(Long teacherId, RenKeChuangJianQingQiu request) {
        return auditLog.audited("TEACHING_ASSIGNMENT", "CREATE", null, "管理员创建教师班级科目任课关系", () -> createAssignmentInternal(teacherId, request));
    }

    private RenKeXiangYing createAssignmentInternal(Long teacherId, RenKeChuangJianQingQiu request) {
        JiaoShiXiangYing teacher = findTeacher(teacherId);
        if (!"ENABLED".equals(teacher.accountStatus()) || !"ACTIVE".equals(teacher.profileStatus())) fail("TEACHER_UNAVAILABLE", "教师账号或档案不是有效状态", HttpStatus.CONFLICT);
        if (!exists("SELECT COUNT(*) FROM ban_ji WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", request.classId())) fail("CLASS_UNAVAILABLE", "班级不存在或不是ACTIVE状态", HttpStatus.CONFLICT);
        String subjectCode = jdbcTemplate.query("SELECT ke_mu_dai_ma FROM ke_mu WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", rs -> rs.next() ? rs.getString(1) : null, request.subjectId());
        if (subjectCode == null || !SUBJECT_CODES.contains(subjectCode)) fail("SUBJECT_UNAVAILABLE", "科目不存在、未启用或不在高中理科范围内", HttpStatus.CONFLICT);
        if (exists("SELECT COUNT(*) FROM ren_ke_guan_xi WHERE jiao_shi_id=? AND ban_ji_id=? AND ke_mu_id=?", teacherId, request.classId(), request.subjectId())) fail("TEACHING_ASSIGNMENT_EXISTS", "该教师、班级、科目的任课关系已存在；历史关系不能重复创建", HttpStatus.CONFLICT);
        jdbcTemplate.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,?,?, 'ACTIVE',?)",
                teacherId, request.classId(), request.subjectId(), request.primary(), request.startTime());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return assignment(id);
    }

    @Transactional
    public RenKeXiangYing changeAssignmentStatus(Long assignmentId, RenKeZhuangTaiQingQiu request) {
        return auditLog.audited("TEACHING_ASSIGNMENT", "STATUS_CHANGE", assignmentId, "管理员结束或停用教师班级科目任课关系", () -> changeAssignmentStatusInternal(assignmentId, request));
    }
    @Transactional
    public JiaoShiXiangQingXiangYing grantAdmin(Long teacherId, long actor) {
        findTeacher(teacherId);
        Long user = jdbcTemplate.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, teacherId);
        Long role = jdbcTemplate.queryForObject("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='ADMIN' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", Long.class);
        if (!exists("SELECT COUNT(*) FROM yong_hu_jiao_se WHERE yong_hu_id=? AND jiao_se_id=? AND zhuang_tai='ACTIVE'", user, role)) {
            int restored = jdbcTemplate.update("UPDATE yong_hu_jiao_se SET zhuang_tai='ACTIVE' WHERE yong_hu_id=? AND jiao_se_id=? AND zhuang_tai='DISABLED'", user, role);
            if (restored == 0) jdbcTemplate.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id,zhuang_tai) VALUES (?,?, 'ACTIVE')", user, role);
        }
        return get(teacherId);
    }

    @Transactional
    public JiaoShiXiangQingXiangYing revokeAdmin(Long teacherId, long actor) {
        Long user = jdbcTemplate.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, teacherId);
        if (user == actor) fail("ADMIN_SELF_REVOKE_FORBIDDEN", "当前管理员不能撤销自己的管理员权限", HttpStatus.CONFLICT);
        Long admins = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT ur.yong_hu_id) FROM yong_hu_jiao_se ur JOIN jiao_se r ON r.id=ur.jiao_se_id JOIN yong_hu u ON u.id=ur.yong_hu_id WHERE r.jiao_se_dai_ma='ADMIN' AND ur.zhuang_tai='ACTIVE' AND u.zhang_hao_zhuang_tai='ENABLED' AND u.yi_shan_chu=0", Long.class);
        if (admins == null || admins <= 1) fail("LAST_ADMIN_REQUIRED", "系统至少需要保留一名可用管理员，请先授予其他教师管理员权限。", HttpStatus.CONFLICT);
        jdbcTemplate.update("UPDATE yong_hu_jiao_se SET zhuang_tai='DISABLED' WHERE yong_hu_id=? AND jiao_se_id=(SELECT id FROM jiao_se WHERE jiao_se_dai_ma='ADMIN')", user);
        return get(teacherId);
    }

    private RenKeXiangYing changeAssignmentStatusInternal(Long assignmentId, RenKeZhuangTaiQingQiu request) {
        RenKeXiangYing existing = assignment(assignmentId);
        if ("ACTIVE".equals(request.status()) && !"ACTIVE".equals(existing.status())) fail("TEACHING_ASSIGNMENT_REACTIVATION_NOT_SUPPORTED", "为保留结束历史，首版不重新启用已结束或停用的三元关系", HttpStatus.CONFLICT);
        LocalDateTime endTime = "ENDED".equals(request.status()) ? LocalDateTime.now() : existing.endTime();
        jdbcTemplate.update("UPDATE ren_ke_guan_xi SET zhuang_tai=?,jie_shu_shi_jian=? WHERE id=?", request.status(), endTime, assignmentId);
        return assignment(assignmentId);
    }

    private String filters(List<Object> args, String employeeNumber, String name, String username, String accountStatus, String profileStatus) {
        StringBuilder where = new StringBuilder(" WHERE p.yi_shan_chu=0 AND u.yi_shan_chu=0");
        like(where,args,"p.gong_hao",employeeNumber); like(where,args,"p.xing_ming",name); like(where,args,"u.yong_hu_ming",username);
        equal(where,args,"u.zhang_hao_zhuang_tai",accountStatus); equal(where,args,"p.zhuang_tai",profileStatus); return where.toString();
    }
    private void like(StringBuilder where,List<Object> args,String field,String value){if(value!=null&&!value.isBlank()){where.append(" AND ").append(field).append(" LIKE ?");args.add("%"+trim(value)+"%");}}
    private void equal(StringBuilder where,List<Object> args,String field,String value){if(value!=null&&!value.isBlank()){where.append(" AND ").append(field).append("=?");args.add(trim(value));}}
    private List<RenKeXiangYing> assignmentsInternal(Long teacherId) { return jdbcTemplate.query("""
            SELECT a.id,b.id,b.ban_ji_bian_ma,b.ban_ji_ming_cheng,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,
                   a.shi_fou_zhu_ren_ke,a.zhuang_tai,a.kai_shi_shi_jian,a.jie_shu_shi_jian
            FROM ren_ke_guan_xi a JOIN ban_ji b ON b.id=a.ban_ji_id JOIN ke_mu s ON s.id=a.ke_mu_id
            WHERE a.jiao_shi_id=? ORDER BY a.kai_shi_shi_jian DESC,a.id DESC""", assignmentMapper(), teacherId); }
    private RenKeXiangYing assignment(Long id) { List<RenKeXiangYing> list=jdbcTemplate.query("""
            SELECT a.id,b.id,b.ban_ji_bian_ma,b.ban_ji_ming_cheng,s.id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,
                   a.shi_fou_zhu_ren_ke,a.zhuang_tai,a.kai_shi_shi_jian,a.jie_shu_shi_jian
            FROM ren_ke_guan_xi a JOIN ban_ji b ON b.id=a.ban_ji_id JOIN ke_mu s ON s.id=a.ke_mu_id WHERE a.id=?""", assignmentMapper(), id);
        if(list.isEmpty()) fail("TEACHING_ASSIGNMENT_NOT_FOUND","任课关系不存在",HttpStatus.NOT_FOUND); return list.getFirst(); }
    private JiaoShiXiangYing findTeacher(Long id) { List<JiaoShiXiangYing> list=jdbcTemplate.query("""
            SELECT p.id,p.gong_hao,p.xing_ming,p.xian_shi_zhi_wu,u.yong_hu_ming,u.zhang_hao_zhuang_tai,p.zhuang_tai
            FROM jiao_shi_dang_an p JOIN yong_hu u ON u.id=p.yong_hu_id WHERE p.id=? AND p.yi_shan_chu=0 AND u.yi_shan_chu=0""", teacherMapper(),id);
        if(list.isEmpty()) fail("TEACHER_NOT_FOUND","教师不存在",HttpStatus.NOT_FOUND); return list.getFirst(); }
    private RowMapper<JiaoShiXiangYing> teacherMapper(){return (rs,row)->new JiaoShiXiangYing(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),roles(rs.getLong(1)));}
    private List<String> roles(long teacherId){return jdbcTemplate.queryForList("SELECT r.jiao_se_dai_ma FROM yong_hu_jiao_se ur JOIN jiao_se r ON r.id=ur.jiao_se_id JOIN jiao_shi_dang_an p ON p.yong_hu_id=ur.yong_hu_id WHERE p.id=? AND ur.zhuang_tai='ACTIVE' AND r.zhuang_tai='ACTIVE' AND r.yi_shan_chu=0 ORDER BY r.id",String.class,teacherId);}
    private RowMapper<RenKeXiangYing> assignmentMapper(){return (rs,row)->new RenKeXiangYing(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getLong(5),rs.getString(6),rs.getString(7),rs.getBoolean(8),rs.getString(9),rs.getObject(10,LocalDateTime.class),rs.getObject(11,LocalDateTime.class));}
    private boolean exists(String sql,Object... args){Long count=jdbcTemplate.queryForObject(sql,args,Long.class);return count!=null&&count>0;}
    private void validatePassword(String password){if(password.isBlank()||!password.matches(".*[A-Za-z].*")||!password.matches(".*[0-9].*"))fail("PASSWORD_POLICY_VIOLATION","初始密码必须为8至64位并同时包含字母和数字",HttpStatus.BAD_REQUEST);}
    private String trim(String value){return value==null?null:value.trim();} private String emptyToNull(String value){String trimmed=trim(value);return trimmed==null||trimmed.isEmpty()?null:trimmed;}
    private void fail(String code,String message,HttpStatus status){throw new RenZhengYeWuYiChang(code,message,status);}
}
