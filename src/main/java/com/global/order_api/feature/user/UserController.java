package com.global.order_api.feature.user;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.auth.ValidationGroups;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users Management", description = "APIs for managing users")
public class UserController {

    private static final String ENTITY_KEY = "entity.user";
    private final UserService userService;
    private final AppTranslator appTranslator;

    /// ///////////////////////
    /// READ METHODS
    /// GET ALL USERS
    @TrackExecutionTime
    @GetMapping("")
    @Operation(summary = "Get All Users", description = "Retrieves a paginated list of all active users. Admin only.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDto>>> getUsersPage
    (
            @ModelAttribute UserFilterRequest filter
    ) {
        PageResponse<UserResponseDto> pageResponse = userService.getUsersPage(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<UserResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    /// GET By ID
    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Retrieves user details. User can only view their own profile, Admin can view any.")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id") // 👈 الأدمن، أو اليوزر صاحب الأكونت
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById
    (
            @PathVariable Long id
    ) {
        UserResponseDto userResponseDto = userService.getUserById(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<UserResponseDto> apiResponse = ApiResponse.success(userResponseDto, message);
        return ResponseEntity.ok(apiResponse);
    }

    /// WRITE METHODS
    @PutMapping("/{id}")
    @Operation(summary = "Update User Profile", description = "Updates user information. User can only update their own profile, Admin can update any.")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @Validated(ValidationGroups.OnUpdate.class) @RequestBody UserRequestDto updateRequest) {
        UserResponseDto updatedUser = userService.updateUser(id, updateRequest);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        ApiResponse<UserResponseDto> apiResponse = ApiResponse.success(updatedUser, message);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/soft-delete/{id}")
    @Operation(summary = "Soft Delete User", description = "Deactivates a user account temporarily. User can deactivate their own account, Admin can deactivate any.")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser
            (
                    @PathVariable Long id
            ) {
        userService.softDeleteUser(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/hard-delete/{id}")
    @Operation(summary = "Hard Delete User", description = "Permanently removes a user from the database. Admin only.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser
            (
                    @PathVariable Long id
            ) {
        userService.hardDeleteUser(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/restore-user/{id}")
    @Operation(summary = "Restore User", description = "Restores a softly deleted user account. Admin only.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> restoreUser
            (
                    @PathVariable Long id
            ) {
        userService.restoreUser(id);
        String message = appTranslator.getTranslatedAction("success.restored", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }

}
