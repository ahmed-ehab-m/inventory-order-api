package com.global.order_api.feature.category;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.global.order_api.core.base.BaseRepo;

@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, Long> {
	
	// check if this category existed before insert
	// better performance
	boolean	existsByName(String name);
	
	// to find by direct name only for backend logic and validation
	Optional<CategoryEntity> findByName(String name);
	
	// for search generally (front-end-search-box)
	// collections can return null 
	List<CategoryEntity> findByNameContainingIgnoreCase(String keyword);
}
