package com.neu.riketiku.tiku.daoru;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/question-import")
public class QuestionImportController {
    private final QuestionImportService service;

    public QuestionImportController(QuestionImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionImportDtos.Preview preview(@RequestParam("file") MultipartFile file) {
        return service.preview(file);
    }

    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionImportDtos.Confirm confirm(@RequestParam("file") MultipartFile file,
                                               @RequestParam("previewFileHash") String previewFileHash,
                                               @AuthenticationPrincipal RenZhengYongHu user) {
        return service.confirm(file, previewFileHash, user.id());
    }
}
