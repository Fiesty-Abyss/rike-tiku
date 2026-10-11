package com.neu.riketiku.tiku.daoru;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 本地真题源数据集（MVP30 候选工作簿及其图片）不随仓库分发。
 * 把整理好的数据放到 question-bank/理综/测试结果/{物理,化学,生物}/待审核_清洗版.xlsx 后，相关导入测试会自动启用。
 */
final class LocalSourceBank {
    private LocalSourceBank() {
    }

    static boolean present() {
        Path root = Path.of("..", "question-bank", "理综", "测试结果").toAbsolutePath().normalize();
        return List.of("物理", "化学", "生物").stream()
                .allMatch(subject -> Files.isRegularFile(root.resolve(subject).resolve("待审核_清洗版.xlsx")));
    }
}
