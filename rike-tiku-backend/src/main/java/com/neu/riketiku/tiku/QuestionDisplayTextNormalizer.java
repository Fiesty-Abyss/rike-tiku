package com.neu.riketiku.tiku;

import org.springframework.stereotype.Component;

@Component
public final class QuestionDisplayTextNormalizer {
    private static final String DEMO = "【演示】";
    private static final String TOPIC = "【专题演示】";
    public String normalize(String value){
        if(value==null)return "";
        String result=value;
        boolean internal=false;
        if(result.startsWith(TOPIC)){result=result.substring(TOPIC.length());internal=true;}
        else if(result.startsWith(DEMO)){result=result.substring(DEMO.length());internal=true;}
        if(internal&&(result.startsWith("覆盖：")||result.startsWith("变式：")))result=result.substring(3);
        return result;
    }
}
