package com.neu.riketiku.renzheng;

import com.neu.riketiku.renzheng.dto.ChuShiMiMaXiuGaiQingQiu;
import com.neu.riketiku.renzheng.dto.DangQianYongHuXiangYing;
import com.neu.riketiku.renzheng.dto.DengLuQingQiu;
import com.neu.riketiku.renzheng.dto.DengLuXiangYing;
import com.neu.riketiku.renzheng.dto.YongHuZhaiYaoXiangYing;
import com.neu.riketiku.renzheng.dto.TuXingYanZhengMaTiaoZhanXiangYing;
import com.neu.riketiku.renzheng.dto.ZhuDongMiMaXiuGaiQingQiu;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RenZhengFuWu {
    private final RenZhengShuJuCangKu repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtLingPaiFuWu jwtService;
    private final TuXingYanZhengMaFuWu captchaService;

    public RenZhengFuWu(
            RenZhengShuJuCangKu repository,
            PasswordEncoder passwordEncoder,
            JwtLingPaiFuWu jwtService,
            TuXingYanZhengMaFuWu captchaService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.captchaService = captchaService;
    }

    public TuXingYanZhengMaTiaoZhanXiangYing tuXingYanZhengMaTiaoZhan(String previousChallengeId) {
        return captchaService.create(previousChallengeId);
    }

    @Transactional
    public DengLuXiangYing dengLu(DengLuQingQiu request) {
        captchaService.verify(request.challengeId(), request.captchaCode());
        YongHuRenZhengShuJu user = repository.anYongHuMingChaZhao(request.username().trim())
                .orElseThrow(this::invalidCredentials);
        validateAccountStatus(user);
        if (!passwordEncoder.matches(request.password(), user.miMaZhaiYao())) {
            throw invalidCredentials();
        }

        List<String> roles = repository.chaZhaoYouXiaoJiaoSe(user.id());
        if (roles.isEmpty()) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_HAS_NO_ROLE", "账号没有有效角色", HttpStatus.FORBIDDEN);
        }
        if (request.expectedRole() != null && !roles.contains(request.expectedRole().name())) {
            throw new RenZhengYeWuYiChang(
                    "ROLE_MISMATCH", "账号与当前登录入口不匹配", HttpStatus.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (repository.gengXinZuiHouDengLuShiJian(user.id(), now) != 1) {
            throw new RenZhengYeWuYiChang(
                    "LOGIN_STATE_UPDATE_FAILED", "登录状态更新失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return createLoginResponse(user, roles, user.shiFouShouCiDengLu());
    }

    @Transactional(readOnly = true)
    public DangQianYongHuXiangYing dangQianYongHu(RenZhengYongHu principal) {
        YongHuRenZhengShuJu user = repository.anIdChaZhao(principal.id())
                .orElseThrow(() -> new RenZhengYeWuYiChang(
                        "ACCOUNT_UNAVAILABLE", "当前账号不可用", HttpStatus.UNAUTHORIZED));
        validateAccountStatus(user);
        List<String> roles = repository.chaZhaoYouXiaoJiaoSe(user.id());
        if (roles.isEmpty()) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_HAS_NO_ROLE", "账号没有有效角色", HttpStatus.FORBIDDEN);
        }
        DangAnXianShiShuJu profile = repository.chaZhaoDangAn(user.id());
        return new DangQianYongHuXiangYing(
                user.id(), user.yongHuMing(), roles, user.shiFouShouCiDengLu(),
                profile.xingMing(), profile.xueHao(), profile.gongHao());
    }

    @Transactional
    public DengLuXiangYing xiuGaiChuShiMiMa(
            RenZhengYongHu principal,
            ChuShiMiMaXiuGaiQingQiu request) {
        YongHuRenZhengShuJu user = repository.anIdChaZhao(principal.id())
                .orElseThrow(() -> new RenZhengYeWuYiChang(
                        "ACCOUNT_UNAVAILABLE", "当前账号不可用", HttpStatus.UNAUTHORIZED));
        validateAccountStatus(user);
        if (!user.shiFouShouCiDengLu()) {
            throw new RenZhengYeWuYiChang(
                    "INITIAL_PASSWORD_ALREADY_CHANGED", "初始密码已经修改", HttpStatus.CONFLICT);
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.miMaZhaiYao())) {
            throw new RenZhengYeWuYiChang(
                    "OLD_PASSWORD_INCORRECT", "旧密码不正确", HttpStatus.BAD_REQUEST);
        }
        validateNewPassword(request, user.miMaZhaiYao());

        LocalDateTime now = LocalDateTime.now();
        String passwordHash = passwordEncoder.encode(request.newPassword());
        if (repository.gengXinChuShiMiMa(user.id(), passwordHash, now) != 1) {
            throw new RenZhengYeWuYiChang(
                    "PASSWORD_UPDATE_FAILED", "密码修改失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<String> roles = repository.chaZhaoYouXiaoJiaoSe(user.id());
        if (roles.isEmpty()) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_HAS_NO_ROLE", "账号没有有效角色", HttpStatus.FORBIDDEN);
        }
        return createLoginResponse(user, roles, false);
    }

    @Transactional
    public DengLuXiangYing zhuDongXiuGaiMiMa(
            RenZhengYongHu principal, ZhuDongMiMaXiuGaiQingQiu request) {
        YongHuRenZhengShuJu user = repository.anIdChaZhao(principal.id())
                .orElseThrow(() -> new RenZhengYeWuYiChang(
                        "ACCOUNT_UNAVAILABLE", "当前账号不可用", HttpStatus.UNAUTHORIZED));
        validateAccountStatus(user);
        if (!passwordEncoder.matches(request.oldPassword(), user.miMaZhaiYao())) {
            throw new RenZhengYeWuYiChang(
                    "OLD_PASSWORD_INCORRECT", "旧密码不正确", HttpStatus.BAD_REQUEST);
        }
        validateNewPassword(request.newPassword(), request.confirmPassword(), user.miMaZhaiYao());
        LocalDateTime now = LocalDateTime.now();
        if (repository.gengXinChuShiMiMa(user.id(), passwordEncoder.encode(request.newPassword()), now) != 1) {
            throw new RenZhengYeWuYiChang(
                    "PASSWORD_UPDATE_FAILED", "密码修改失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List<String> roles = repository.chaZhaoYouXiaoJiaoSe(user.id());
        return createLoginResponse(user, roles, false);
    }

    private void validateAccountStatus(YongHuRenZhengShuJu user) {
        if ("DISABLED".equals(user.zhangHaoZhuangTai())) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_DISABLED", "账号已停用", HttpStatus.FORBIDDEN);
        }
        if ("LOCKED".equals(user.zhangHaoZhuangTai())) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_LOCKED", "账号已锁定", HttpStatus.FORBIDDEN);
        }
        if (!"ENABLED".equals(user.zhangHaoZhuangTai())) {
            throw new RenZhengYeWuYiChang(
                    "ACCOUNT_UNAVAILABLE", "账号不可用", HttpStatus.FORBIDDEN);
        }
    }

    private void validateNewPassword(ChuShiMiMaXiuGaiQingQiu request, String currentHash) {
        validateNewPassword(request.newPassword(), request.confirmPassword(), currentHash);
    }

    private void validateNewPassword(String newPassword, String confirmPassword, String currentHash) {
        if (!newPassword.equals(confirmPassword)) {
            throw new RenZhengYeWuYiChang(
                    "PASSWORD_CONFIRMATION_MISMATCH", "两次输入的新密码不一致", HttpStatus.BAD_REQUEST);
        }
        if (newPassword.length() < 8 || newPassword.length() > 64
                || newPassword.isBlank()
                || !newPassword.matches(".*[A-Za-z].*")
                || !newPassword.matches(".*[0-9].*")) {
            throw new RenZhengYeWuYiChang(
                    "PASSWORD_POLICY_VIOLATION", "新密码必须为8至64位并同时包含字母和数字",
                    HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(newPassword, currentHash)) {
            throw new RenZhengYeWuYiChang(
                    "PASSWORD_UNCHANGED", "新密码不能与旧密码相同", HttpStatus.BAD_REQUEST);
        }
    }

    private DengLuXiangYing createLoginResponse(
            YongHuRenZhengShuJu user,
            List<String> roles,
            boolean mustChangePassword) {
        RenZhengYongHu principal = new RenZhengYongHu(
                user.id(), user.yongHuMing(), List.copyOf(roles), mustChangePassword);
        return new DengLuXiangYing(
                jwtService.shengChengLingPai(principal),
                "Bearer",
                jwtService.getExpirationSeconds(),
                mustChangePassword,
                new YongHuZhaiYaoXiangYing(user.id(), user.yongHuMing(), List.copyOf(roles)));
    }

    private RenZhengYeWuYiChang invalidCredentials() {
        return new RenZhengYeWuYiChang(
                "INVALID_CREDENTIALS", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
    }
}
