package com.neu.riketiku.aishengcheng;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class AiCandidateNoveltyService {
    private static final Pattern NUMBERS_UNITS_NAMES=Pattern.compile("(?iu)[\\d.]+|[a-z]+|千克|克|米|秒|牛|焦|伏|安|欧|摄氏度|小明|小红|甲|乙|丙|苹果|小球");
    Result evaluate(String motherStem,String motherOptions,String motherAnalysis,AiCandidateParser.Candidate candidate){
        double stem=similarity(normalize(motherStem),normalize(candidate.stem()));double option=similarity(normalize(motherOptions),normalize(candidate.options().toString()));double analysis=similarity(normalize(motherAnalysis),normalize(candidate.standardAnalysis()));double keyword=similarity(tokens(motherStem),tokens(candidate.stem()));
        String reason=null;if(stem>=0.92)reason="STEM_TOO_SIMILAR";else if(stem>=0.82&&keyword>=0.82)reason="ONLY_ENTITY_OR_NUMBER_CHANGED";else if(option>=0.95)reason="OPTIONS_ONLY_REORDERED";else if(analysis>=0.96)reason="ANALYSIS_COPIED";else if(candidate.changedDimensions().size()<2)reason="CHANGED_DIMENSIONS_INSUFFICIENT";else if(candidate.difficulty()>=4&&!candidate.changedDimensions().contains("REASONING_PATH")&&!candidate.changedDimensions().contains("KNOWLEDGE_COMBINATION"))reason="DIFFICULTY_COMPLEXITY_MISMATCH";
        double similarity=Math.max(stem,Math.max(option,keyword));double novelty=Math.max(0,1-similarity);return new Result(novelty,similarity,reason);
    }
    private String normalize(String value){return NUMBERS_UNITS_NAMES.matcher(value==null?"":value.toLowerCase(Locale.ROOT)).replaceAll("").replaceAll("[\\p{Punct}\\s]","");}
    private String tokens(String value){return String.join(" ",new HashSet<>(Arrays.asList(normalize(value).split("(?<=\\G.{2})"))));}
    private double similarity(String a,String b){if(a.isBlank()||b.isBlank())return 0;Set<String>x=grams(a),y=grams(b);Set<String>i=new HashSet<>(x);i.retainAll(y);Set<String>u=new HashSet<>(x);u.addAll(y);return u.isEmpty()?0:(double)i.size()/u.size();}
    private Set<String> grams(String s){Set<String>v=new HashSet<>();if(s.length()<2){v.add(s);return v;}for(int i=0;i<s.length()-1;i++)v.add(s.substring(i,i+2));return v;}
    record Result(double noveltyScore,double similarityScore,String rejectionReason){boolean accepted(){return rejectionReason==null;}}
}
