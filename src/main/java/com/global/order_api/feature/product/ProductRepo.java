package com.global.order_api.feature.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.global.order_api.core.base.BaseRepo;

public interface ProductRepo extends BaseRepo<ProductEntity,Long>{
	
	/// READ METHODS 
	
	@Override // because we add EntityGraph
	@EntityGraph(attributePaths = {"category"})
	// for better performance using SELECT * 
	// sort by name ,price ,created at
	Page<ProductEntity> findAll(Pageable pageable);
	
	////////////////////////////////////////////////////
	
	// for show only
	@EntityGraph(attributePaths = {"category"})
	@Query("SELECT p FROM ProductEntity p WHERE p.id = :id") // because sb can't translate WithCategory
	Optional<ProductEntity> findByIdWithCategory(@Param("id") Long id);
	
	////////////////////////////////////////////////
	// entity graph => when hibernate go to to db to execute
	// generated sql , i tell him to get data of category too 
	// to solve n+1 problem
	// without writing sql because using JOIN FETCH may cause performance problems
	//@Query(value = "SELECT p from ProductEntity p JOIN FETCH p.category")
	@EntityGraph(attributePaths = {"category"})
	Page<ProductEntity> findByNameContainingIgnoreCase(String keyword,Pageable pageable);
	
	////////////////////////////////////////////////////
	
	@EntityGraph(attributePaths = {"category"})
	Page<ProductEntity> findByCategoryId(Long categoryId,Pageable pageable);
	//////////////////////////////////////////	
	@EntityGraph(attributePaths = {"category"})
	Optional<ProductEntity> findByName(String name);
	
	////////////////////////////////////////////////////////////
	
	/// WRITE METHODS
	@Modifying // make spring update in db not reading
	// tell hibernate that => data changed so clean your cache
	// tell spring we well retreive number not entity or list of entities
	@Query("UPDATE ProductEntity p SET p.category.id =999 WHERE p.category.id =:oldCategoryId")
	void moveProductsToDefaultCategory(@Param ("oldCategoryId") Long oldCategoryId);
	
	///////////////////////////////////////////////////////////
	////HARD DELETE METHODS
	/// 
	@Query(value = "SELECT image FROM products WHERE id = :id", nativeQuery = true)
    String getImageUrlByIdEvenIfDeleted(@Param("id") Long id);
	@Modifying
    @Query(value = "DELETE FROM products WHERE id = :id", nativeQuery = true)
    void hardDeleteProduct(@Param("id") Long id);
}
