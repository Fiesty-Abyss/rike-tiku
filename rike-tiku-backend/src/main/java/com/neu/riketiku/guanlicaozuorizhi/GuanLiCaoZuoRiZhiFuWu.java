package com.neu.riketiku.guanlicaozuorizhi;

import com.neu.riketiku.guanlicaozuorizhi.dto.GuanLiCaoZuoRiZhiDtos;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuanLiCaoZuoRiZhiFuWu {
    private final JdbcTemplate jdbc;
    private final GuanLiCaoZuoRiZhiJiLuFuWu recordService;

    public GuanLiCaoZuoRiZhiFuWu(JdbcTemplate jdbc, GuanLiCaoZuoRiZhiJiLuFuWu recordService) {
        this.jdbc = jdbc;
        this.recordService = recordService;
    }

    public <T> T audited(String module, String action, Long objectId, String summary, Supplier<T> operation) {
        return audited(module, action, objectId, summary, operation, ignored -> objectId);
    }

    public <T> T audited(String module, String action, Long objectId, String summary,
            Supplier<T> operation, Function<T, Long> resultObjectId) {
        try {
            T result = operation.get();
            recordService.success(module, action, resultObjectId.apply(result), summary);
            return result;
        } catch (RuntimeException exception) {
            recordService.failure(module, action, objectId, errorCode(exception));
            throw exception;
        }
    }

    public void audited(String module, String action, Long objectId, String summary, Runnable operation) {
        audited(module, action, objectId, summary, () -> {
            operation.run();
            return null;
        });
    }

    @Transactional(readOnly = true)
    public GuanLiCaoZuoRiZhiDtos.Page page(long page, long size, String module, String action, String result) {
        List<Object> arguments = new ArrayList<>();
        String where = filters(arguments, module, action, result);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM guan_li_cao_zuo_ri_zhi l" + where,
                arguments.toArray(), Long.class);
        List<Object> queryArguments = new ArrayList<>(arguments);
        queryArguments.add(size);
        queryArguments.add((page - 1) * size);
        List<GuanLiCaoZuoRiZhiDtos.Item> records = jdbc.query("""
                SELECT l.id,l.cao_zuo_ren_yong_hu_id,u.yong_hu_ming,l.mo_kuai,l.cao_zuo_lei_xing,
                       l.ye_wu_dui_xiang_id,l.cao_zuo_jie_guo,l.zhai_yao,l.cuo_wu_dai_ma,l.chuang_jian_shi_jian
                FROM guan_li_cao_zuo_ri_zhi l
                LEFT JOIN yong_hu u ON u.id=l.cao_zuo_ren_yong_hu_id
                """ + where + " ORDER BY l.id DESC LIMIT ? OFFSET ?", this::mapItem, queryArguments.toArray());
        return new GuanLiCaoZuoRiZhiDtos.Page(records, total == null ? 0 : total, page, size,
                total == null ? 0 : (total + size - 1) / size);
    }

    private String filters(List<Object> arguments, String module, String action, String result) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        equal(where, arguments, "l.mo_kuai", module);
        equal(where, arguments, "l.cao_zuo_lei_xing", action);
        equal(where, arguments, "l.cao_zuo_jie_guo", result);
        return where.toString();
    }

    private void equal(StringBuilder where, List<Object> arguments, String column, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append("=?");
            arguments.add(value.trim());
        }
    }

    private GuanLiCaoZuoRiZhiDtos.Item mapItem(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new GuanLiCaoZuoRiZhiDtos.Item(rs.getLong(1), rs.getObject(2, Long.class), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getObject(6, Long.class), rs.getString(7),
                rs.getString(8), rs.getString(9), rs.getObject(10, LocalDateTime.class));
    }

    private String errorCode(RuntimeException exception) {
        return exception instanceof RenZhengYeWuYiChang business ? business.getCode() : "INTERNAL_ERROR";
    }

}
