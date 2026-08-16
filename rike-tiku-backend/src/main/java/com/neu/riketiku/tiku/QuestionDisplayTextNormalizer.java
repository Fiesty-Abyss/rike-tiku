package com.neu.riketiku.tiku;

import org.springframework.stereotype.Component;

@Component
public final class QuestionDisplayTextNormalizer {
    private static final String[] INTERNAL_PREFIXES = {"【演示】", "【专题演示】", "覆盖：", "变式："};

    public String normalize(String value){
        if(value==null)return "";
        String result=value;
        boolean removed;
        do {
            removed=false;
            result=result.stripLeading();
            for(String prefix:INTERNAL_PREFIXES){
                if(result.startsWith(prefix)){
                    result=result.substring(prefix.length());
                    removed=true;
                    break;
                }
            }
        } while(removed);
        return result.stripLeading();
    }
}
