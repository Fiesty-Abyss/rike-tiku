package com.neu.riketiku.tiku.fujian;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuestionAttachmentController {
    private final QuestionAttachmentContentService service;
    public QuestionAttachmentController(QuestionAttachmentContentService service) { this.service = service; }

    @GetMapping("/api/v1/admin/question-attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> admin(@PathVariable long attachmentId) { return response(service.admin(attachmentId)); }

    @GetMapping("/api/v1/student/practice-sessions/{sessionId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> practice(@PathVariable long sessionId, @PathVariable long attachmentId, @AuthenticationPrincipal RenZhengYongHu user) {
        return response(service.practice(user.id(), sessionId, attachmentId));
    }

    @GetMapping("/api/v1/student/wrong-questions/{questionId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> wrong(@PathVariable long questionId, @PathVariable long attachmentId, @AuthenticationPrincipal RenZhengYongHu user) {
        return response(service.wrongQuestion(user.id(), questionId, attachmentId));
    }

    private ResponseEntity<byte[]> response(QuestionAttachmentStorage.StoredImage image) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
    }
}
