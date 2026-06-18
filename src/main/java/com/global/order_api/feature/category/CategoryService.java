package com.global.order_api.feature.category;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CategoryService extends BaseService<CategoryEntity, Long> {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;

    public CategoryService(BaseRepo<CategoryEntity, Long> baseRepo, CategoryRepo categoryRepo,
                           CategoryMapper categoryMapper) {
        super(baseRepo);
        this.categoryRepo = categoryRepo;
        this.categoryMapper = categoryMapper;
    }

    /// ////////////////////////
    /// CACHING
    ///
    /// CACHE TYPE => READ-HEAVY, WRITE-RARE
    /// CACHE STRATEGY => READING = CACHE-ASIDE || WRITING = WRITE-AROUND
    ///
    /// WRITE-AROUND:
    /// no need to write in DB and Redis in the same time
    /// rare editing on categories
    /// simple and save resources
    ///
    /// CACHE-ASIDE:
    /// resistance even if Redis fail
    /// simple
    /// on-demand loading or lazy loading
    /// store only what users needed, high traffic to save resources
    /// ////////////////////////
    ///
    /// GET METHODS

    /// USER GET ALL METHOD
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponseDto> findAllCategories() {
        List<CategoryResponseDto> categories = categoryMapper.mapToDtoList(categoryRepo.findAll());
        return categories;
    }

    /// caching here for Business Logic
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponseDto findCategoryById(Long id) {
        /// avoid + to avoid String concatenation for better performance
        log.debug("go to data base not cache to get category id : {}", id);
        CategoryEntity entity = categoryRepo.findByIdOrThrow(id);
        return categoryMapper.mapToDto(entity);
    }

    /// to get the identical name not containing
    /// to enhance SEO or if reading from Excel sheet so i read names not IDs
    /// or Third-Party Integrations Like Odoo or SAP because they may send requests
    /// using name not IDs
    /// caching here for Business Logic
    @Cacheable(value = "categories", key = "#name")
    public CategoryResponseDto findCategoryByName(String name) {
        CategoryEntity entity = categoryRepo.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", name));
        return categoryMapper.mapToDto(entity);
    }
}
