package com.global.order_api.feature.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	/// READ METHODS (Public Access)

	//// for user
	@TrackExecutionTime
	@Operation(summary = "Get All Products (Paginated)")
	@GetMapping("")
	public ResponseEntity<ApiResponse<PageResponse<UserProductResponseDto>>> getProductsPage(
			@Valid  @ModelAttribute ProductFilterRequest filter)
	{
		PageResponse<UserProductResponseDto> pageResponse=productService.getProductsPage(filter);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<UserProductResponseDto>> apiResponse=ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse);
	}

	//// for Admin
	@TrackExecutionTime
	@Operation(summary = "Get All Products For Admin")
	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PageResponse<AdminProductResponseDto>>> getAdminProductsPage(
			@Valid @ModelAttribute ProductFilterRequest filter)
	{
		PageResponse<AdminProductResponseDto> pageResponse=productService.getAdminProductsPage(filter);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<AdminProductResponseDto>> apiResponse=ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get Product By ID")
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<UserProductResponseDto>> getProductsByIdWithCategory(
			@Parameter(description = "Product ID") @PathVariable Long id)
	{
		UserProductResponseDto product=productService.getProductByIdWithCategory(id);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<UserProductResponseDto> apiResponse=ApiResponse.success(product, message);
		return ResponseEntity.ok(apiResponse);
	}

	@Operation(summary = "Get Product By Name")
	@GetMapping("/name/{name}")
	public ResponseEntity<ApiResponse<UserProductResponseDto>> getProductByName(
			@Parameter(description = "Exact product name") @PathVariable String name)
	{
		UserProductResponseDto productDto=productService.getProductByName(name);
		String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
		ApiResponse<UserProductResponseDto> apiResponse=ApiResponse.success(productDto, message);
		return ResponseEntity.ok(apiResponse);
	}
	////////////////////
	@Operation(summary = "Get Product Stock Count")
	@GetMapping("/{id}/stock")
	public ResponseEntity<ApiResponse<Integer>> getProductStockCount(
			@Parameter(description = "Product ID") @PathVariable Long id)
	{
		Integer stockCount = productService.getProductStockCount(id);

		String message = appTranslator.getTranslatedAction("success.retrieved", "field.stock");

		ApiResponse<Integer> apiResponse = ApiResponse.success(stockCount, message);

		return ResponseEntity.ok(apiResponse);
	}

	/////////////////
	/// GET SOFT DELETED PRODUCTS (ADMIN ONLY)
	@Operation(summary = "Get Soft Deleted Products")
	@GetMapping("/deletedProducts")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PageResponse<UserProductResponseDto>>> getDeletedProducts(
			@ModelAttribute ProductFilterRequest filter)
	{
		PageResponse<UserProductResponseDto> pageResponse=productService.getDeletedProducts(filter);
		String message = appTranslator.getTranslatedAction("success.deleted_retrieved", ENTITY_KEY);
		ApiResponse<PageResponse<UserProductResponseDto>> apiResponse=ApiResponse.success(pageResponse, message);
		return ResponseEntity.ok(apiResponse);
	}

	/////////////////////////////////////
	/// WRITE METHODS (ADMIN ONLY)

	@Operation(summary = "Create a New Product")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	/// @RequestPart => Multi part/form-data => request is separate to
	/// part holds image
	/// part holds json "data"
	public ResponseEntity<ApiResponse<UserProductResponseDto>> createProduct(
			@Parameter(description = "Product Details (JSON format)") @Valid @RequestPart("data") ProductRequestDto request ,
			@Parameter(description = "Product Image File") @RequestPart(value = "image",required = false) MultipartFile image
	) {
		UserProductResponseDto response=productService.createProduct(request,image);
		String message=appTranslator.getTranslatedAction("success.created",ENTITY_KEY);
		ApiResponse<UserProductResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
	}

	@Operation(summary = "Update an Existing Product")
	@PutMapping(value="/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserProductResponseDto>> updateProduct(
			@Parameter(description = "Product ID to update") @PathVariable Long id,
			@Parameter(description = "Updated Product Details (JSON)") @Valid @RequestPart("data") ProductRequestDto request ,
			@Parameter(description = "New Image File (Optional)") @RequestPart(value = "image",required = false) MultipartFile image
	) {
		UserProductResponseDto response=productService.updateProduct(request,id,image);
		String message=appTranslator.getTranslatedAction("success.updated",ENTITY_KEY);
		ApiResponse<UserProductResponseDto> apiResponse=ApiResponse.success(response,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	/////////////////////////////////
	/// DELETE METHODS (ADMIN ONLY)

	@Operation(summary = "Soft Delete Product")
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteProduct(
			@Parameter(description = "Product ID to soft-delete") @PathVariable Long id
	)
	{
		productService.deleteProduct(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	@Operation(summary = "Hard Delete Product (DANGER)")
	@DeleteMapping("/{id}/force")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> forceDeleteProductCategory(
			@Parameter(description = "Product ID to hard-delete") @PathVariable Long id
	)
	{
		productService.forceDeleteProduct(id);
		String message=appTranslator.getTranslatedAction("success.deleted",ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}

	@Operation(summary = "Restore Deleted Product")
	@PutMapping("/{id}/restore")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> restoreProduct(
			@Parameter(description = "Product ID to restore") @PathVariable Long id)
	{
		productService.restoreProduct(id);
		String message = appTranslator.getTranslatedAction("success.restored", ENTITY_KEY);
		ApiResponse<Void> apiResponse=ApiResponse.success(null, message);
		return ResponseEntity.ok(apiResponse);
	}
}