package com.neu.riketiku.zhuantixuexi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neu.riketiku.aishengcheng.AiQuestionGenerationDtos;
import com.neu.riketiku.aishengcheng.AiQuestionGenerationService;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TopicLearningControllerTest {
    @Test
    void propagatesVisualRequirementAndPrimaryKnowledgeSelectionToGenerationService() {
        TopicLearningService topics=mock(TopicLearningService.class);
        AiQuestionGenerationService generation=mock(AiQuestionGenerationService.class);
        TopicLearningController controller=new TopicLearningController(topics,generation);
        var detail=new TopicLearningDtos.TopicDetail(19L,1L,"PHYSICS","物理","专题","材料","CALCULATION",3,"STANDARD",
                List.of(new TopicLearningDtos.KnowledgePoint(11L,"主知识点","力学>主知识点"),
                        new TopicLearningDtos.KnowledgePoint(12L,"关联知识点","力学>关联知识点")),List.of(),List.of());
        when(topics.detail(7L,19L)).thenReturn(detail);
        RenZhengYongHu user=new RenZhengYongHu(7L,"anonymous",List.of("STUDENT"),false);

        controller.variants(19L,new TopicLearningDtos.VariantRequest(3,"COMBINED",1,true,true),user);
        ArgumentCaptor<AiQuestionGenerationDtos.Generate> primary=ArgumentCaptor.forClass(AiQuestionGenerationDtos.Generate.class);
        verify(generation).generateTopic(eq(7L),primary.capture(),eq(true),eq(true));
        assertThat(primary.getValue().knowledgePointIds()).containsExactly(11L);

        controller.variants(19L,new TopicLearningDtos.VariantRequest(3,"COMBINED",1,false,false),user);
        ArgumentCaptor<AiQuestionGenerationDtos.Generate> related=ArgumentCaptor.forClass(AiQuestionGenerationDtos.Generate.class);
        verify(generation).generateTopic(eq(7L),related.capture(),eq(false),eq(false));
        assertThat(related.getValue().knowledgePointIds()).containsExactly(11L,12L);
    }
}
