package com.global.order_api.feature.category;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
@Tag(name ="Category Management" ,description = "APIs for managing product categories")
public class CategoryController {

	private final CategoryService categoryService;
	private final AppTranslator appTranslator;

	private static final String ENTITY_KEY = "entity.category";

	//////////////////////////////////////
	/// READ METHODS (Public Access)
	@TrackExecutionTime
	@Operation(summary = "Get all categories (Public)")
	@GetMapping("")
	public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategories()
	{
		List<CategoryResponseDto> categories=categoryService.findAllCategories();
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<List<CategoryResponseDto>> apiResponse = ApiResponse.success(categories, message);
		return ResponseEntity.ok(apiResponse);
	}


	@TrackExecutionTime
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Get all categories for admin")
	@GetMapping("/page")
	public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDto>>> getCategoriesPage(
			@Valid @ModelAttribute CategoryFilterRequestDto filter)
	{
		PageResponse<CategoryResponseDto> pageResponse=categoryService.getCategoriesPage(filter);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<CategoryResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get category by ID")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryById(@PathVariable Long id)
	{
		CategoryResponseDto response=categoryService.findCategoryById(id);
		String message=appTranslator.getTranslatedAction("success.retrieved",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	@Operation(summary = "Get category by exact name")
	@GetMapping("/name/{name}")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryByName(@PathVariable String name)
	{
		CategoryResponseDto response=categoryService.findCategoryByName(name);
		String message=appTranslator.getTranslatedAction("success.retrieved",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	//////////////////////////////
	///// WRITE METHODS (ADMIN ONLY)

	@Operation(summary = "Create a new category")
	@PostMapping("")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
			@Valid @RequestBody CategoryRequestDto request
	) {
		CategoryResponseDto response=categoryService.createCategory(request);
		String message=appTranslator.getTranslatedAction("success.created",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
	}

	@Operation(summary = "Update an existing category")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
			@PathVariable Long id,
			@Valid @RequestBody CategoryRequestDto request
	) {
		CategoryResponseDto response=categoryService.updateCategory(request,id);
		String message=appTranslator.getTranslatedAction("success.updated",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	/////////////////////////////////////
	/// DELETE METHOD (ADMIN ONLY)

	@Operation(summary = "Delete a category")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteCategory(
			@PathVariable Long id
	)
	{
		categoryService.deleteCategory(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}
}