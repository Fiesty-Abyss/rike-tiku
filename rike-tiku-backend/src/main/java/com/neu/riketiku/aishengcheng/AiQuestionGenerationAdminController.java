package com.neu.riketiku.aishengcheng;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ai-generation")
public class AiQuestionGenerationAdminController {
    private final AiQuestionGenerationService service;
    public AiQuestionGenerationAdminController(AiQuestionGenerationService service){this.service=service;}
    @GetMapping("/mothers") public List<AiQuestionGenerationDtos.MotherOption> mothers(@AuthenticationPrincipal RenZhengYongHu user){return service.mothers(user.id(),"ADMIN");}
    @GetMapping("/tasks") public List<AiQuestionGenerationDtos.Task> tasks(@AuthenticationPrincipal RenZhengYongHu user){return service.tasks(user.id(),"ADMIN");}
    @GetMapping("/tasks/{id}") public AiQuestionGenerationDtos.Task task(@PathVariable long id,@AuthenticationPrincipal RenZhengYongHu user){return service.task(id,user.id(),"ADMIN");}
    @PostMapping("/tasks") public AiQuestionGenerationDtos.Task generate(@Valid @RequestBody AiQuestionGenerationDtos.Generate request,@AuthenticationPrincipal RenZhengYongHu user){return service.generate(user.id(),"ADMIN",request);}
    @PostMapping("/candidates/{id}/review") public AiQuestionGenerationDtos.Candidate review(@PathVariable long id,@Valid @RequestBody AiQuestionGenerationDtos.Review request,@AuthenticationPrincipal RenZhengYongHu user){return service.review(user.id(),"ADMIN",id,request);}
    @GetMapping("/stats") public AiQuestionGenerationDtos.Stats stats(@AuthenticationPrincipal RenZhengYongHu user){return service.stats(user.id(),"ADMIN");}
}
