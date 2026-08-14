package com.neu.riketiku.aixuesheng;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/student/ai")
public class StudentAiController {
    private final StudentAiService service;
    private final AiRuntimeConfigurationService runtimeConfigurations;

    public StudentAiController(StudentAiService service, AiRuntimeConfigurationService runtimeConfigurations) {
        this.service = service;
        this.runtimeConfigurations = runtimeConfigurations;
    }

    @GetMapping("/model-options")
    public List<StudentAiDtos.ModelOption> modelOptions() {
        return runtimeConfigurations.safeTextOptions().stream().map(option -> new StudentAiDtos.ModelOption(option.id(),
                option.displayName(), option.modelCode(), option.available(), option.defaultOption(), option.capabilityTags())).toList();
    }

    @GetMapping("/capabilities")
    public StudentAiDtos.Capabilities capabilities() { return new StudentAiDtos.Capabilities(runtimeConfigurations.searchAvailable()); }

    @GetMapping("/analyses/{answerFactId}")
    public StudentAiDtos.Analysis analysis(@PathVariable Long answerFactId,
                                           @AuthenticationPrincipal RenZhengYongHu user) {
        return service.analysis(user.id(), answerFactId);
    }

    @PostMapping("/analyses/{answerFactId}")
    public StudentAiDtos.Analysis generateAnalysis(@PathVariable Long answerFactId,
                                                   @AuthenticationPrincipal RenZhengYongHu user) {
        return service.generateAnalysis(user.id(), answerFactId);
    }

    @PostMapping("/conversations")
    public StudentAiDtos.Conversation createConversation(
            @Valid @RequestBody StudentAiDtos.CreateConversationRequest request,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.createConversation(user.id(), request.answerFactId(), request.topicQuestionId(), request.knowledgeCardId(),
                request.contextType(), request.modelConfigId(), request.thinkingMode(),
                Boolean.TRUE.equals(request.webSearch()));
    }

    @GetMapping("/conversations/{conversationId}")
    public StudentAiDtos.Conversation conversation(@PathVariable Long conversationId,
                                                   @AuthenticationPrincipal RenZhengYongHu user) {
        return service.conversation(user.id(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public StudentAiDtos.Conversation sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody StudentAiDtos.SendMessageRequest request,
            @AuthenticationPrincipal RenZhengYongHu user) {
        return service.sendMessage(user.id(), conversationId, request.content());
    }
}
