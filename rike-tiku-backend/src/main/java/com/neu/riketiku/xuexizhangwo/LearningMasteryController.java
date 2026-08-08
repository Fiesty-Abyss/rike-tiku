package com.neu.riketiku.xuexizhangwo;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.StudentLearningSummary;
import com.neu.riketiku.xuexizhangwo.LearningMasteryDtos.TeacherScopeLearningSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LearningMasteryController {
    private final LearningMasteryService service;

    public LearningMasteryController(LearningMasteryService service) {
        this.service = service;
    }

    @GetMapping("/student/learning-summary")
    public StudentLearningSummary studentSummary(@RequestParam long subjectId,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.studentSummary(principal.id(), subjectId);
    }

    @GetMapping("/teacher/scopes/{scopeId}/learning-summary")
    public TeacherScopeLearningSummary teacherSummary(@PathVariable long scopeId,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.teacherScopeSummary(principal.id(), scopeId);
    }
}
