package com.neu.riketiku.shijuan;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
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
public class PaperAssignmentTeacherController {
    private final PaperAssignmentService service;
    public PaperAssignmentTeacherController(PaperAssignmentService service) { this.service = service; }

    @PostMapping("/{paperId}/releases")
    public PaperAssignmentDtos.Release publish(@AuthenticationPrincipal RenZhengYongHu user,
                                               @PathVariable long paperId,
                                               @Valid @RequestBody PaperAssignmentDtos.Publish request) {
        return service.publish(user.id(), paperId, request);
    }
    @GetMapping("/releases/{releaseId}/stats")
    public PaperAssignmentDtos.ClassStats stats(@AuthenticationPrincipal RenZhengYongHu user,
                                                @PathVariable long releaseId) {
        return service.classStats(user.id(), releaseId);
    }
    @GetMapping("/releases/{releaseId}/students/{studentId}/profile")
    public PaperAssignmentDtos.StudentProfile profile(@AuthenticationPrincipal RenZhengYongHu user,
                                                      @PathVariable long releaseId, @PathVariable long studentId) {
        return service.studentProfile(user.id(), releaseId, studentId);
    }
    @GetMapping("/{paperId}/quality-assessment")
    public PaperAssignmentDtos.QualityAssessment quality(@AuthenticationPrincipal RenZhengYongHu user,
                                                         @PathVariable long paperId) {
        return service.quality(user.id(), paperId);
    }
    @PostMapping("/{paperId}/quality-assessment/ai")
    public PaperAssignmentDtos.AiQualityAssessment aiQuality(@AuthenticationPrincipal RenZhengYongHu user,
                                                             @PathVariable long paperId) {
        return service.aiQuality(user.id(), paperId);
    }
}
