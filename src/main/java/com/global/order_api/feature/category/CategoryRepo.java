package com.global.order_api.feature.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.global.order_api.core.base.BaseRepo;

@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, Long> {
	
	// READ METHODS
	// check if this category existed before insert
	// catch exception early and provide simple message to front-end
	// better performance
	boolean	existsByName(String name);
	
	// to find by direct name only for backend logic and validation during insert 
	// better performance using DB indexes
	Optional<CategoryEntity> findByName(String name);
	
	// for search generally (front-end-search-box)
	// collections can return null 
	// Pagination
	// Page => my data + meta data about the data in the table
	// Pageable =>holds = pageNumber ,pageSize , sort(option)
	Page<CategoryEntity> findByNameContainingIgnoreCase(String keyword,Pageable pageable);
}
