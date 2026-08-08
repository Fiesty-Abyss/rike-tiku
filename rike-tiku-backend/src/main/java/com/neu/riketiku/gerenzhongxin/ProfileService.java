package com.neu.riketiku.gerenzhongxin;

import com.neu.riketiku.gerenzhongxin.ProfileDtos.AccountResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.AvatarResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.PersonalResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.ProfileResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.StudentProfileResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.TeacherProfileResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.TeachingScopeResponse;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {
    private static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/png", "image/jpeg");

    private final JdbcTemplate jdbc;

    public ProfileService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(long userId) {
        UserRow user = requireUser(userId);
        List<String> roles = jdbc.queryForList("""
                SELECT r.jiao_se_dai_ma
                FROM yong_hu_jiao_se ur
                JOIN jiao_se r ON r.id=ur.jiao_se_id
                WHERE ur.yong_hu_id=? AND ur.zhuang_tai='ACTIVE'
                  AND r.zhuang_tai='ACTIVE' AND r.yi_shan_chu=0
                ORDER BY r.jiao_se_dai_ma
                """, String.class, userId);
        StudentProfileResponse student = studentProfile(userId);
        TeacherProfileResponse teacher = teacherProfile(userId);
        String displayName = teacher != null ? teacher.name()
                : student != null ? student.name() : user.username();
        return new ProfileResponse(
                displayName,
                new AccountResponse(user.username(), user.accountStatus(), roles, user.firstLogin(),
                        user.passwordChangedAt(), user.lastLoginAt()),
                student,
                teacher,
                personal(user));
    }

    @Transactional
    public ProfileResponse updateIntroduction(long userId, String introduction) {
        String normalized = introduction == null || introduction.trim().isEmpty() ? null : introduction.trim();
        int updated = jdbc.update("""
                UPDATE yong_hu SET ge_ren_jian_jie=?,geng_xin_shi_jian=CURRENT_TIMESTAMP(3)
                WHERE id=? AND yi_shan_chu=0
                """, normalized, userId);
        if (updated == 0) notFound();
        return getProfile(userId);
    }

    @Transactional
    public AvatarResponse uploadAvatar(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            fail("AVATAR_EMPTY", "请选择需要上传的头像图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            fail("AVATAR_TOO_LARGE", "头像文件不能超过2MB");
        }
        String declaredMime = normalizeMime(file.getContentType());
        if (!ALLOWED_MIME_TYPES.contains(declaredMime)) {
            fail("AVATAR_TYPE_INVALID", "头像仅支持PNG或JPEG图片");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception exception) {
            throw new RenZhengYeWuYiChang("AVATAR_READ_FAILED", "头像文件读取失败", HttpStatus.BAD_REQUEST);
        }
        String detectedMime = detectImageMime(bytes);
        if (detectedMime == null || !detectedMime.equals(declaredMime)) {
            fail("AVATAR_CONTENT_INVALID", "头像文件不是有效的PNG或JPEG图片");
        }
        LocalDateTime updatedAt = LocalDateTime.now();
        int updated = jdbc.update("""
                UPDATE yong_hu
                SET tou_xiang_mime=?,tou_xiang=?,tou_xiang_geng_xin_shi_jian=?,geng_xin_shi_jian=?
                WHERE id=? AND yi_shan_chu=0
                """, detectedMime, bytes, updatedAt, updatedAt, userId);
        if (updated == 0) notFound();
        return avatar(bytes, detectedMime, updatedAt);
    }

    @Transactional
    public AvatarResponse deleteAvatar(long userId) {
        int updated = jdbc.update("""
                UPDATE yong_hu
                SET tou_xiang_mime=NULL,tou_xiang=NULL,tou_xiang_geng_xin_shi_jian=NULL,
                    geng_xin_shi_jian=CURRENT_TIMESTAMP(3)
                WHERE id=? AND yi_shan_chu=0
                """, userId);
        if (updated == 0) notFound();
        return new AvatarResponse(null, null, null);
    }

    private UserRow requireUser(long userId) {
        UserRow user = jdbc.query("""
                SELECT yong_hu_ming,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu,
                       mi_ma_xiu_gai_shi_jian,zui_hou_deng_lu_shi_jian,ge_ren_jian_jie,
                       tou_xiang_mime,tou_xiang,tou_xiang_geng_xin_shi_jian
                FROM yong_hu WHERE id=? AND yi_shan_chu=0
                """, rs -> rs.next() ? new UserRow(
                rs.getString(1), rs.getString(2), rs.getBoolean(3),
                rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class),
                rs.getString(6), rs.getString(7), rs.getBytes(8),
                rs.getObject(9, LocalDateTime.class)) : null, userId);
        if (user == null) notFound();
        return user;
    }

    private StudentProfileResponse studentProfile(long userId) {
        return jdbc.query("""
                SELECT p.xue_hao,p.xing_ming,p.nian_ji,b.ban_ji_ming_cheng
                FROM xue_sheng_dang_an p
                LEFT JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=p.id
                  AND bx.zhuang_tai='ACTIVE' AND bx.shi_fou_zhu_ban_ji=1
                LEFT JOIN ban_ji b ON b.id=bx.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                WHERE p.yong_hu_id=? AND p.zhuang_tai='ACTIVE' AND p.yi_shan_chu=0
                """, rs -> rs.next() ? new StudentProfileResponse(
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null, userId);
    }

    private TeacherProfileResponse teacherProfile(long userId) {
        TeacherSeed teacher = jdbc.query("""
                SELECT id,gong_hao,xing_ming,xian_shi_zhi_wu
                FROM jiao_shi_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, rs -> rs.next() ? new TeacherSeed(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)) : null, userId);
        if (teacher == null) return null;
        List<TeachingScopeResponse> scopes = jdbc.query("""
                SELECT r.id,b.ban_ji_ming_cheng,b.nian_ji,s.ke_mu_ming_cheng
                FROM ren_ke_guan_xi r
                JOIN ban_ji b ON b.id=r.ban_ji_id AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                JOIN ke_mu s ON s.id=r.ke_mu_id AND s.zhuang_tai='ACTIVE' AND s.yi_shan_chu=0
                WHERE r.jiao_shi_id=? AND r.zhuang_tai='ACTIVE'
                ORDER BY b.ban_ji_ming_cheng,s.id,r.id
                """, (rs, row) -> new TeachingScopeResponse(
                rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)), teacher.id());
        return new TeacherProfileResponse(teacher.number(), teacher.name(), teacher.title(), scopes);
    }

    private PersonalResponse personal(UserRow user) {
        AvatarResponse avatar = avatar(user.avatar(), user.avatarMime(), user.avatarUpdatedAt());
        return new PersonalResponse(user.introduction(), avatar.avatarDataUrl(), avatar.avatarMime(),
                avatar.avatarUpdatedAt());
    }

    private AvatarResponse avatar(byte[] bytes, String mime, LocalDateTime updatedAt) {
        String dataUrl = bytes == null || mime == null ? null
                : "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        return new AvatarResponse(dataUrl, mime, updatedAt);
    }

    private String detectImageMime(byte[] bytes) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) return null;
            if (bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                    && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                    && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
            if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String normalizeMime(String mime) {
        return mime == null ? "" : mime.trim().toLowerCase();
    }

    private void notFound() {
        throw new RenZhengYeWuYiChang("PROFILE_NOT_FOUND", "当前用户不存在", HttpStatus.NOT_FOUND);
    }

    private void fail(String code, String message) {
        throw new RenZhengYeWuYiChang(code, message, HttpStatus.BAD_REQUEST);
    }

    private record UserRow(String username, String accountStatus, boolean firstLogin,
                           LocalDateTime passwordChangedAt, LocalDateTime lastLoginAt,
                           String introduction, String avatarMime, byte[] avatar,
                           LocalDateTime avatarUpdatedAt) {
    }

    private record TeacherSeed(long id, String number, String name, String title) {
    }
}
