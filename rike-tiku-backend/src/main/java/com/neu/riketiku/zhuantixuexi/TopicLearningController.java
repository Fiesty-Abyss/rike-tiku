package com.neu.riketiku.zhuantixuexi;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.aishengcheng.AiQuestionGenerationDtos;
import com.neu.riketiku.aishengcheng.AiQuestionGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/student/topic-learning")
public class TopicLearningController {
    private final TopicLearningService service;
    private final AiQuestionGenerationService generation;

    public TopicLearningController(TopicLearningService service,AiQuestionGenerationService generation) {
        this.service = service;this.generation=generation;
    }

    @GetMapping
    public List<TopicLearningDtos.TopicItem> list(@RequestParam(required = false) String subjectCode,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.list(user.id(), subjectCode);
    }

    @GetMapping("/{id}")
    public TopicLearningDtos.TopicDetail detail(@PathVariable Long id,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.detail(user.id(), id);
    }

    @GetMapping("/units")
    public List<TopicLearningDtos.UnitItem> units(@RequestParam(required=false) String subjectCode,
            @AuthenticationPrincipal RenZhengYongHu user){return service.units(user.id(),subjectCode);}

    @GetMapping("/units/{id}")
    public TopicLearningDtos.UnitDetail unit(@PathVariable Long id,@AuthenticationPrincipal RenZhengYongHu user){
        return service.unit(user.id(),id);
    }

    @PostMapping("/{id}/variants")
    public AiQuestionGenerationDtos.Task variants(@PathVariable Long id,@Valid @RequestBody TopicLearningDtos.VariantRequest request,
            @AuthenticationPrincipal RenZhengYongHu user){
        TopicLearningDtos.TopicDetail topic=service.detail(user.id(),id);
        List<Long> points=request.keepPrimaryKnowledgePoint()
                ? topic.knowledgePoints().stream().limit(1).map(TopicLearningDtos.KnowledgePoint::id).toList()
                : topic.knowledgePoints().stream().map(TopicLearningDtos.KnowledgePoint::id).toList();
        return generation.generateTopic(user.id(),new AiQuestionGenerationDtos.Generate(id,"SUBJECTIVE",points,
                request.targetDifficulty(),request.variationMode(),request.count()),
                request.requireVisualContext(),request.keepPrimaryKnowledgePoint());
    }

    @PostMapping("/variants/{questionId}/submit-review")
    public AiQuestionGenerationDtos.Task submitVariant(@PathVariable Long questionId,
            @AuthenticationPrincipal RenZhengYongHu user){
        return generation.submitStudentTopicVariant(user.id(),questionId);
    }

    @DeleteMapping("/variants/{questionId}")
    public ResponseEntity<Void> discardVariant(@PathVariable Long questionId,
            @AuthenticationPrincipal RenZhengYongHu user){
        generation.discardStudentTopicVariant(user.id(),questionId);
        return ResponseEntity.noContent().build();
    }
}
