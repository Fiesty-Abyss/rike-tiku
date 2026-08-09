package com.neu.riketiku.jiaoxue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.jiaoxue.dto.BanJiChuangJianQingQiu;
import com.neu.riketiku.jiaoxue.dto.BanJiXiangYing;
import com.neu.riketiku.jiaoxue.dto.BanJiXiuGaiQingQiu;
import com.neu.riketiku.jiaoxue.dto.BanJiZhuangTaiQingQiu;
import com.neu.riketiku.jiaoxue.entity.BanJi;
import com.neu.riketiku.jiaoxue.mapper.BanJiMapper;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BanJiFuWu {
    private final BanJiMapper mapper;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;

    public BanJiFuWu(BanJiMapper mapper, GuanLiCaoZuoRiZhiFuWu auditLog) {
        this.mapper = mapper;
        this.auditLog = auditLog;
    }

    public BanJiXiangYing create(BanJiChuangJianQingQiu request) {
        return auditLog.audited("CLASS", "CREATE", null, "管理员创建班级", () -> createInternal(request), BanJiXiangYing::id);
    }

    private BanJiXiangYing createInternal(BanJiChuangJianQingQiu request) {
        String classCode = trim(request.classCode());
        if (mapper.selectCount(new LambdaQueryWrapper<BanJi>()
                .eq(BanJi::getBanJiBianMa, classCode)) > 0) {
            throw new RenZhengYeWuYiChang("CLASS_CODE_EXISTS", "班级编码已存在", HttpStatus.CONFLICT);
        }

        BanJi banJi = new BanJi();
        banJi.setBanJiBianMa(classCode);
        banJi.setBanJiMingCheng(trim(request.className()));
        banJi.setNianJi(trim(request.grade()));
        banJi.setRuXueNianFen(request.enrollmentYear());
        banJi.setZhuangTai("ACTIVE");
        mapper.insert(banJi);
        return BanJiXiangYing.from(banJi);
    }

    public BanJiXiangYing get(Long id) {
        return BanJiXiangYing.from(findExisting(id));
    }

    public BanJiXiangYing update(Long id, BanJiXiuGaiQingQiu request) {
        return auditLog.audited("CLASS", "UPDATE", id, "管理员修改班级档案", () -> updateInternal(id, request));
    }

    private BanJiXiangYing updateInternal(Long id, BanJiXiuGaiQingQiu request) {
        BanJi banJi = findExisting(id);
        banJi.setBanJiMingCheng(trim(request.className()));
        banJi.setNianJi(trim(request.grade()));
        banJi.setRuXueNianFen(request.enrollmentYear());
        mapper.updateById(banJi);
        return BanJiXiangYing.from(banJi);
    }

    public BanJiXiangYing changeStatus(Long id, BanJiZhuangTaiQingQiu request) {
        return auditLog.audited("CLASS", "STATUS_CHANGE", id, "管理员变更班级状态", () -> changeStatusInternal(id, request));
    }

    private BanJiXiangYing changeStatusInternal(Long id, BanJiZhuangTaiQingQiu request) {
        String status = trim(request.status());
        if (!List.of("ACTIVE", "GRADUATED", "DISABLED").contains(status)) {
            throw new RenZhengYeWuYiChang("INVALID_CLASS_STATUS", "班级状态不正确", HttpStatus.BAD_REQUEST);
        }

        BanJi banJi = findExisting(id);
        banJi.setZhuangTai(status);
        mapper.updateById(banJi);
        return BanJiXiangYing.from(banJi);
    }

    public Page<BanJi> page(long page, long size, String code, String name, String grade, String status) {
        LambdaQueryWrapper<BanJi> query = new LambdaQueryWrapper<>();
        query.like(hasText(code), BanJi::getBanJiBianMa, trim(code))
                .like(hasText(name), BanJi::getBanJiMingCheng, trim(name))
                .eq(hasText(grade), BanJi::getNianJi, trim(grade))
                .eq(hasText(status), BanJi::getZhuangTai, trim(status))
                .orderByDesc(BanJi::getId);
        return mapper.selectPage(new Page<>(page, size), query);
    }

    private BanJi findExisting(Long id) {
        BanJi banJi = mapper.selectById(id);
        if (banJi == null) {
            throw new RenZhengYeWuYiChang("CLASS_NOT_FOUND", "班级不存在", HttpStatus.NOT_FOUND);
        }
        return banJi;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
