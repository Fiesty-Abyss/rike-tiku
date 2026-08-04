package com.neu.riketiku.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class ShenJiZiDuanTianChongChuLiQi implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "chuangJianShiJian", LocalDateTime.class, now);
        strictInsertFill(metaObject, "gengXinShiJian", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "gengXinShiJian", LocalDateTime.class, LocalDateTime.now());
    }
}
