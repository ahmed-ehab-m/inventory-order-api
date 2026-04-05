package com.global.order_api.feature.category;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
	private final CategoryService categoryService;
	
	// CREATE CATEGORY
	// valid => spring check our validation annotations first before go to service
	@PostMapping("")
	public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto request) {
		CategoryResponseDto response=categoryService.createCategory(request);
		
		return new ResponseEntity<>(response,HttpStatus.CREATED); // 201
	}	
	
	
	
}
