package com.neu.riketiku.sixin;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.sixin.dto.SiXinDtos.ContactResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.ConversationCreateRequest;
import com.neu.riketiku.sixin.dto.SiXinDtos.ConversationResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessagePageResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessageResponse;
import com.neu.riketiku.sixin.dto.SiXinDtos.MessageSendRequest;
import com.neu.riketiku.sixin.dto.SiXinDtos.ReadResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class SiXinController {
    private final SiXinFuWu service;

    public SiXinController(SiXinFuWu service) {
        this.service = service;
    }

    @GetMapping("/contacts")
    List<ContactResponse> contacts(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.contacts(principal);
    }

    @GetMapping("/conversations")
    List<ConversationResponse> conversations(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.conversations(principal);
    }

    @PostMapping("/conversations")
    ConversationResponse create(@Valid @RequestBody ConversationCreateRequest request,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.create(principal, request);
    }

    @GetMapping("/conversations/{id}/messages")
    MessagePageResponse messages(@PathVariable long id, @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.messages(principal, id);
    }

    @PostMapping("/conversations/{id}/messages")
    MessageResponse send(@PathVariable long id, @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.send(principal, id, request);
    }

    @PostMapping("/conversations/{id}/read")
    ReadResponse read(@PathVariable long id, @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.read(principal, id);
    }
}
