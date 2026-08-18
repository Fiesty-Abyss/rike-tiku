package com.neu.riketiku.shijuan;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentStorage;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/papers")
public class PaperAssignmentStudentController {
    private final PaperAssignmentService service;
    public PaperAssignmentStudentController(PaperAssignmentService service) { this.service = service; }

    @GetMapping public List<PaperAssignmentDtos.Release> list(@AuthenticationPrincipal RenZhengYongHu user) {
        return service.studentList(user.id());
    }
    @GetMapping("/{releaseId}")
    public PaperAssignmentDtos.Detail detail(@AuthenticationPrincipal RenZhengYongHu user,
                                             @PathVariable long releaseId) {
        return service.studentDetail(user.id(), releaseId);
    }
    @GetMapping("/{releaseId}/items/{itemId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> attachment(@AuthenticationPrincipal RenZhengYongHu user, @PathVariable long releaseId,
                                             @PathVariable long itemId, @PathVariable long attachmentId) {
        return response(service.studentAttachment(user.id(), releaseId, itemId, attachmentId));
    }
    @PutMapping("/{releaseId}/draft")
    public void draft(@AuthenticationPrincipal RenZhengYongHu user, @PathVariable long releaseId,
                      @Valid @RequestBody PaperAssignmentDtos.SaveDraft request) {
        service.saveDraft(user.id(), releaseId, request);
    }
    @PostMapping("/{releaseId}/submit")
    public PaperAssignmentDtos.SubmitResult submit(@AuthenticationPrincipal RenZhengYongHu user,
                                                   @PathVariable long releaseId,
                                                   @Valid @RequestBody PaperAssignmentDtos.Submit request) {
        return service.submit(user.id(), releaseId, request);
    }
    private ResponseEntity<byte[]> response(QuestionAttachmentStorage.StoredImage image) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
    }
}
