package com.global.order_api.feature.product.controller;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.product.dto.AdminProductResponseDto;
import com.global.order_api.feature.product.specification.ProductFilterRequest;
import com.global.order_api.feature.product.dto.ProductRequestDto;
import com.global.order_api.feature.product.dto.UserProductResponseDto;
import com.global.order_api.feature.product.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Products Management", description = "Admin APIs for managing products")
public class AdminProductController {

    private static final String ENTITY_KEY = "entity.product";
    private final AdminProductService adminProductService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                READ METHODS (ADMIN)
    // ==================================================================================

    @TrackExecutionTime
    @Operation(summary = "Get All Products For Admin")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminProductResponseDto>>> getAdminProductsPage(
            @Valid @ModelAttribute ProductFilterRequest filter) {
        PageResponse<AdminProductResponseDto> pageResponse = adminProductService.getAdminProductsPage(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<AdminProductResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get Soft Deleted Products")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PageResponse<UserProductResponseDto>>> getDeletedProducts(
            @Valid @ModelAttribute ProductFilterRequest filter) {
        PageResponse<UserProductResponseDto> pageResponse = adminProductService.getDeletedProducts(filter);
        String message = appTranslator.getTranslatedAction("success.deleted_retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<UserProductResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    // ==================================================================================
    //                                WRITE METHODS (ADMIN)
    // ==================================================================================

    @Operation(summary = "Create a New Product")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProductResponseDto>> createProduct(
            @Parameter(description = "Product Details (JSON format)") @Valid @RequestPart("data") ProductRequestDto request,
            @Parameter(description = "Product Image File") @RequestPart(value = "image", required = false) MultipartFile image) {
        UserProductResponseDto response = adminProductService.createProduct(request, image);
        String message = appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<UserProductResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an Existing Product")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProductResponseDto>> updateProduct(
            @Parameter(description = "Product ID to update") @PathVariable Long id,
            @Parameter(description = "Updated Product Details (JSON)") @Valid @RequestPart("data") ProductRequestDto request,
            @Parameter(description = "New Image File (Optional)") @RequestPart(value = "image", required = false) MultipartFile image) {
        UserProductResponseDto response = adminProductService.updateProduct(request, id, image);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        ApiResponse<UserProductResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    // ==================================================================================
    //                                DELETE & RESTORE METHODS (ADMIN)
    // ==================================================================================

    @Operation(summary = "Soft Delete Product")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID to soft-delete") @PathVariable Long id) {
        adminProductService.deleteProduct(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "Hard Delete Product (DANGER)")
    @DeleteMapping("/{id}/force")
    public ResponseEntity<ApiResponse<Void>> forceDeleteProduct(
            @Parameter(description = "Product ID to hard-delete") @PathVariable Long id) {
        adminProductService.forceDeleteProduct(id);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "Restore Deleted Product")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreProduct(
            @Parameter(description = "Product ID to restore") @PathVariable Long id) {
        adminProductService.restoreProduct(id);
        String message = appTranslator.getTranslatedAction("success.restored", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }
}