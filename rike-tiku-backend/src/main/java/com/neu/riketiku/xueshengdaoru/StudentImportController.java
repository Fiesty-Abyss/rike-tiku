package com.neu.riketiku.xueshengdaoru;

import com.neu.riketiku.xueshengdaoru.response.StudentImportPreviewResponse;
import com.neu.riketiku.xueshengdaoru.response.StudentImportConfirmResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/student-import")
public class StudentImportController {
    private final StudentImportService studentImportService;
    private final StudentImportConfirmService confirmService;

    public StudentImportController(StudentImportService studentImportService, StudentImportConfirmService confirmService) {
        this.studentImportService = studentImportService;
        this.confirmService = confirmService;
    }

    @GetMapping(value = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=student-import-template.xlsx");
        studentImportService.writeTemplate(response.getOutputStream());
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentImportPreviewResponse preview(@RequestParam("file") MultipartFile file) {
        return studentImportService.preview(file);
    }

    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportConfirmResponse> confirm(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache")
                .body(confirmService.confirm(file));
    }
}
