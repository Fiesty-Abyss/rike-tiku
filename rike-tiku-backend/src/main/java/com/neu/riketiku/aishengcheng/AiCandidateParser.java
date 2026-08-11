package com.neu.riketiku.aishengcheng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class AiCandidateParser {
    private static final Set<String> ROOT_FIELDS=Set.of("candidates");
    private static final Set<String> FIELDS=Set.of("stem","questionType","difficulty","options","correctAnswer","standardAnalysis","knowledgePoints","variationSummary");
    private static final Set<String> TYPES=Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","FILL_BLANK");
    private static final Pattern DANGEROUS=Pattern.compile("(?i)(<\\s*/?\\s*(script|iframe|object|embed)|javascript\\s*:|onerror\\s*=)");
    private final ObjectMapper mapper=new ObjectMapper();

    List<Candidate> parse(String json,int expectedCount,String expectedType,int expectedDifficulty,Set<Long> allowedPoints){
        try{
            JsonNode root=mapper.readTree(json);
            if(!root.isObject()||!fields(root).equals(ROOT_FIELDS)||!root.path("candidates").isArray()
                    ||root.path("candidates").size()!=expectedCount) invalid();
            List<Candidate> values=new ArrayList<>();
            for(JsonNode node:root.path("candidates")) values.add(candidate(node,expectedType,expectedDifficulty,allowedPoints));
            return List.copyOf(values);
        }catch(InvalidCandidateException exception){throw exception;}
        catch(Exception exception){throw new InvalidCandidateException();}
    }

    private Candidate candidate(JsonNode node,String expectedType,int expectedDifficulty,Set<Long> allowedPoints){
        if(!node.isObject()||!fields(node).equals(FIELDS)) invalid();
        String stem=text(node,"stem",1,6000); String type=text(node,"questionType",1,32);
        String analysis=text(node,"standardAnalysis",1,8000); String summary=text(node,"variationSummary",1,1000);
        if(!TYPES.contains(type)||!type.equals(expectedType)||!node.path("difficulty").canConvertToInt()
                ||node.path("difficulty").intValue()!=expectedDifficulty) invalid();
        if(danger(stem)||danger(analysis)||danger(summary)) invalid();
        JsonNode answer=node.path("correctAnswer"); if(!answer.isObject()) invalid();
        List<Option> options=options(node.path("options"));
        validateAnswer(type,options,answer);
        JsonNode points=node.path("knowledgePoints");
        if(!points.isArray()||points.isEmpty()||points.size()>10) invalid();
        List<Long> pointIds=new ArrayList<>();
        for(JsonNode point:points){if(!point.canConvertToLong()||!allowedPoints.contains(point.longValue()))invalid();pointIds.add(point.longValue());}
        if(new HashSet<>(pointIds).size()!=pointIds.size())invalid();
        return new Candidate(stem,type,expectedDifficulty,options,mapper.writeValueAsString(answer),analysis,List.copyOf(pointIds),summary);
    }
    private List<Option> options(JsonNode node){
        if(!node.isArray()||node.size()>8)invalid(); List<Option> values=new ArrayList<>(); Set<String> labels=new HashSet<>();
        for(JsonNode value:node){
            if(!value.isObject()||!fields(value).equals(Set.of("label","content","correct")))invalid();
            String label=text(value,"label",1,16);String content=text(value,"content",1,3000);
            if(!value.path("correct").isBoolean()||!labels.add(label)||danger(content))invalid();
            values.add(new Option(label,content,value.path("correct").booleanValue()));
        }
        return List.copyOf(values);
    }
    private void validateAnswer(String type,List<Option> options,JsonNode answer){
        if("FILL_BLANK".equals(type)){
            if(!options.isEmpty()||!answer.path("schemaVersion").canConvertToInt()||!type.equals(answer.path("type").asText())
                    ||!answer.path("blanks").isArray()||answer.path("blanks").isEmpty())invalid();
            for(JsonNode blank:answer.path("blanks")){
                JsonNode accepted=blank.path("acceptedAnswers");
                if(!blank.isObject()||!accepted.isArray()||accepted.isEmpty()||accepted.size()>8)invalid();
                for(JsonNode value:accepted)if(!value.isTextual()||value.asText().isBlank()||value.asText().length()>300||danger(value.asText()))invalid();
            }
            return;
        }
        if(options.size()<2||!answer.path("schemaVersion").canConvertToInt()||!type.equals(answer.path("type").asText())
                ||!answer.path("optionLabels").isArray())invalid();
        Set<String> correct=new HashSet<>();for(Option option:options)if(option.correct())correct.add(option.label());
        Set<String> declared=new HashSet<>();for(JsonNode label:answer.path("optionLabels")){if(!label.isTextual())invalid();declared.add(label.asText());}
        if(!correct.equals(declared)||("SINGLE_CHOICE".equals(type)&&correct.size()!=1)
                ||("MULTIPLE_CHOICE".equals(type)&&correct.size()<2))invalid();
    }
    private String text(JsonNode node,String field,int min,int max){JsonNode value=node.path(field);if(!value.isTextual())invalid();String text=value.asText().trim();if(text.length()<min||text.length()>max)invalid();return text;}
    private boolean danger(String value){return DANGEROUS.matcher(value).find();}
    private Set<String> fields(JsonNode node){return new HashSet<>(node.propertyNames());}
    private void invalid(){throw new InvalidCandidateException();}
    record Candidate(String stem,String questionType,int difficulty,List<Option> options,String correctAnswer,
                     String standardAnalysis,List<Long> knowledgePointIds,String variationSummary){ }
    record Option(String label,String content,boolean correct){ }
    static final class InvalidCandidateException extends RuntimeException { }
}
