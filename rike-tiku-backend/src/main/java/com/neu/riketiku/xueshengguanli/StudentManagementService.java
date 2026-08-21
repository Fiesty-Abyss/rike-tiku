package com.neu.riketiku.xueshengguanli;

import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.ClassHistoryResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.ClassSummaryResponse;
import com.neu.riketiku.zhanghao.AdminDefaultPasswordPolicy;
import com.neu.riketiku.zhanghao.dto.AdminPasswordRecoveryDtos.PasswordRecoveryResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentCreateRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentCreateResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentDetailResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentListResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentSummaryResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentTransferRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentUpdateRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentManagementService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;
    private final AdminDefaultPasswordPolicy defaultPasswordPolicy;

    public StudentManagementService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            GuanLiCaoZuoRiZhiFuWu auditLog,
            AdminDefaultPasswordPolicy defaultPasswordPolicy) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
        this.defaultPasswordPolicy = defaultPasswordPolicy;
    }

    @Transactional(readOnly = true)
    public StudentListResponse page(long page, long size, String studentNumber, String name, String username,
            Long classId, String grade, String accountStatus, String profileStatus) {
        List<Object> args = new ArrayList<>();
        String where = filters(args, studentNumber, name, username, classId, grade, accountStatus, profileStatus);
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + baseFrom() + where, Long.class, args.toArray());
        args.add(size);
        args.add((page - 1) * size);
        List<StudentSummaryResponse> records = jdbc.query("SELECT " + summaryColumns() + baseFrom() + where
                + " ORDER BY p.id DESC LIMIT ? OFFSET ?", summaryMapper(), args.toArray());
        return new StudentListResponse(records, total, page, size, (total + size - 1) / size);
    }

    @Transactional(readOnly = true)
    public StudentDetailResponse get(Long studentId) {
        StudentSummaryResponse student = findStudent(studentId);
        List<String> roles = jdbc.queryForList("""
                SELECT r.jiao_se_dai_ma FROM xue_sheng_dang_an p
                JOIN yong_hu_jiao_se ur ON ur.yong_hu_id=p.yong_hu_id AND ur.zhuang_tai='ACTIVE'
                JOIN jiao_se r ON r.id=ur.jiao_se_id AND r.zhuang_tai='ACTIVE' AND r.yi_shan_chu=0
                WHERE p.id=? ORDER BY r.id
                """, String.class, studentId);
        List<ClassHistoryResponse> history = jdbc.query("""
                SELECT b.id,b.ban_ji_bian_ma,b.ban_ji_ming_cheng,bx.jia_ru_shi_jian,bx.tui_chu_shi_jian,
                       (bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL) AS is_current
                FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id
                WHERE bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1
                ORDER BY bx.jia_ru_shi_jian DESC,bx.id DESC
                """, (rs, row) -> new ClassHistoryResponse(
                        rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class), rs.getBoolean(6)),
                studentId);
        return new StudentDetailResponse(student, roles, history);
    }

    @Transactional
    public StudentCreateResponse create(StudentCreateRequest request) {
        return auditLog.audited("STUDENT", "CREATE", null, "管理员新增学生账号和档案", () -> createInternal(request), result -> result.student().student().id());
    }

    private StudentCreateResponse createInternal(StudentCreateRequest request) {
        String studentNumber = trim(request.studentNumber());
        String username = trim(request.username());
        if (exists("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao=?", studentNumber)) {
            fail("STUDENT_NUMBER_EXISTS", "学号已存在", HttpStatus.CONFLICT);
        }
        if (exists("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming=?", username)) {
            fail("USERNAME_EXISTS", "用户名已存在", HttpStatus.CONFLICT);
        }
        Long roleId = jdbc.query("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='STUDENT' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (roleId == null) fail("STUDENT_ROLE_UNAVAILABLE", "STUDENT角色不存在或已停用", HttpStatus.CONFLICT);
        String password = defaultPasswordPolicy.password();
        try {
            jdbc.update("""
                    INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu)
                    VALUES (?,?,'ENABLED',1)
                    """, username, passwordEncoder.encode(password));
            Long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id,zhuang_tai) VALUES (?,?,'ACTIVE')", userId, roleId);
            jdbc.update("""
                    INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji,zhuang_tai)
                    VALUES (?,?,?,?,'ACTIVE')
                    """, userId, studentNumber, trim(request.name()), trim(request.grade()));
            Long studentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            requireActiveClass(request.classId());
            jdbc.update("""
                    INSERT INTO ban_ji_xue_sheng(ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai)
                    VALUES (?,?,1,'ACTIVE')
                    """, request.classId(), studentId);
            return new StudentCreateResponse(get(studentId), password);
        } catch (DataIntegrityViolationException exception) {
            fail("STUDENT_CREATE_CONFLICT", "学生信息与当前数据库状态冲突", HttpStatus.CONFLICT);
            throw exception;
        }
    }

    @Transactional
    public StudentDetailResponse update(Long studentId, StudentUpdateRequest request) {
        return auditLog.audited("STUDENT", "UPDATE", studentId, "管理员修改学生档案或账号状态", () -> updateInternal(studentId, request));
    }

    private StudentDetailResponse updateInternal(Long studentId, StudentUpdateRequest request) {
        StudentSummaryResponse student = findStudent(studentId);
        jdbc.update("UPDATE yong_hu SET zhang_hao_zhuang_tai=? WHERE yong_hu_ming=?",
                request.accountStatus(), student.username());
        jdbc.update("UPDATE xue_sheng_dang_an SET xing_ming=?,nian_ji=?,zhuang_tai=? WHERE id=?",
                trim(request.name()), trim(request.grade()), request.profileStatus(), studentId);
        return get(studentId);
    }

    @Transactional
    public StudentDetailResponse transfer(Long studentId, StudentTransferRequest request) {
        return auditLog.audited("STUDENT", "TRANSFER", studentId, "管理员变更学生主班级", () -> transferInternal(studentId, request));
    }

    private StudentDetailResponse transferInternal(Long studentId, StudentTransferRequest request) {
        findStudent(studentId);
        requireActiveClass(request.classId());
        List<Long> currentRelations = jdbc.queryForList("""
                SELECT id FROM ban_ji_xue_sheng
                WHERE xue_sheng_id=? AND shi_fou_zhu_ban_ji=1 AND zhuang_tai='ACTIVE' AND tui_chu_shi_jian IS NULL
                """, Long.class, studentId);
        if (currentRelations.size() != 1) {
            fail("ACTIVE_MAIN_CLASS_INVALID", "学生必须且只能有一个当前主班级", HttpStatus.CONFLICT);
        }
        Long currentClassId = jdbc.queryForObject(
                "SELECT ban_ji_id FROM ban_ji_xue_sheng WHERE id=?", Long.class, currentRelations.getFirst());
        if (request.classId().equals(currentClassId)) {
            fail("STUDENT_ALREADY_IN_CLASS", "学生已经在目标班级", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE ban_ji_xue_sheng SET zhuang_tai='EXITED',tui_chu_shi_jian=? WHERE id=?",
                now, currentRelations.getFirst());
        try {
            jdbc.update("""
                    INSERT INTO ban_ji_xue_sheng(ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,jia_ru_shi_jian,zhuang_tai)
                    VALUES (?,?,1,?,'ACTIVE')
                    """, request.classId(), studentId, now);
        } catch (DataIntegrityViolationException exception) {
            fail("ACTIVE_MAIN_CLASS_CONFLICT", "学生当前主班级关系冲突", HttpStatus.CONFLICT);
        }
        return get(studentId);
    }

    @Transactional
    public PasswordRecoveryResponse resetPassword(Long studentId) {
        return auditLog.audited("STUDENT", "RESET_PASSWORD", studentId,
                "管理员恢复学生默认密码（不记录密码）", () -> resetPasswordInternal(studentId));
    }

    private PasswordRecoveryResponse resetPasswordInternal(Long studentId) {
        StudentSummaryResponse student = findStudent(studentId);
        String password = defaultPasswordPolicy.password();
        jdbc.update("""
                UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=1,mi_ma_xiu_gai_shi_jian=NULL
                WHERE yong_hu_ming=?
                """, passwordEncoder.encode(password), student.username());
        return new PasswordRecoveryResponse(1, password, true);
    }

    @Transactional
    public PasswordRecoveryResponse resetPasswords(List<Long> requestedIds) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(requestedIds));
        String summary = "管理员批量恢复学生默认密码；数量=" + ids.size() + "；目标业务ID=" + ids;
        return auditLog.audited("STUDENT", "BATCH_RESET_PASSWORD", null, summary,
                () -> resetPasswordsInternal(ids));
    }

    private PasswordRecoveryResponse resetPasswordsInternal(List<Long> ids) {
        List<StudentSummaryResponse> students = ids.stream().map(this::findStudent).toList();
        String password = defaultPasswordPolicy.password();
        for (StudentSummaryResponse student : students) {
            jdbc.update("""
                    UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=1,mi_ma_xiu_gai_shi_jian=NULL
                    WHERE yong_hu_ming=?
                    """, passwordEncoder.encode(password), student.username());
        }
        return new PasswordRecoveryResponse(students.size(), password, true);
    }

    private StudentSummaryResponse findStudent(Long studentId) {
        List<StudentSummaryResponse> students = jdbc.query("SELECT " + summaryColumns() + baseFrom()
                + " WHERE p.id=? AND p.yi_shan_chu=0 AND u.yi_shan_chu=0", summaryMapper(), studentId);
        if (students.isEmpty()) fail("STUDENT_NOT_FOUND", "学生不存在", HttpStatus.NOT_FOUND);
        return students.getFirst();
    }

    private void requireActiveClass(Long classId) {
        if (!exists("SELECT COUNT(*) FROM ban_ji WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", classId)) {
            fail("CLASS_UNAVAILABLE", "班级不存在或已停用", HttpStatus.CONFLICT);
        }
    }

    private String filters(List<Object> args, String studentNumber, String name, String username,
            Long classId, String grade, String accountStatus, String profileStatus) {
        StringBuilder where = new StringBuilder(" WHERE p.yi_shan_chu=0 AND u.yi_shan_chu=0");
        like(where, args, "p.xue_hao", studentNumber);
        like(where, args, "p.xing_ming", name);
        like(where, args, "u.yong_hu_ming", username);
        if (classId != null) {
            where.append(" AND b.id=?");
            args.add(classId);
        }
        equal(where, args, "p.nian_ji", grade);
        equal(where, args, "u.zhang_hao_zhuang_tai", accountStatus);
        equal(where, args, "p.zhuang_tai", profileStatus);
        return where.toString();
    }

    private String baseFrom() {
        return """
                 FROM xue_sheng_dang_an p JOIN yong_hu u ON u.id=p.yong_hu_id
                 LEFT JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=p.id AND bx.shi_fou_zhu_ban_ji=1
                      AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                 LEFT JOIN ban_ji b ON b.id=bx.ban_ji_id
                """;
    }

    private String summaryColumns() {
        return "p.id,p.xue_hao,p.xing_ming,u.yong_hu_ming,p.nian_ji,b.id,b.ban_ji_bian_ma,b.ban_ji_ming_cheng,b.nian_ji,u.zhang_hao_zhuang_tai,p.zhuang_tai ";
    }

    private RowMapper<StudentSummaryResponse> summaryMapper() {
        return (rs, row) -> {
            Long classId = rs.getObject(6, Long.class);
            ClassSummaryResponse currentClass = classId == null ? null
                    : new ClassSummaryResponse(classId, rs.getString(7), rs.getString(8), rs.getString(9));
            return new StudentSummaryResponse(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), currentClass, rs.getString(10), rs.getString(11));
        };
    }

    private void like(StringBuilder where, List<Object> args, String field, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(field).append(" LIKE ?");
            args.add("%" + trim(value) + "%");
        }
    }

    private void equal(StringBuilder where, List<Object> args, String field, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(field).append("=?");
            args.add(trim(value));
        }
    }

    private boolean exists(String sql, Object... args) {
        Long count = jdbc.queryForObject(sql, Long.class, args);
        return count != null && count > 0;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void fail(String code, String message, HttpStatus status) {
        throw new RenZhengYeWuYiChang(code, message, status);
    }
}
