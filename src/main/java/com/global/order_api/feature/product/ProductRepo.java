package com.global.order_api.feature.product;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.global.order_api.core.base.BaseRepo;

// JPA specification executor => to build SQL statement step by step for Advanced Search Filters
public interface ProductRepo extends BaseRepo<ProductEntity,Long> ,JpaSpecificationExecutor<ProductEntity>{
	
	/// READ METHODS 
	
//	@Override // because we add EntityGraph
//	@EntityGraph(attributePaths = {"category"})
//	// for better performance using SELECT *
//	// sort by name ,price ,created at
//	Page<ProductEntity> findAll(Pageable pageable);

	//// indexing ////
	/// use product pk = clustered index => very fast
	//////////////////
	/// apply lock
	/// call when user press on submit order to decrease stock count
	/// pessimistic => to lock record and make db make an queue in reading process
	/// write => to create Exclusive Lock
	/// Lock => add FOR UPDATE to my SQL statement depend on DB type
	/// database engine add row-level-lock in ram on this record
	/// so any thread wanna to get this record , db engine block it and add it into waiting queue
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
	Optional<ProductEntity> findByIdForUpdate(@Param("id") Long id);

	/// default findbyid() => for fast reading

	//// indexing ////
	/// use product pk = clustered index => very fast
	//////////////////
	/// for only stock count for better caching strategy
	@Query("SELECT p.stockCount FROM ProductEntity p WHERE p.id = :id")
	Optional<Integer> findStockCountById(@Param("id") Long id);

    //// indexing ////
    /// use product pk = clustered index => very fast
    /// join category pk = clustered index => very fast
    //////////////////
	/// for show only
	/// get full category not using projection
	/// because category table not large
	@EntityGraph(attributePaths = {"category"}) // JOIN operation 
	@Query("SELECT p FROM ProductEntity p WHERE p.id = :id") // because sb can't translate WithCategory
	Optional<ProductEntity> findByIdWithCategory(@Param("id") Long id);
	
	////////////////////////////////////////////////
	// entity graph => when hibernate go to db to execute
	// generated sql , i tell him to get data of category too 
	// to solve n+1 problem
	// without writing sql because using JOIN FETCH may cause performance problems
	//@Query(value = "SELECT p from ProductEntity p JOIN FETCH p.category")
    //// for search bar
    //// indexing ////
    /// use Full text index => very fast for searching words
	/// write native query because jpa will use like method in generated query
    //////////////////
	/// match => to make optimizer to use full text index on name not using like and full table scan
	/// against => take search word and compare it with name
	/// concat => because full text index search only complete word not part of word
	/// so we concat it with * to get rows matches this keyword
	/// boolean mode => to use mathematical operators like *
	@Query(
			value = "SELECT * FROM products WHERE MATCH(name) AGAINST(CONCAT(:keyword, '*') IN BOOLEAN MODE) AND is_deleted = false",
			countQuery = "SELECT count(*) FROM products WHERE MATCH(name) AGAINST(CONCAT(:keyword, '*') IN BOOLEAN MODE) AND is_deleted = false",
			nativeQuery = true
	)
	Page<ProductEntity> searchActiveByNameFullText(@Param("keyword") String keyword,Pageable pageable);

	//// for Admin
	@Query(
			value = "SELECT * FROM products WHERE MATCH(name) AGAINST(CONCAT(:keyword, '*') IN BOOLEAN MODE) AND is_deleted = true",
			countQuery = "SELECT count(*) FROM products WHERE MATCH(name) AGAINST(CONCAT(:keyword, '*') IN BOOLEAN MODE) AND is_deleted = true",
			nativeQuery = true
	)
	Page<ProductEntity> searchDeletedByNameFullText(@Param("keyword") String keyword, Pageable pageable);
	////////////////////////////////////////////////////

    //// indexing ////
    /// use category_id fk = non-clustered index => very fast
    /// KEY LOOKUP
    //////////////////
	@EntityGraph(attributePaths = {"category"})
	Page<ProductEntity> findByCategoryId(Long categoryId,Pageable pageable);
	//////////////////////////////////////////


//	@EntityGraph(attributePaths = {"category"})
//	Optional<ProductEntity> findByName(String name);
	
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
    Optional<String> getImageUrlByIdEvenIfDeleted(@Param("id") Long id);
	@Modifying
    @Query(value = "DELETE FROM products WHERE id = :id", nativeQuery = true)
    void hardDeleteProduct(@Param("id") Long id);
	
	///////////////////////////////////////////
	/// DELETED PRODUCTS
//	//// indexing ////
//	/// use category_id fk = non-clustered index => very fast
//	/// KEY LOOKUP
//	//////////////////
//	@Query(value = "SELECT * FROM products WHERE is_deleted=true",nativeQuery = true)
//	Page<ProductEntity> findAllDeletedProducts(Specification<ProductEntity> spec,Pageable pageable);
	
	@Modifying
	@Query(value = "UPDATE products SET is_deleted=false WHERE id= :id",nativeQuery = true)
	void restoreProduct(@Param("id") Long id);
}
