package com.neu.riketiku.tiku.admin;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class QuestionAdminController {
    private final QuestionAdminService service;

    public QuestionAdminController(QuestionAdminService service) {
        this.service = service;
    }

    @GetMapping("/questions")
    public QuestionDtos.Page page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String usageMode,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String rightsStatus) {
        return service.page(page, size, subjectCode, questionType, usageMode, difficulty, status, keyword, rightsStatus);
    }

    @GetMapping("/questions/{id}")
    public QuestionDtos.Detail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/questions")
    public QuestionDtos.Detail create(
            @Valid @RequestBody QuestionDtos.Save request,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.create(request, user.id());
    }

    @PutMapping("/questions/{id}")
    public QuestionDtos.Detail update(@PathVariable Long id, @Valid @RequestBody QuestionDtos.Save request) {
        return service.update(id, request);
    }

    @PostMapping("/questions/{id}/submit-review")
    public QuestionDtos.Detail submit(@PathVariable Long id, @AuthenticationPrincipal RenZhengYongHu user) {
        return service.transition(id, "SUBMITTED", "DRAFT", "PENDING", null, user.id());
    }

    @PostMapping("/questions/{id}/approve")
    public QuestionDtos.Detail approve(@PathVariable Long id, @RequestBody(required = false) QuestionDtos.Action action,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.transition(id, "APPROVED", "PENDING", "PUBLISHED", action == null ? null : action.opinion(), user.id());
    }

    @PostMapping("/questions/{id}/return")
    public QuestionDtos.Detail reject(@PathVariable Long id, @RequestBody(required = false) QuestionDtos.Action action,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.transition(id, "REJECTED", "PENDING", "DRAFT", action == null ? null : action.opinion(), user.id());
    }

    @PostMapping("/questions/{id}/disable")
    public QuestionDtos.Detail disable(@PathVariable Long id, @AuthenticationPrincipal RenZhengYongHu user) {
        return service.transition(id, "DISABLED", "PUBLISHED", "DISABLED", null, user.id());
    }

    @PostMapping("/questions/{id}/republish")
    public QuestionDtos.Detail republish(@PathVariable Long id, @AuthenticationPrincipal RenZhengYongHu user) {
        return service.transition(id, "APPROVED", "DISABLED", "PUBLISHED", null, user.id());
    }

    @GetMapping("/knowledge-points")
    public List<QuestionDtos.KnowledgePoint> knowledgePoints(@RequestParam Long subjectId) {
        return service.knowledgePoints(subjectId);
    }
}
