package com.global.order_api.feature.category;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Category Management", description = "Admin APIs for managing product categories")
public class AdminCategoryController {

    private static final String ENTITY_KEY = "entity.category";

    @Qualifier("adminCategoryService")
    private final AdminCategoryService adminCategoryService;
    private final AppTranslator appTranslator;

    /// GET METHODS (Admin Only)

    @TrackExecutionTime
    @Operation(summary = "Get all categories for admin")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDto>>> getCategoriesPage(
            @Valid @ModelAttribute CategoryFilterRequestDto filter) {
        PageResponse<CategoryResponseDto> pageResponse = adminCategoryService.getCategoriesPage(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<CategoryResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    /// POST METHODS (Admin Only)

    @Operation(summary = "Create a new category")
    @PostMapping("")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
            @Valid @RequestBody CategoryRequestDto request
    ) {
        CategoryResponseDto response = adminCategoryService.createCategory(request);
        String message = appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<CategoryResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /// PUT METHODS (Admin Only)

    @Operation(summary = "Update an existing category")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto request
    ) {
        CategoryResponseDto response = adminCategoryService.updateCategory(request, id);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        ApiResponse<CategoryResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    /// DELETE METHODS (Admin Only)

    @Operation(summary = "Delete a category")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id
    ) {
        adminCategoryService.deleteCategory(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
