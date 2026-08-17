package com.neu.riketiku.guanlicaozuorizhi;

import com.neu.riketiku.guanlicaozuorizhi.dto.GuanLiCaoZuoRiZhiDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
public class GuanLiCaoZuoRiZhiController {
    private final GuanLiCaoZuoRiZhiFuWu service;

    public GuanLiCaoZuoRiZhiController(GuanLiCaoZuoRiZhiFuWu service) {
        this.service = service;
    }

    @GetMapping
    public GuanLiCaoZuoRiZhiDtos.Page page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) Long objectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue="DESC") String sort) {
        return service.page(page,size,module,action,result,operatorId,objectId,keyword,start,end,sort);
    }

    @GetMapping("/{id}")
    public GuanLiCaoZuoRiZhiDtos.Item detail(@org.springframework.web.bind.annotation.PathVariable long id){return service.detail(id);}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @GetMapping(value="/export.csv",produces="text/csv")
    public ResponseEntity<byte[]> export(@RequestParam(required=false)String module,@RequestParam(required=false)String action,
            @RequestParam(required=false)String result,@RequestParam(required=false)Long operatorId,@RequestParam(required=false)Long objectId,
            @RequestParam(required=false)String keyword,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime start,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)LocalDateTime end){byte[] body=("\uFEFF"+service.csv(module,action,result,operatorId,objectId,keyword,start,end)).getBytes(StandardCharsets.UTF_8);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=operation-logs.csv").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(body);}
}
