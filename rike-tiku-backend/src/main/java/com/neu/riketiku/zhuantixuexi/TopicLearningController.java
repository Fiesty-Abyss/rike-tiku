package com.neu.riketiku.zhuantixuexi;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/topic-learning")
public class TopicLearningController {
    private final TopicLearningService service;

    public TopicLearningController(TopicLearningService service) {
        this.service = service;
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
}
