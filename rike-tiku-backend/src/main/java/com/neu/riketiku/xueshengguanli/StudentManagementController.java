package com.neu.riketiku.xueshengguanli;

import com.neu.riketiku.zhanghao.dto.AdminPasswordRecoveryDtos.BatchPasswordRecoveryRequest;
import com.neu.riketiku.zhanghao.dto.AdminPasswordRecoveryDtos.PasswordRecoveryResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentCreateRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentCreateResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentDetailResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentListResponse;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentTransferRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/students")
public class StudentManagementController {
    private final StudentManagementService service;

    public StudentManagementController(StudentManagementService service) {
        this.service = service;
    }

    @GetMapping
    public StudentListResponse page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String studentNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String accountStatus,
            @RequestParam(required = false) String profileStatus) {
        return service.page(page, size, studentNumber, name, username, classId, grade, accountStatus, profileStatus);
    }

    @GetMapping("/{id}")
    public StudentDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<StudentCreateResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        return noStore(service.create(request));
    }

    @PutMapping("/{id}")
    public StudentDetailResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/transfer")
    public StudentDetailResponse transfer(@PathVariable Long id, @Valid @RequestBody StudentTransferRequest request) {
        return service.transfer(id, request);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<PasswordRecoveryResponse> resetPassword(@PathVariable Long id) {
        return noStore(service.resetPassword(id));
    }

    @PostMapping("/reset-passwords")
    public ResponseEntity<PasswordRecoveryResponse> resetPasswords(
            @Valid @RequestBody BatchPasswordRecoveryRequest request) {
        return noStore(service.resetPasswords(request.ids()));
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(body);
    }
}
