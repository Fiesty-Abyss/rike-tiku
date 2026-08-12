package com.neu.riketiku.aishengcheng;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/ai-generation")
public class AiQuestionGenerationTeacherController {
    private final AiQuestionGenerationService service;
    public AiQuestionGenerationTeacherController(AiQuestionGenerationService service){this.service=service;}
    @GetMapping("/mothers") public List<AiQuestionGenerationDtos.MotherOption> mothers(@AuthenticationPrincipal RenZhengYongHu user){return service.mothers(user.id(),"TEACHER");}
    @GetMapping("/knowledge-points") public List<AiQuestionGenerationDtos.KnowledgePointOption> knowledgePoints(@RequestParam long subjectId,@AuthenticationPrincipal RenZhengYongHu user){return service.knowledgePoints(user.id(),"TEACHER",subjectId);}
    @GetMapping("/tasks") public List<AiQuestionGenerationDtos.Task> tasks(@AuthenticationPrincipal RenZhengYongHu user){return service.tasks(user.id(),"TEACHER");}
    @GetMapping("/tasks/{id}") public AiQuestionGenerationDtos.Task task(@PathVariable long id,@AuthenticationPrincipal RenZhengYongHu user){return service.task(id,user.id(),"TEACHER");}
    @PostMapping("/tasks") public AiQuestionGenerationDtos.Task generate(@Valid @RequestBody AiQuestionGenerationDtos.Generate request,@AuthenticationPrincipal RenZhengYongHu user){return service.generate(user.id(),"TEACHER",request);}
    @PostMapping("/candidates/{id}/review") public AiQuestionGenerationDtos.Candidate review(@PathVariable long id,@Valid @RequestBody AiQuestionGenerationDtos.Review request,@AuthenticationPrincipal RenZhengYongHu user){return service.review(user.id(),"TEACHER",id,request);}
}
