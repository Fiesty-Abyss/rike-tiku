package com.neu.riketiku.xueshenglianxi;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ObjectiveAnswerGrader {
    private final ObjectMapper mapper=new ObjectMapper();
    public boolean grade(String type,String correctAnswer,String optionsJson,JsonNode submitted){
        JsonNode answer=mapper.readTree(correctAnswer);
        return switch(type){case "SINGLE_CHOICE"->single(answer,submitted,optionsJson);case "MULTIPLE_CHOICE"->multiple(answer,submitted,optionsJson);case "FILL_BLANK"->fill(answer,submitted);default->throw error("PRACTICE_QUESTION_INVALID","题型不支持自动判分",HttpStatus.CONFLICT);};
    }
    private boolean single(JsonNode answer,JsonNode submitted,String options){if(!submitted.isTextual())throw error("PRACTICE_ANSWER_FORMAT_INVALID","单选题答案必须是选项标识",HttpStatus.BAD_REQUEST);String value=label(submitted.asText());if(!labels(options).contains(value))throw error("PRACTICE_OPTION_INVALID","答案不是有效选项",HttpStatus.BAD_REQUEST);JsonNode expected=answer.path("optionLabels");return expected.isArray()&&expected.size()==1&&value.equals(label(expected.get(0).asText()));}
    private boolean multiple(JsonNode answer,JsonNode submitted,String options){if(!submitted.isArray())throw error("PRACTICE_ANSWER_FORMAT_INVALID","多选题答案必须是数组",HttpStatus.BAD_REQUEST);Set<String> available=labels(options),actual=new HashSet<>(),expected=new HashSet<>();for(JsonNode item:submitted){String value=label(item.asText());if(!available.contains(value))throw error("PRACTICE_OPTION_INVALID","答案包含无效选项",HttpStatus.BAD_REQUEST);actual.add(value);}answer.path("optionLabels").forEach(item->expected.add(label(item.asText())));return !actual.isEmpty()&&actual.equals(expected);}
    private boolean fill(JsonNode answer,JsonNode submitted){JsonNode blanks=answer.path("blanks");if(!submitted.isArray()||!blanks.isArray()||blanks.size()!=submitted.size())throw error("PRACTICE_BLANK_COUNT_INVALID","填空答案数量不一致",HttpStatus.BAD_REQUEST);for(int i=0;i<blanks.size();i++){JsonNode blank=blanks.get(i);boolean sensitive=blank.path("caseSensitive").asBoolean(false);String actual=normalize(submitted.get(i).asText(),sensitive);boolean ok=false;for(JsonNode accepted:blank.path("acceptedAnswers"))if(actual.equals(normalize(accepted.asText(),sensitive))){ok=true;break;}if(!ok)return false;}return true;}
    private Set<String> labels(String json){try{return mapper.readValue(json,new TypeReference<List<StudentPracticeDtos.Option>>(){}).stream().map(StudentPracticeDtos.Option::label).map(this::label).collect(java.util.stream.Collectors.toSet());}catch(Exception e){throw error("PRACTICE_QUESTION_INVALID","题目选项不合法",HttpStatus.CONFLICT);}}
    private String label(String v){return v==null?"":v.trim().toUpperCase(Locale.ROOT);}
    private String normalize(String value,boolean sensitive){StringBuilder b=new StringBuilder();for(char c:value.trim().toCharArray()){if(c=='\u3000')b.append(' ');else if(c>='\uFF01'&&c<='\uFF5E')b.append((char)(c-0xFEE0));else b.append(c);}String r=b.toString().replace('，',',').replace('。','.').replace('；',';').replace('：',':').trim();return sensitive?r:r.toLowerCase(Locale.ROOT);}
    private RenZhengYeWuYiChang error(String code,String message,HttpStatus status){return new RenZhengYeWuYiChang(code,message,status);}
}
