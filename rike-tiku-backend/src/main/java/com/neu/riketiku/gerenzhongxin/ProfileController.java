package com.neu.riketiku.gerenzhongxin;

import com.neu.riketiku.gerenzhongxin.ProfileDtos.AvatarResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.ProfileResponse;
import com.neu.riketiku.gerenzhongxin.ProfileDtos.ProfileUpdateRequest;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    ProfileResponse getProfile(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.getProfile(principal.id());
    }

    @PutMapping
    ProfileResponse updateProfile(
            @AuthenticationPrincipal RenZhengYongHu principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return service.updateIntroduction(principal.id(), request.introduction());
    }

    @PostMapping("/avatar")
    AvatarResponse uploadAvatar(
            @AuthenticationPrincipal RenZhengYongHu principal,
            @RequestPart("file") MultipartFile file) {
        return service.uploadAvatar(principal.id(), file);
    }

    @DeleteMapping("/avatar")
    AvatarResponse deleteAvatar(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.deleteAvatar(principal.id());
    }
}
