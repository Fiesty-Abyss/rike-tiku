package com.neu.riketiku.zhishikapian;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class KnowledgeCardDtos {
    private KnowledgeCardDtos() {}
    public record Save(@NotBlank String type,@NotBlank @Size(max=200)String title,@NotEmpty List<@NotNull Long> knowledgePointIds,
                       @NotBlank String content,String latex,String applicableConditions,String derivation,String commonMistake,
                       String example,@Size(max=500)String mnemonic,@Size(max=255)String sourceName,@Size(max=1000)String sourceUrl,
                       @NotBlank String rightsStatus,@Min(0)@Max(100000)int sortOrder,boolean aiDraft){}
    public record State(boolean favorite,@NotBlank String mastery){}
    public record Attachment(Long id,String name,String mime,long size,String contentUrl){}
    public record Card(Long id,Long subjectId,String subjectCode,String subjectName,Long teachingScopeId,String className,
                       String type,String title,List<Point> knowledgePoints,String content,String latex,String applicableConditions,
                       String derivation,String commonMistake,String example,String mnemonic,String sourceName,String sourceUrl,
                       String rightsStatus,String status,int sortOrder,boolean favorite,String mastery,List<Attachment> attachments){}
    public record Point(Long id,String name,String path){}
    public record Frequency(String window,long occurrences,long paperCount,String sampleNotice){}
    public record Review(@NotBlank String action,@Size(max=1000)String comment){}
}
