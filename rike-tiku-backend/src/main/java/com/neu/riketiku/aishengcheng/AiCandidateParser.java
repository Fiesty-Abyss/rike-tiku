package com.neu.riketiku.aishengcheng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class AiCandidateParser {
    private static final Set<String> ROOT_FIELDS=Set.of("schemaVersion","candidates");
    private static final Set<String> FIELDS=Set.of("stem","questionType","difficulty","options","correctAnswer","standardAnalysis","variationMode","variationSummary","changedDimensions");
    private static final Set<String> TYPES=Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","FILL_BLANK","SUBJECTIVE");
    private static final Set<String> MODES=Set.of("SCENARIO_TRANSFER","CONDITION_RECOMBINATION","REPRESENTATION_SWITCH","MULTI_STEP_EXTENSION","DISTRACTOR_REDESIGN","COMBINED");
    private static final Set<String> DIMENSIONS=Set.of("SCENARIO","CONDITION","DATA","REPRESENTATION","REASONING_PATH","DISTRACTOR","KNOWLEDGE_COMBINATION");
    private static final Pattern FENCE=Pattern.compile("(?s)^\\s*```(?:json)?\\s*([\\s\\S]*?)\\s*```\\s*$",Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS=Pattern.compile("(?i)(<\\s*/?\\s*(script|iframe|object|embed)|javascript\\s*:|onerror\\s*=)");
    private final ObjectMapper mapper=new ObjectMapper();

    List<Candidate> parse(String content,int expectedCount,String expectedType,int expectedDifficulty,String expectedMode){
        if(content==null||content.isBlank())throw invalid("EMPTY_CONTENT","$","Provider 返回内容为空");
        String json=unwrap(content);
        final JsonNode root;
        try{root=mapper.readTree(json);}catch(Exception e){throw invalid("INVALID_JSON","$","返回内容不是合法 JSON");}
        if(!root.isObject())throw invalid("FIELD_TYPE_INVALID","$","根节点必须是对象");
        exactFields(root,ROOT_FIELDS,"$");
        if(root.path("schemaVersion").asInt(-1)!=2)throw invalid("SCHEMA_VERSION_INVALID","$.schemaVersion","必须使用 schemaVersion 2");
        JsonNode nodes=root.path("candidates");
        if(!nodes.isArray())throw invalid("FIELD_TYPE_INVALID","$.candidates","candidates 必须是数组");
        if(nodes.size()!=expectedCount)throw invalid("COUNT_MISMATCH","$.candidates","候选数量与请求不一致");
        List<Candidate> values=new ArrayList<>();
        for(int i=0;i<nodes.size();i++)values.add(candidate(nodes.get(i),expectedType,expectedDifficulty,expectedMode,"$.candidates["+i+"]"));
        return List.copyOf(values);
    }

    private String unwrap(String value){
        String trimmed=value.trim();
        if(!trimmed.startsWith("```"))return trimmed;
        Matcher matcher=FENCE.matcher(trimmed);
        if(!matcher.matches())throw invalid("FENCE_CONTENT_INVALID","$","代码围栏外不得包含文字");
        return matcher.group(1).trim();
    }

    private Candidate candidate(JsonNode node,String expectedType,int expectedDifficulty,String expectedMode,String path){
        if(!node.isObject())throw invalid("FIELD_TYPE_INVALID",path,"候选必须是对象");
        exactFields(node,FIELDS,path);
        String stem=text(node,"stem",1,6000,path);String type=text(node,"questionType",1,32,path);
        String analysis=text(node,"standardAnalysis",1,8000,path);String summary=text(node,"variationSummary",1,1000,path);
        String mode=text(node,"variationMode",1,48,path);
        if(!TYPES.contains(type)||!type.equals(expectedType))throw invalid("QUESTION_TYPE_INVALID",path+".questionType","题型与请求不一致");
        if(!node.path("difficulty").canConvertToInt()||node.path("difficulty").intValue()!=expectedDifficulty)throw invalid("DIFFICULTY_MISMATCH",path+".difficulty","难度与请求不一致");
        if(!MODES.contains(mode)||!mode.equals(expectedMode))throw invalid("VARIATION_MODE_INVALID",path+".variationMode","变化方式与请求不一致");
        if(danger(stem)||danger(analysis)||danger(summary))throw invalid("UNSAFE_CONTENT",path,"内容包含不安全标记");
        JsonNode answer=node.path("correctAnswer");if(!answer.isObject())throw invalid("ANSWER_INVALID",path+".correctAnswer","答案必须是对象");
        List<Option> options=options(node.path("options"),path+".options");validateAnswer(type,options,answer,path+".correctAnswer");
        JsonNode dimensions=node.path("changedDimensions");
        if(!dimensions.isArray())throw invalid("FIELD_TYPE_INVALID",path+".changedDimensions","变化维度必须是数组");
        Set<String> unique=new HashSet<>();
        for(JsonNode dimension:dimensions){if(!dimension.isTextual()||!DIMENSIONS.contains(dimension.asText()))throw invalid("CHANGED_DIMENSION_INVALID",path+".changedDimensions","包含非法变化维度");unique.add(dimension.asText());}
        if(unique.size()<2)throw invalid("CHANGED_DIMENSIONS_INSUFFICIENT",path+".changedDimensions","至少需要两个不同变化维度");
        return new Candidate(stem,type,expectedDifficulty,options,mapper.writeValueAsString(answer),analysis,mode,summary,List.copyOf(unique));
    }

    private List<Option> options(JsonNode node,String path){
        if(!node.isArray()||node.size()>8)throw invalid("OPTIONS_INVALID",path,"选项必须是最多 8 项的数组");
        List<Option> values=new ArrayList<>();Set<String> labels=new HashSet<>();
        for(int i=0;i<node.size();i++){
            JsonNode value=node.get(i);String item=path+"["+i+"]";
            if(!value.isObject())throw invalid("OPTIONS_INVALID",item,"选项必须是对象");
            exactFields(value,Set.of("label","content","correct"),item);
            String label=text(value,"label",1,16,item);String content=text(value,"content",1,3000,item);
            if(!value.path("correct").isBoolean()||!labels.add(label)||danger(content))throw invalid("OPTIONS_INVALID",item,"选项标签、正文或正确性非法");
            values.add(new Option(label,content,value.path("correct").booleanValue()));
        }
        return List.copyOf(values);
    }

    private void validateAnswer(String type,List<Option> options,JsonNode answer,String path){
        if("SUBJECTIVE".equals(type)){
            exactFields(answer,Set.of("schemaVersion","type"),path);
            if(!options.isEmpty()||answer.path("schemaVersion").asInt(-1)!=1||!"SUBJECTIVE".equals(answer.path("type").asText()))throw invalid("ANSWER_INVALID",path,"主观专题题答案必须为 schemaVersion 1 / SUBJECTIVE");
            return;
        }
        if("FILL_BLANK".equals(type)){
            if(!options.isEmpty()||answer.path("schemaVersion").asInt(-1)!=1||!type.equals(answer.path("type").asText())||!answer.path("blanks").isArray()||answer.path("blanks").isEmpty())throw invalid("ANSWER_INVALID",path,"填空答案结构非法");
            for(JsonNode blank:answer.path("blanks")){JsonNode accepted=blank.path("acceptedAnswers");if(!blank.isObject()||!accepted.isArray()||accepted.isEmpty()||accepted.size()>8)throw invalid("ANSWER_INVALID",path,"每空必须提供可接受答案");for(JsonNode value:accepted)if(!value.isTextual()||value.asText().isBlank()||value.asText().length()>300||danger(value.asText()))throw invalid("ANSWER_INVALID",path,"可接受答案非法");}
            return;
        }
        if(options.size()<2||answer.path("schemaVersion").asInt(-1)!=1||!type.equals(answer.path("type").asText())||!answer.path("optionLabels").isArray())throw invalid("ANSWER_INVALID",path,"选择题答案结构非法");
        Set<String> correct=new HashSet<>();for(Option option:options)if(option.correct())correct.add(option.label());
        Set<String> declared=new HashSet<>();for(JsonNode label:answer.path("optionLabels")){if(!label.isTextual())throw invalid("ANSWER_INVALID",path,"答案标签必须是文本");declared.add(label.asText());}
        if(!correct.equals(declared)||("SINGLE_CHOICE".equals(type)&&correct.size()!=1)||("MULTIPLE_CHOICE".equals(type)&&correct.size()<2))throw invalid("ANSWER_OPTION_MISMATCH",path,"答案与选项正确性不一致");
    }
    private void exactFields(JsonNode node,Set<String> expected,String path){Set<String> actual=new HashSet<>(node.propertyNames());Set<String> missing=new HashSet<>(expected);missing.removeAll(actual);if(!missing.isEmpty())throw invalid("FIELD_MISSING",path,"缺少字段 "+missing);Set<String> extra=new HashSet<>(actual);extra.removeAll(expected);if(!extra.isEmpty())throw invalid("FIELD_EXTRA",path,"存在多余字段 "+extra);}
    private String text(JsonNode node,String field,int min,int max,String path){JsonNode value=node.path(field);if(!value.isTextual())throw invalid("FIELD_TYPE_INVALID",path+"."+field,"字段必须是文本");String text=value.asText().trim();if(text.length()<min||text.length()>max)throw invalid("FIELD_LENGTH_INVALID",path+"."+field,"字段长度非法");return text;}
    private boolean danger(String value){return DANGEROUS.matcher(value).find();}
    private InvalidCandidateException invalid(String code,String field,String message){return new InvalidCandidateException(code,field,message);}
    record Candidate(String stem,String questionType,int difficulty,List<Option> options,String correctAnswer,String standardAnalysis,String variationMode,String variationSummary,List<String> changedDimensions){}
    record Option(String label,String content,boolean correct){}
    static final class InvalidCandidateException extends RuntimeException {private final String code;private final String field;InvalidCandidateException(String code,String field,String message){super(message);this.code=code;this.field=field;}String code(){return code;}String field(){return field;}}
}
