package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class Mvp30WorkbookReadIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionImportService service;

    @Test
    void readsAllThreeUnmodifiedMvp30WorkbooksWithoutWritingBusinessRows() throws Exception {
        for (Map.Entry<String, String> entry : Map.of("物理", "PHYSICS", "化学", "CHEMISTRY", "生物", "BIOLOGY").entrySet()) {
            Path path = Path.of("..", "题库", "理综", "测试结果", entry.getKey(), "待审核_清洗版.xlsx");
            byte[] content = Files.readAllBytes(path);
            var preview = service.preview(new MockMultipartFile("file", path.getFileName().toString(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content));
            assertThat(preview.subjectCode()).isEqualTo(entry.getValue());
            assertThat(preview.totalCount()).isEqualTo(10);
            assertThat(preview.fileHash()).hasSize(64);
        }
    }
}
