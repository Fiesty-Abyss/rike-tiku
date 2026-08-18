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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/papers")
public class PaperController {
    private final PaperService service;
    private final QuestionAttachmentStorage storage;

    public PaperController(PaperService service, QuestionAttachmentStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping public List<PaperDtos.ListItem> list(@AuthenticationPrincipal RenZhengYongHu user) { return service.list(user.id()); }
    @GetMapping("/questions")
    public List<PaperDtos.QuestionOption> questions(@AuthenticationPrincipal RenZhengYongHu user,
            @RequestParam long subjectId, @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String questionType, @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        return service.questions(user.id(), subjectId, knowledgePointId, questionType, difficulty, keyword);
    }
    @GetMapping("/knowledge-points")
    public List<PaperDtos.KnowledgePoint> points(@AuthenticationPrincipal RenZhengYongHu user, @RequestParam long subjectId) {
        return service.knowledgePoints(user.id(), subjectId);
    }
    @GetMapping("/questions/{questionId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> questionAttachment(@AuthenticationPrincipal RenZhengYongHu user, @PathVariable long questionId,
                                                       @PathVariable long attachmentId) {
        PaperService.PaperAttachmentContent attachment = service.teacherQuestionAttachment(user.id(), questionId, attachmentId);
        return response(storage.read(attachment.relativePath(), attachment.hash()));
    }
    @GetMapping("/{id}") public PaperDtos.Paper detail(@AuthenticationPrincipal RenZhengYongHu user, @PathVariable long id) { return service.detail(user.id(), id); }
    @GetMapping("/{paperId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> attachment(@AuthenticationPrincipal RenZhengYongHu user, @PathVariable long paperId,
                                               @PathVariable long attachmentId) {
        PaperService.PaperAttachmentContent attachment = service.teacherAttachment(user.id(), paperId, attachmentId);
        return response(storage.read(attachment.relativePath(), attachment.hash()));
    }
    @PostMapping public PaperDtos.Paper save(@AuthenticationPrincipal RenZhengYongHu user, @Valid @RequestBody PaperDtos.Save request) { return service.save(user.id(), request); }
    @PostMapping("/rule") public PaperDtos.Paper rule(@AuthenticationPrincipal RenZhengYongHu user, @Valid @RequestBody PaperDtos.Rule request) { return service.rule(user.id(), request); }
    @PostMapping("/random") public PaperDtos.Paper random(@AuthenticationPrincipal RenZhengYongHu user, @Valid @RequestBody PaperDtos.Rule request) { return service.random(user.id(), request); }

    private ResponseEntity<byte[]> response(QuestionAttachmentStorage.StoredImage image) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
    }
}
