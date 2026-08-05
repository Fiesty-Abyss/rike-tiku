package com.neu.riketiku.xueshengdaoru;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.riketiku.jiaoxue.entity.BanJi;
import com.neu.riketiku.jiaoxue.entity.BanJiXueSheng;
import com.neu.riketiku.jiaoxue.mapper.BanJiMapper;
import com.neu.riketiku.jiaoxue.mapper.BanJiXueShengMapper;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.xueshengdaoru.response.StudentImportAccountResponse;
import com.neu.riketiku.xueshengdaoru.response.StudentImportConfirmResponse;
import com.neu.riketiku.zhanghao.entity.YongHu;
import com.neu.riketiku.zhanghao.mapper.YongHuMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentImportConfirmService {
    private final StudentImportService previewService;
    private final StudentInitialPasswordGenerator passwordGenerator;
    private final YongHuMapper userMapper;
    private final BanJiMapper classMapper;
    private final BanJiXueShengMapper classStudentMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public StudentImportConfirmService(StudentImportService previewService, StudentInitialPasswordGenerator passwordGenerator,
            YongHuMapper userMapper, BanJiMapper classMapper, BanJiXueShengMapper classStudentMapper,
            JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.previewService = previewService; this.passwordGenerator = passwordGenerator; this.userMapper = userMapper;
        this.classMapper = classMapper; this.classStudentMapper = classStudentMapper; this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public StudentImportConfirmResponse confirm(MultipartFile file) {
        List<StudentImportService.ValidatedRow> rows = previewService.validateRows(file);
        if (rows.stream().anyMatch(row -> !"VALID".equals(row.response().status()))) {
            throw new RenZhengYeWuYiChang("IMPORT_VALIDATION_FAILED", "文件存在无效行，请先修正后再确认导入", HttpStatus.BAD_REQUEST);
        }
        Long roleId = jdbcTemplate.query("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='STUDENT' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                result -> result.next() ? result.getLong(1) : null);
        if (roleId == null) fail("STUDENT_ROLE_UNAVAILABLE", "STUDENT角色不存在或已停用");
        List<StudentImportAccountResponse> accounts = new ArrayList<>();
        try {
            for (StudentImportService.ValidatedRow validated : rows) {
                StudentImportService.ParsedRow row = validated.row();
                BanJi banJi = classMapper.selectOne(new LambdaQueryWrapper<BanJi>().eq(BanJi::getBanJiBianMa, row.classCode()));
                if (banJi == null) fail("IMPORT_CONFLICT", "确认时班级不存在");
                if (!"ACTIVE".equals(banJi.getZhuangTai())) fail("IMPORT_CONFLICT", "确认时班级不是ACTIVE状态");
                if (!banJi.getNianJi().equals(row.grade())) fail("IMPORT_CONFLICT", "确认时年级与班级不一致");
                String password = row.password().isEmpty() ? passwordGenerator.generate() : row.password();
                YongHu user = new YongHu();
                user.setYongHuMing(row.username());
                user.setMiMaZhaiYao(passwordEncoder.encode(password));
                user.setZhangHaoZhuangTai(row.accountStatus());
                user.setShiFouShouCiDengLu(true);
                userMapper.insert(user);
                jdbcTemplate.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id,zhuang_tai) VALUES (?,?, 'ACTIVE')", user.getId(), roleId);
                jdbcTemplate.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji,zhuang_tai) VALUES (?,?,?,?, 'ACTIVE')",
                        user.getId(), row.studentNumber(), row.name(), row.grade());
                Long studentId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                BanJiXueSheng relation = new BanJiXueSheng();
                relation.setBanJiId(banJi.getId()); relation.setXueShengId(studentId); relation.setShiFouZhuBanJi(true);
                relation.setJiaRuShiJian(LocalDateTime.now()); relation.setZhuangTai("ACTIVE");
                classStudentMapper.insert(relation);
                accounts.add(new StudentImportAccountResponse(row.studentNumber(), row.name(), row.classCode(), row.username(),
                        password, row.accountStatus(), true));
            }
        } catch (DataIntegrityViolationException exception) {
            fail("IMPORT_CONFLICT", "导入数据与当前数据库状态冲突，整批未导入");
        }
        return new StudentImportConfirmResponse(rows.size(), accounts.size(), List.copyOf(accounts));
    }
    private void fail(String code, String message) { throw new RenZhengYeWuYiChang(code, message, HttpStatus.CONFLICT); }
}
