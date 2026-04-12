package com.global.order_api.feature.category;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;

import io.swagger.v3.core.jackson.ApiResponsesSerializer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpRequest;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
@Tag(name ="Category Management" ,description = "APIs for managing product categories")

public class CategoryController {
	private final CategoryService categoryService;
	private final AppTranslator appTranslator;
	
	private static final String ENTITY_KEY = "entity.category";
	
	//////////////////////////////////////
	/// READ METHODS
	// GET ALL CATEGORIES 
	@TrackExecutionTime
	@Operation(
			summary = "Get all categories with pagination (Support Search)", 
	        description = "Retrieves a paginated and sortable list of categories. Supports searching by category name."
	        )
	@GetMapping("")
	// Model attributes because GET method doesn't have Body
	public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDto>>> getCategoriesPage(
			@ModelAttribute CategoryFilterRequestDto filter)
	{
		PageResponse<CategoryResponseDto> pageResponse=categoryService.getCategoriesPage(filter);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<CategoryResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse); // 200
	}	
		
	// GET CATEGORY BY ID
	@Operation(
			summary = "Get category by ID", 
			description = "Retrieves the details of a specific category using its unique ID."
		)
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryById(@PathVariable Long id)
	{
		CategoryResponseDto response=categoryService.findCategoryById(id);
		String message=appTranslator.getTranslatedAction("success.retrieved",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
	}
	
	// GET CATEGORY BY NAME
	@Operation(
			summary = "Get category by exact name", 
			description = "Retrieves the details of a specific category using its exact unique name."
		)
	@GetMapping("/name/{name}")
	public ResponseEntity<ApiResponse<CategoryResponseDto>> findCategoryByName(@PathVariable String name)
	{
		CategoryResponseDto response=categoryService.findCategoryByName(name);
		String message=appTranslator.getTranslatedAction("success.retrieved",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
	}
	
	//////////////////////////////
	/////WRITE METHODS
	// CREATE CATEGORY
	// valid => spring check our validation annotations first before go to service
	@Operation(
			summary = "Create a new category", 
			description = "Creates a new category in the system. The category name must be unique."
		)
	@PostMapping("")
	@Transactional
	public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(
			@Valid @RequestBody CategoryRequestDto request
			) {
		CategoryResponseDto response=categoryService.createCategory(request);
		
		String message=appTranslator.getTranslatedAction("success.created",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		
		return new ResponseEntity<>(apiResponse, HttpStatus.CREATED); // 201
	}
	
	//UPDATE CATEGORY
	@Operation(
			summary = "Update an existing category", 
			description = "Updates the details of an existing category by its ID. Validates unique name constraints."
		)
	@PutMapping("/{id}")
	@Transactional
	public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
		@PathVariable Long id,
		@Valid @RequestBody CategoryRequestDto request
		) {
		CategoryResponseDto response=categoryService.updateCategory(request,id);
		
		String message=appTranslator.getTranslatedAction("success.updated",ENTITY_KEY);
		ApiResponse<CategoryResponseDto> apiResponse=ApiResponse.success(response,message);
		
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
	}
	
	/////////////////////////////////////
	/// DELETE METHOD
	//DELETE CATEGORY
	@Operation(
			summary = "Delete a category", 
			description = "Performs a soft delete on a category by its ID."
		)
	@DeleteMapping("/{id}")
	// Void => Generics must take reference types
	// Void means null
	@Transactional
	public ResponseEntity<ApiResponse<Void>> deleteCategory(
		@PathVariable Long id
	)
	{
		categoryService.deleteCategory(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200

	}
	
//	///HARD DELETE METHODS
//	@DeleteMapping("/{id}/force")
//	@Transactional
//	public ResponseEntity<ApiResponse<Void>> forceDeleteCategory(
//		@PathVariable Long id
//	)
//	{
//		categoryService.forceDeleteCategory(id);
//		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
//		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
//		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
//
//	}
}
