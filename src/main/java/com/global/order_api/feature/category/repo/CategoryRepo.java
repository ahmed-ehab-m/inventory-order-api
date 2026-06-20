package com.global.order_api.feature.category.repo;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.feature.category.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, Long> {

    ///////////////////// READ METHODS //////////////////////////
    ////////////////////////////////////////////////////////////
    /// check if this category existed before insert
    /// catch exception early and provide simple message to front-end
    /// for internal logic
    ///
    /// better performance => short circuit:
    /// SELECT 1 FROM category WHERE name = 'Electronics' LIMIT 1;
    ///
    /// if DB found one record include this name then stop search and return true
    /// hibernate take 1 or 0 from DB so he doesn't create object (save resources)
    /// save Network BandWidth because DB send only one bit 0 or 1, not full record
    boolean existsByName(String name);

    /// to find by direct name only for backend logic and validation during insert
    /// better performance using DB indexes
    Optional<CategoryEntity> findByName(String name);

    /// for search generally (front-end-search-box)
    /// collections can return null
    ///
    /// Pagination
    /// Page => my data + meta data about the data in the table
    /// Pageable => holds = pageNumber, pageSize, sort(option)
    Page<CategoryEntity> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    // // HARD DELETE METHODS
    // @Modifying
    // @Query(value = "DELETE FROM categories WHERE id = :id", nativeQuery = true)
    // void hardDeleteCategory(@Param("id") Long id);
}
