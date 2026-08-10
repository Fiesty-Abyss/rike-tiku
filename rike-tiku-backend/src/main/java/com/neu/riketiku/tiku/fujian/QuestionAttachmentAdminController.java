package com.neu.riketiku.tiku.fujian;

import com.neu.riketiku.tiku.admin.QuestionDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/questions")
public class QuestionAttachmentAdminController {
    private final QuestionAttachmentAdminService service;

    public QuestionAttachmentAdminController(QuestionAttachmentAdminService service) {
        this.service = service;
    }

    @PostMapping(value = "/{questionId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionDtos.Attachment upload(@PathVariable long questionId,
            @RequestParam String position, @RequestParam MultipartFile file) {
        return service.upload(questionId, position, file);
    }

    @PutMapping(value = "/{questionId}/attachments/{attachmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuestionDtos.Attachment replace(@PathVariable long questionId, @PathVariable long attachmentId,
            @RequestParam MultipartFile file) {
        return service.replace(questionId, attachmentId, file);
    }

    @DeleteMapping("/{questionId}/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable long questionId, @PathVariable long attachmentId) {
        service.delete(questionId, attachmentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
