package com.neu.riketiku.tiku.teacher;
import com.neu.riketiku.renzheng.RenZhengYongHu;import com.neu.riketiku.tiku.admin.QuestionDtos;import jakarta.validation.Valid;import java.util.List;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/teacher/private-questions") public class TeacherPrivateQuestionController{
 private final TeacherPrivateQuestionService service;public TeacherPrivateQuestionController(TeacherPrivateQuestionService service){this.service=service;}
 @GetMapping public List<TeacherPrivateQuestionService.Item> list(@AuthenticationPrincipal RenZhengYongHu u){return service.list(u.id());}
 @GetMapping("/{id}") public QuestionDtos.Detail detail(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id){return service.detail(u.id(),id);}
 @PostMapping public TeacherPrivateQuestionService.Item create(@AuthenticationPrincipal RenZhengYongHu u,@RequestParam long teachingAssignmentId,@Valid @RequestBody QuestionDtos.Save save){return service.create(u.id(),teachingAssignmentId,save);}
 @PutMapping("/{id}") public TeacherPrivateQuestionService.Item update(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id,@Valid @RequestBody QuestionDtos.Save save){return service.update(u.id(),id,save);}
 @PostMapping("/{id}/publish") public TeacherPrivateQuestionService.Item publish(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id){return service.publish(u.id(),id);}
 @PostMapping("/{id}/submit-admin") public TeacherPrivateQuestionService.Item submitAdmin(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id){return service.submitAdmin(u.id(),id);}
 @PostMapping("/{id}/disable") public TeacherPrivateQuestionService.Item disable(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id){return service.disable(u.id(),id);}
 @DeleteMapping("/{id}") public void delete(@AuthenticationPrincipal RenZhengYongHu u,@PathVariable long id){service.delete(u.id(),id);}
}
