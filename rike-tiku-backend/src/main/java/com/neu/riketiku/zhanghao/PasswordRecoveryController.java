package com.neu.riketiku.zhanghao;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;
    public PasswordRecoveryController(PasswordRecoveryService service){this.service=service;}
    @PostMapping("/api/v1/auth/password-recovery-requests")
    ResponseEntity<PasswordRecoveryDtos.Accepted> request(@Valid @RequestBody PasswordRecoveryDtos.Request request){return noStore(service.request(request));}
    @GetMapping("/api/v1/admin/password-recovery-requests")
    ResponseEntity<PasswordRecoveryDtos.Page> list(){return noStore(service.list());}
    @PostMapping("/api/v1/admin/password-recovery-requests/{id}/resolve")
    ResponseEntity<PasswordRecoveryDtos.Resolution> resolve(@PathVariable long id,@AuthenticationPrincipal RenZhengYongHu user){return noStore(service.resolve(id,user.id()));}
    @PostMapping("/api/v1/admin/password-recovery-requests/{id}/reject")
    ResponseEntity<PasswordRecoveryDtos.Resolution> reject(@PathVariable long id,@AuthenticationPrincipal RenZhengYongHu user,@Valid @RequestBody PasswordRecoveryDtos.Reject request){return noStore(service.reject(id,user.id(),request.reason()));}
    private <T> ResponseEntity<T> noStore(T body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
