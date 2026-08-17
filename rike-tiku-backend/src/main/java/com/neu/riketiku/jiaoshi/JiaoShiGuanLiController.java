package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.jiaoshi.dto.*;
import com.neu.riketiku.zhanghao.dto.AdminPasswordRecoveryDtos.BatchPasswordRecoveryRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class JiaoShiGuanLiController {
    private final JiaoShiGuanLiFuWu service;
    public JiaoShiGuanLiController(JiaoShiGuanLiFuWu service) { this.service = service; }
    @GetMapping("/teachers") public JiaoShiFenYeXiangYing page(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="10") @Min(1) @Max(100) long size,@RequestParam(required=false) String employeeNumber,@RequestParam(required=false) String name,@RequestParam(required=false) String username,@RequestParam(required=false) String accountStatus,@RequestParam(required=false) String profileStatus){return service.page(page,size,employeeNumber,name,username,accountStatus,profileStatus);}
    @GetMapping("/teachers/{id}") public JiaoShiXiangQingXiangYing get(@PathVariable Long id){return service.get(id);}
    @PostMapping("/teachers") public ResponseEntity<JiaoShiChuangJianXiangYing> create(@Valid @RequestBody JiaoShiChuangJianQingQiu request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma","no-cache").body(service.create(request));}
    @PutMapping("/teachers/{id}") public JiaoShiXiangYing update(@PathVariable Long id,@Valid @RequestBody JiaoShiXiuGaiQingQiu request){return service.update(id,request);}
    @PostMapping("/teachers/{id}/reset-password") public ResponseEntity<JiaoShiMiMaChongZhiXiangYing> resetPassword(@PathVariable Long id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma","no-cache").body(service.resetPassword(id));}
    @PostMapping("/teachers/reset-passwords") public ResponseEntity<JiaoShiMiMaChongZhiXiangYing> resetPasswords(@Valid @RequestBody BatchPasswordRecoveryRequest request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma","no-cache").body(service.resetPasswords(request.ids()));}
    @GetMapping("/subjects") public List<KeMuXiangYing> subjects(){return service.subjects();}
    @GetMapping("/teachers/{teacherId}/teaching-assignments") public List<RenKeXiangYing> assignments(@PathVariable Long teacherId){return service.assignments(teacherId);}
    @PostMapping("/teachers/{teacherId}/teaching-assignments") public RenKeXiangYing createAssignment(@PathVariable Long teacherId,@Valid @RequestBody RenKeChuangJianQingQiu request){return service.createAssignment(teacherId,request);}
    @PatchMapping("/teaching-assignments/{id}/status") public RenKeXiangYing changeStatus(@PathVariable Long id,@Valid @RequestBody RenKeZhuangTaiQingQiu request){return service.changeAssignmentStatus(id,request);}
    @PostMapping("/teachers/{teacherId}/admin-role") public JiaoShiXiangQingXiangYing grantAdmin(@PathVariable Long teacherId,@AuthenticationPrincipal RenZhengYongHu user){return service.grantAdmin(teacherId,user.id());}
    @DeleteMapping("/teachers/{teacherId}/admin-role") public JiaoShiXiangQingXiangYing revokeAdmin(@PathVariable Long teacherId,@AuthenticationPrincipal RenZhengYongHu user){return service.revokeAdmin(teacherId,user.id());}
}
