package com.global.order_api.feature.user;

import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Management",description = "APIs for managing Authentication")
public class AuthController
{
    private final UserService userService;
    private  final AppTranslator appTranslator;
    private static  final String ENTITY_KEY="entity.user";

    /// Auth METHODS
    @PostMapping("/register")
    @Operation(summary = "Register New User", description = "Creates a new user account and returns a JWT token. Public access.")    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@RequestBody UserRequestDto user)
    {
        AuthResponseDto authResponseDto=userService.register(user);
        String message=appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<AuthResponseDto> apiResponse=ApiResponse.success(authResponseDto,message);
        return  ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user by email and password, and returns a JWT token. Public access.")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@RequestBody UserRequestDto user)
    {
        AuthResponseDto authResponseDto=userService.login(user);
        String message=appTranslator.translateMessage("success.login");
        ApiResponse<AuthResponseDto> apiResponse=ApiResponse.success(authResponseDto,message);
        return  ResponseEntity.ok(apiResponse);
    }
}
