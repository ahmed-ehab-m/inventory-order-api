package com.global.order_api.feature.user.controller;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.user.service.AdminUserService;
import com.global.order_api.feature.user.specification.UserFilterRequest;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users Management", description = "APIs for administrators to manage users")
public class AdminUserController {

    private static final String ENTITY_KEY = "entity.user";
    private final AdminUserService adminUserService;
    private final UserService userService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                1. READ METHODS
    // ==================================================================================

    @TrackExecutionTime
    @GetMapping
    @Operation(summary = "Get All Users", description = "Retrieves a paginated list of all users.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getUsersPage(
            @ModelAttribute UserFilterRequest filter) {
        PageResponse<UserResponseDto> pageResponse = adminUserService.getUsersPage(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(pageResponse, message));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Retrieves user details by their ID.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(
            @PathVariable Long id) {
        UserResponseDto userResponseDto = userService.getUserById(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(userResponseDto, message));
    }

    // ==================================================================================
    //                                2. DELETE & RESTORE METHODS
    // ==================================================================================

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft Delete User", description = "Deactivates a user account temporarily.")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(
            @PathVariable Long id) {
        userService.softDeleteUser(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @DeleteMapping("/{id}/force")
    @Operation(summary = "Hard Delete User", description = "Permanently removes a user from the database. Cannot be undone.")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(
            @PathVariable Long id) {
        adminUserService.hardDeleteUser(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore User", description = "Restores a softly deleted user account.")
    public ResponseEntity<ApiResponse<Void>> restoreUser(
            @PathVariable Long id) {
        adminUserService.restoreUser(id);
        String message = appTranslator.getTranslatedAction("success.restored", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ==================================================================================
    //                                3. UPDATE METHODS
    // ==================================================================================

    @PutMapping("/{id}/promote")
    @Operation(summary = "Promote User to Admin", description = "Promotes a regular user to an administrator role.")
    public ResponseEntity<ApiResponse<Void>> promoteUserToAdmin(
            @PathVariable Long id) {
        adminUserService.promoteUserToAdmin(id);

        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);

        return ResponseEntity.ok(ApiResponse.success(null, message));
    }
}