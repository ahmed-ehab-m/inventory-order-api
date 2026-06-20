package com.global.order_api.feature.product.controller;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.product.specification.ProductFilterRequest;
import com.global.order_api.feature.product.service.ProductService;
import com.global.order_api.feature.product.dto.UserProductResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products Management", description = "Public APIs for viewing products")
public class ProductController {

    private static final String ENTITY_KEY = "entity.product";
    private final ProductService productService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                READ METHODS (Public Access)
    // ==================================================================================

    @TrackExecutionTime
    @Operation(summary = "Get All Products (Paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserProductResponseDto>>> getProductsPage(
            @Valid @ModelAttribute ProductFilterRequest filter) {
        PageResponse<UserProductResponseDto> pageResponse = productService.getProductsPage(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<UserProductResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get Product By ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProductResponseDto>> getProductsByIdWithCategory(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        UserProductResponseDto product = productService.getProductByIdWithCategory(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<UserProductResponseDto> apiResponse = ApiResponse.success(product, message);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get Product Stock Count")
    @GetMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Integer>> getProductStockCount(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        Integer stockCount = productService.getProductStockCount(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", "field.stock");
        ApiResponse<Integer> apiResponse = ApiResponse.success(stockCount, message);
        return ResponseEntity.ok(apiResponse);
    }
}