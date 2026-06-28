package com.global.order_api.feature.user.controller;

import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.auth.ValidationGroups;
import com.global.order_api.feature.auth.dtos.UserRequestDto;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.service.UserService;
import com.global.order_api.feature.auth.dtos.ChangePasswordRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile Management", description = "APIs for users to manage their own profiles")
public class UserController {

    private static final String ENTITY_KEY = "entity.user";
    private final UserService userService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                1. READ METHODS
    // ==================================================================================

    @GetMapping
    @Operation(summary = "Get My Profile", description = "Retrieves the details of the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserResponseDto userResponseDto = userService.getUserById(userId);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(userResponseDto, message));
    }

    // ==================================================================================
    //                                2. WRITE & UPDATE METHODS
    // ==================================================================================

    @PutMapping
    @Operation(summary = "Update My Profile", description = "Updates the authenticated user's information.")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfile(
            @Validated(ValidationGroups.OnUpdate.class) @RequestBody UserRequestDto updateRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserResponseDto updatedUser = userService.updateUser(userId, updateRequest);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, message));
    }

    @PutMapping("/password")
    @Operation(summary = "Change Password", description = "Allows the user to securely change their password.")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId,request);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ==================================================================================
    //                                3. DELETE METHODS
    // ==================================================================================

    @DeleteMapping
    @Operation(summary = "Deactivate My Account (Soft Delete)", description = "Deactivates the user's account temporarily.")
    public ResponseEntity<ApiResponse<Void>> deactivateMyAccount() {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.softDeleteUser(userId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

//    /// test ip address
//    @GetMapping("/test-ip")
//    public String testIp(HttpServletRequest request) {
//
//        return """
//            remoteAddr = %s
//            x-forwarded-for = %s
//            """.formatted(
//                request.getRemoteAddr(),
//                request.getHeader("X-Forwarded-For")
//        );
//    }
}