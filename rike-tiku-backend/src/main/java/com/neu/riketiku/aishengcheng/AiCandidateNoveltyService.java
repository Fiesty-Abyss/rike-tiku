package com.neu.riketiku.aishengcheng;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class AiCandidateNoveltyService {
    private static final Pattern NUMBERS_UNITS_NAMES=Pattern.compile("(?iu)[\\d.]+|[a-z]+|千克|克|米|秒|牛|焦|伏|安|欧|摄氏度|小明|小红|甲|乙|丙|苹果|小球");
    Result evaluate(String motherStem,String motherOptions,String motherAnalysis,AiCandidateParser.Candidate candidate){
        double stem=similarity(normalize(motherStem),normalize(candidate.stem()));double option=similarity(normalize(motherOptions),normalize(candidate.options().toString()));double analysis=similarity(normalize(motherAnalysis),normalize(candidate.standardAnalysis()));double keyword=similarity(tokens(motherStem),tokens(candidate.stem()));
        double similarity=Math.max(Math.max(stem,option),Math.max(keyword,analysis));
        String reason=null;
        Decision decision=Decision.ACCEPT;
        if(stem>=0.92) { decision=Decision.REJECT; reason="STEM_TOO_SIMILAR"; }
        else if(stem>=0.82&&keyword>=0.82) { decision=Decision.REJECT; reason="ONLY_ENTITY_OR_NUMBER_CHANGED"; }
        else if(option>=0.95) { decision=Decision.REJECT; reason="OPTIONS_ONLY_REORDERED"; }
        else if(analysis>=0.96) { decision=Decision.REJECT; reason="ANALYSIS_COPIED"; }
        else if(candidate.changedDimensions().size()<2) { decision=Decision.REJECT; reason="CHANGED_DIMENSIONS_INSUFFICIENT"; }
        else if(!modeRequirementsMet(candidate.variationMode(),candidate.changedDimensions())) { decision=Decision.REJECT; reason="VARIATION_DIMENSIONS_INSUFFICIENT"; }
        else if(candidate.difficulty()>=4&&!complexityEvidence(candidate.changedDimensions())) { decision=Decision.REJECT; reason="DIFFICULTY_COMPLEXITY_MISMATCH"; }
        else if(stem>=0.72||keyword>=0.72) { decision=Decision.WARN; reason="NOVELTY_WARN_STEM_OVERLAP"; }
        else if(option>=0.88) { decision=Decision.WARN; reason="NOVELTY_WARN_OPTION_OVERLAP"; }
        else if(analysis>=0.90) { decision=Decision.WARN; reason="NOVELTY_WARN_ANALYSIS_OVERLAP"; }
        double novelty=Math.max(0,1-similarity);return new Result(novelty,similarity,decision,reason);
    }
    private String normalize(String value){return NUMBERS_UNITS_NAMES.matcher(value==null?"":value.toLowerCase(Locale.ROOT)).replaceAll("").replaceAll("[\\p{Punct}\\s]","");}
    private String tokens(String value){return String.join(" ",new HashSet<>(Arrays.asList(normalize(value).split("(?<=\\G.{2})"))));}
    private double similarity(String a,String b){if(a.isBlank()||b.isBlank())return 0;Set<String>x=grams(a),y=grams(b);Set<String>i=new HashSet<>(x);i.retainAll(y);Set<String>u=new HashSet<>(x);u.addAll(y);return u.isEmpty()?0:(double)i.size()/u.size();}
    private Set<String> grams(String s){Set<String>v=new HashSet<>();if(s.length()<2){v.add(s);return v;}for(int i=0;i<s.length()-1;i++)v.add(s.substring(i,i+2));return v;}
    private boolean modeRequirementsMet(String mode,List<String> dimensions){Set<String>d=new HashSet<>(dimensions);return switch(mode){
        case "SCENARIO_TRANSFER"->pair(d,"SCENARIO","DATA")||pair(d,"SCENARIO","CONDITION");
        case "CONDITION_RECOMBINATION"->pair(d,"CONDITION","REASONING_PATH")||pair(d,"CONDITION","DATA");
        case "REPRESENTATION_SWITCH"->pair(d,"REPRESENTATION","REASONING_PATH");
        case "MULTI_STEP_EXTENSION"->pair(d,"REASONING_PATH","CONDITION");
        case "DISTRACTOR_REDESIGN"->pair(d,"DISTRACTOR","REASONING_PATH");
        case "COMBINED"->d.size()>=3;
        default->false;
    };}
    private boolean complexityEvidence(List<String> dimensions){Set<String>d=new HashSet<>(dimensions);return d.contains("REASONING_PATH")||d.contains("KNOWLEDGE_COMBINATION")||pair(d,"CONDITION","DATA")||pair(d,"SCENARIO","CONDITION")||pair(d,"REPRESENTATION","REASONING_PATH")||d.containsAll(Set.of("SCENARIO","DATA","CONDITION"));}
    private boolean pair(Set<String> dimensions,String left,String right){return dimensions.contains(left)&&dimensions.contains(right);}
    enum Decision { ACCEPT, WARN, REJECT }
    record Result(double noveltyScore,double similarityScore,Decision decision,String rejectionReason){
        boolean accepted(){return decision!=Decision.REJECT;}
        boolean warning(){return decision==Decision.WARN;}
        boolean rejected(){return decision==Decision.REJECT;}
    }
}
