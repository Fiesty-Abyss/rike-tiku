package com.neu.riketiku.xueshenglianxi;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/student")
public class StudentPracticeController {
    private final StudentPracticeService service;

    public StudentPracticeController(StudentPracticeService service) {
        this.service = service;
    }

    @GetMapping("/practice-options")
    public StudentPracticeDtos.Options options(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.options(user.id(), subjectId);
    }

    @PostMapping("/practice-sessions")
    public StudentPracticeDtos.Session create(
            @Valid @RequestBody StudentPracticeDtos.CreateRequest request,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.create(user.id(), request);
    }

    @GetMapping("/practice-availability")
    public StudentPracticeDtos.Availability availability(
            @RequestParam Long subjectId,
            @RequestParam(required = false) List<Long> knowledgePointIds,
            @RequestParam(required = false) List<String> questionTypes,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Long referenceQuestionId,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.availability(user.id(), new StudentPracticeDtos.CreateRequest(
                subjectId, knowledgePointIds, questionTypes, difficulty, 1, referenceQuestionId));
    }

    @GetMapping("/practice-sessions/{id}")
    public StudentPracticeDtos.Session session(
            @PathVariable Long id,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.session(user.id(), id);
    }

    @PostMapping("/practice-sessions/{id}/submit")
    public StudentPracticeDtos.Result submit(
            @PathVariable Long id,
            @Valid @RequestBody StudentPracticeDtos.SubmitRequest request,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.submit(user.id(), id, request);
    }

    @GetMapping("/practice-sessions/{id}/result")
    public StudentPracticeDtos.Result result(
            @PathVariable Long id,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.result(user.id(), id);
    }

    @GetMapping("/wrong-questions")
    public StudentPracticeDtos.WrongQuestionPage wrongQuestions(
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required=false) Long knowledgePointId,@RequestParam(required=false) String status,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.wrongQuestions(user.id(), subjectCode,knowledgePointId,status,keyword,page,Math.min(100,Math.max(1,size)));
    }

    @PostMapping("/wrong-questions/{questionId}/retry") public StudentPracticeDtos.Session retry(@PathVariable Long questionId,@AuthenticationPrincipal RenZhengYongHu user){return service.retryWrongQuestion(user.id(),questionId);}
    @PostMapping("/wrong-questions/{questionId}/archive") public void archive(@PathVariable Long questionId,@AuthenticationPrincipal RenZhengYongHu user){service.archiveWrongQuestion(user.id(),questionId);}

    @GetMapping("/wrong-questions/{questionId}")
    public StudentPracticeDtos.WrongQuestionDetail wrongQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.wrongQuestion(user.id(), questionId);
    }
}
