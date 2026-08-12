package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/ai/variants")
public class StudentAiVariantController {
    private final StudentAiVariantService service;public StudentAiVariantController(StudentAiVariantService service){this.service=service;}
    @PostMapping public StudentAiVariantDtos.Variant generate(@AuthenticationPrincipal RenZhengYongHu user,@Valid @RequestBody StudentAiVariantDtos.Generate request){return service.generate(user.id(),request.answerFactId());}
    @GetMapping("/{id}") public StudentAiVariantDtos.Variant detail(@AuthenticationPrincipal RenZhengYongHu user,@PathVariable long id){return service.detail(user.id(),id);}
    @PostMapping("/{id}/answer") public StudentAiVariantDtos.Variant answer(@AuthenticationPrincipal RenZhengYongHu user,@PathVariable long id,@Valid @RequestBody StudentAiVariantDtos.Answer request){return service.answer(user.id(),id,request.answer());}
    @PostMapping("/{id}/submit-review") public StudentAiVariantDtos.Variant submit(@AuthenticationPrincipal RenZhengYongHu user,@PathVariable long id){return service.submit(user.id(),id);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> discard(@AuthenticationPrincipal RenZhengYongHu user,@PathVariable long id){service.discard(user.id(),id);return ResponseEntity.noContent().build();}
}
