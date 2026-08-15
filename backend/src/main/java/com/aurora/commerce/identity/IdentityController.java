package com.aurora.commerce.identity;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class IdentityController {

    private final IdentityService identityService;

    IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/auth/register")
    ApiResponse<IdentityService.AuthResult> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(identityService.register(request.email(), request.password(), request.displayName()));
    }

    @PostMapping("/auth/login")
    ApiResponse<IdentityService.AuthResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(identityService.login(request.email(), request.password()));
    }

    @GetMapping("/me")
    ApiResponse<IdentityService.UserView> me(JwtAuthenticationToken authentication) {
        return ApiResponse.success(identityService.currentUser(authentication.getToken().getSubject()));
    }

    record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName
    ) {
    }

    record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }
}
