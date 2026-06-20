package com.global.order_api.feature.category.controller;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.category.dto.CategoryResponseDto;
import com.global.order_api.feature.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "APIs for managing product categories")
public class CategoryController {

    private static final String ENTITY_KEY = "entity.category";
    private final CategoryService categoryService;
    private final AppTranslator appTranslator;

    /// GET METHODS (Public Access)

    @TrackExecutionTime
    @Operation(summary = "Get all categories (Public)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories() {
        List<CategoryResponseDto> categories = categoryService.findAllCategories();
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<List<CategoryResponseDto>> apiResponse = ApiResponse.success(categories, message);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get category by ID")
    @GetMapping("/{id}")
    @TrackExecutionTime
    public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryById(@PathVariable Long id) {
        CategoryResponseDto response = categoryService.findCategoryById(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<CategoryResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "Get category by exact name")
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryByName(@PathVariable String name) {
        CategoryResponseDto response = categoryService.findCategoryByName(name);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<CategoryResponseDto> apiResponse = ApiResponse.success(response, message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
