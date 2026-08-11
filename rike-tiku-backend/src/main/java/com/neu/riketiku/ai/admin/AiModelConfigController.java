package com.neu.riketiku.ai.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai-models")
public class AiModelConfigController {
    private final AiModelConfigService service;
    public AiModelConfigController(AiModelConfigService service){this.service=service;}
    @GetMapping public AiModelConfigDtos.Page list(){return service.list();}
    @PostMapping public AiModelConfigDtos.Item create(@Valid @RequestBody AiModelConfigDtos.Save request){return service.create(request);}
    @PutMapping("/{id}") public AiModelConfigDtos.Item update(@PathVariable long id,@Valid @RequestBody AiModelConfigDtos.Save request){return service.update(id,request);}
    @DeleteMapping("/{id}/api-key") public AiModelConfigDtos.Item clearKey(@PathVariable long id){return service.clearKey(id);}
    @PostMapping("/{id}/test") public AiModelConfigDtos.ConnectionResult test(@PathVariable long id){return service.test(id);}
}
