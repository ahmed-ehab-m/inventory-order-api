package com.global.order_api.feature.category.service;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.feature.category.entity.CategoryEntity;
import com.global.order_api.feature.category.dto.CategoryResponseDto;
import com.global.order_api.feature.category.dto.CategoryFilterRequestDto;
import com.global.order_api.feature.category.dto.CategoryRequestDto;
import com.global.order_api.feature.category.mapper.CategoryMapper;
import com.global.order_api.feature.category.repo.CategoryRepo;
import com.global.order_api.feature.product.repo.ProductRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCategoryService extends BaseService<CategoryEntity, Long> {

    /// to save our memory
    private static final Long DEFAULT_CATEGORY_ID = 999L;

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final ProductRepo productRepo;

    public AdminCategoryService(BaseRepo<CategoryEntity, Long> baseRepo, CategoryRepo categoryRepo,
                                CategoryMapper categoryMapper, ProductRepo productRepo) {
        super(baseRepo);
        this.categoryRepo = categoryRepo;
        this.categoryMapper = categoryMapper;
        this.productRepo = productRepo;
    }

    /// GET METHODS

    /// ADMIN METHOD
    /// smart method for pagination
    /// take filter => smart object contains page number, size, sort type, keyword
    /// return pageResponse we created in core folder
    /// for Advanced Search Bar and Filters
    /// SpEL (Spring Expression Language)
    public PageResponse<CategoryResponseDto> getCategoriesPage(CategoryFilterRequestDto filter) {
        // take user input => "ASC" OR "DESC" from headers
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());

        // Pageable => take all user input
        // will be translated to SQL
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // holds data + meta data about it
        Page<CategoryEntity> categoryPage;
        if (filter.getSearchKeyword() != null && !filter.getSearchKeyword().isBlank()) {
            categoryPage = categoryRepo.findByNameContainingIgnoreCase(filter.getSearchKeyword(), pageable);
        } else {
            // if user want all categories
            categoryPage = categoryRepo.findAll(pageable);
        }

        List<CategoryResponseDto> dtoList = categoryMapper.mapToDtoList(categoryPage.getContent());
        return PageResponse.from(categoryPage, dtoList);
    }

    /// POST METHODS

    /// remove all page from cache
    /// editing methods here, very minor or low editing here so remove all
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        // first check unique name
        if (categoryRepo.existsByName(requestDto.getName())) {
            throw new DuplicateRecordException("Category", "name", requestDto.getName());
        }

        CategoryEntity entity = categoryMapper.mapToEntity(requestDto);
        return categoryMapper.mapToDto(save(entity));
    }

    /// PUT METHODS

    /// remove this category from cache memory using id
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponseDto updateCategory(CategoryRequestDto requestDto, Long id) {
        // first check the category is existed
        CategoryEntity existingEntity = findById(id);

        // check if user want to change name of category must be unique
        if (!existingEntity.getName().equalsIgnoreCase(requestDto.getName()) && categoryRepo.existsByName(requestDto.getName())) {
            throw new DuplicateRecordException("Category", "name", requestDto.getName());
        }

        // update the entity data
        categoryMapper.updateEntityFromDto(requestDto, existingEntity);
        return categoryMapper.mapToDto(save(existingEntity));
    }

    /// DELETE METHODS

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        // hard delete method and move products into Uncategorized

        // first check about default category
        // equals because this an object not primitive
        // if i use == here i compare the memory address
        if (id.equals(DEFAULT_CATEGORY_ID)) {
            throw new BusinessLogicException("validation.category.default");
        }

        CategoryEntity existingEntity = categoryRepo.findByIdOrThrow(id);

        // move here
        productRepo.moveProductsToDefaultCategory(id);
        categoryRepo.delete(existingEntity);
    }

    // ///////// HARD DELETE METHOD
    // @Transactional
    // public void forceDeleteCategory(Long id) {
    //     categoryRepo.hardDeleteCategory(id);
    // }
}
