package com.global.order_api.feature.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
@Tag(name ="Products Management" ,description = "APIs for managing products")
public class ProductController {

	private final ProductService productService;
	private final AppTranslator appTranslator;
	
	private static final String ENTITY_KEY = "entity.product";

	
	//////////////////////////////////////
	/// READ METHODS
	// GET ALL Products
	@TrackExecutionTime
	@Operation(summary = "Get All Products (Paginated)", 
	description = "Retrieves a paginated list of products with optional search and sorting.")
	@GetMapping("")
	public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> getProductsPage(
			@ModelAttribute ProductFilterRequest filter)
	{
		PageResponse<ProductResponseDto> pageResponse=productService.getProductsPage(filter);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<ProductResponseDto>> apiResponse=ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse); // 200
	}
	///////////////////////////////////
	// GET BY PRODUCT ID WITH CATEGROY DETAILS
	@Operation(summary = "Get Products By Product Id With Category",
			description = "Retrieves a paginated list of products belonging to a specific category.")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponseDto>> getProductsByIdWithCategory(
			@Parameter(description = "Product ID") @PathVariable Long id)
	{
		ProductResponseDto product=productService.getProductByIdWithCategory(id);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<ProductResponseDto> apiResponse=ApiResponse.success(product, message);
		return ResponseEntity.ok(apiResponse); // 200
	}
	
	///////////////////////////////////
	// GET BY NAME
	@Operation(summary = "Get Product By Name",
			description = "Retrieves a single product exactly matching the given name.")
	@GetMapping("/name/{name}")
	public ResponseEntity<ApiResponse<ProductResponseDto>> getProductByName(
			@Parameter(description = "Exact product name") @PathVariable String name)
	{
		ProductResponseDto productDto=productService.getProductByName(name);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<ProductResponseDto> apiResponse=ApiResponse.success(productDto, message);
		return ResponseEntity.ok(apiResponse); // 200
	}
	/////////////////////////////////////
	/// WRITE METHODS
	/// CREATE PRODUCT
	/// write it for 1=> swagger ui
	/// 2=> fail fast if user send application/json not form-data
	/// 3=> clean code
	@Operation(summary = "Create a New Product",
	description = "Creates a product. Expects data as JSON and an optional image file (multipart/form-data).")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
			@Parameter(description = "Product Details (JSON format)") @Valid @RequestPart("data") ProductRequestDto request ,
			@Parameter(description = "Product Image File") @RequestPart(value = "image",required = false) MultipartFile image
			) {
		ProductResponseDto response=productService.createProduct(request,image);
		
		String message=appTranslator.getTranslatedAction("success.created",ENTITY_KEY);
		ApiResponse<ProductResponseDto> apiResponse=ApiResponse.success(response,message);
		
		return new ResponseEntity<>(apiResponse, HttpStatus.CREATED); // 201
	}
	///////////////////////////////////////
	/// UPDATE PRODUCT
	@Operation(summary = "Update an Existing Product", 
			description = "Updates product details and optionally replaces its image. Overwrites the old image in Cloudinary if a new one is provided.")
	@PutMapping(value="/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
			@Parameter(description = "Product ID to update") @PathVariable Long id,
			@Parameter(description = "Updated Product Details (JSON)") @Valid @RequestPart("data") ProductRequestDto request ,
			@Parameter(description = "New Image File (Optional)") @RequestPart(value = "image",required = false) MultipartFile image
			) {
		ProductResponseDto response=productService.updateProduct(request,id,image);
		
		String message=appTranslator.getTranslatedAction("success.updated",ENTITY_KEY);
		ApiResponse<ProductResponseDto> apiResponse=ApiResponse.success(response,message);
		
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
	}
	/////////////////////////////////
	/// DELETE METHODS
	/// SOFT DELETE
	@Operation(summary = "Soft Delete Product",
			description = "Performs a soft delete on the product (marks as deleted in DB, keeps image in Cloudinary).")
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteProduct(
			@Parameter(description = "Product ID to soft-delete") @PathVariable Long id
	)
	{
		productService.deleteProduct(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
	}
	///HARD DELETE METHODS
	@Operation(summary = "Hard Delete Product (DANGER)",
			description = "Permanently deletes the product from the database AND removes its image from Cloudinary.")
	@DeleteMapping("/{id}/force")
	@Transactional
	public ResponseEntity<ApiResponse<Void>> forceDeleteProductCategory(
			@Parameter(description = "Product ID to hard-delete") @PathVariable Long id
	)
	{
		productService.forceDeleteProduct(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK); // 200
		}
}
