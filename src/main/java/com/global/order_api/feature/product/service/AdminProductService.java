package com.global.order_api.feature.product.service;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.service.FileUploadService;
import com.global.order_api.feature.category.entity.CategoryEntity;
import com.global.order_api.feature.category.repo.CategoryRepo;
import com.global.order_api.feature.product.dto.AdminProductResponseDto;
import com.global.order_api.feature.product.specification.ProductFilterRequest;
import com.global.order_api.feature.product.dto.ProductRequestDto;
import com.global.order_api.feature.product.dto.UserProductResponseDto;
import com.global.order_api.feature.product.entity.ProductEntity;
import com.global.order_api.feature.product.mapper.ProductMapper;
import com.global.order_api.feature.product.repo.ProductRepo;
import com.global.order_api.feature.product.specification.ProductSpecification;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
/// disable dirty checking to save mem ,cpu
@Transactional(readOnly = true)
public class AdminProductService extends BaseService<ProductEntity, Long> {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductMapper productMapper;
    private final FileUploadService fileUploadService;
    /// to control my cache
    private final CacheManager cacheManager;

    public AdminProductService(ProductRepo productRepo, ProductMapper productMapper,
            CategoryRepo categoryRepo, FileUploadService fileUploadService,
            CacheManager cacheManager) {
        super(productRepo);
        this.productRepo = productRepo;
        this.productMapper = productMapper;
        this.categoryRepo = categoryRepo;
        this.fileUploadService = fileUploadService;
        this.cacheManager = cacheManager;
    }

    // ==================================================================================
    // 1. READ METHODS (ADMIN)
    // ==================================================================================

    /// ADMIN METHODS (No Caching - Real-Time Data)
    public PageResponse<AdminProductResponseDto> getAdminProductsPage(ProductFilterRequest filter) {

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Page<ProductEntity> productPage;

        /// check search word to call full text index
        if (filter.getSearchKeyword() != null && !filter.getSearchKeyword().isBlank()) {
            String formattedKeyword = filter.getSearchKeyword().trim() + "*";
            productPage = productRepo.searchActiveByNameFullText(formattedKeyword, pageable);
        } else {
            Specification<ProductEntity> spec = ProductSpecification.buildFilter(filter, false);
            productPage = productRepo.findAll(spec, pageable);
        }

        List<AdminProductResponseDto> dtoList = productMapper.mapToAdminDtoList(productPage.getContent());
        return PageResponse.from(productPage, dtoList);
    }

    @Cacheable(value = "productsPage", key = "'deletedProductsPage'", condition = "#filter.page == 0 &&" +
            " (#filter.searchKeyword == null || #filter.searchKeyword.isEmpty())" +
            " && #filter.categoryId == null")
    public PageResponse<UserProductResponseDto> getDeletedProducts(ProductFilterRequest filter) {

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        // Pageable => take all user input
        // will be translated to SQL
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        // holds data + meta data about it
        Page<ProductEntity> productPage;

        if (filter.getSearchKeyword() != null && !filter.getSearchKeyword().isBlank()) {
            String formattedKeyword = filter.getSearchKeyword().trim() + "*";
            productPage = productRepo.searchActiveByNameFullText(formattedKeyword, pageable);
        } else {
            Specification<ProductEntity> spec = ProductSpecification.buildFilter(filter, true);
            productPage = productRepo.findAll(spec, pageable);
        }
        List<UserProductResponseDto> dtoList = productMapper.mapToDtoList(productPage.getContent());
        return PageResponse.from(productPage, dtoList);
    }

    // ==================================================================================
    // 2. WRITE METHODS (CREATE & UPDATE)
    // ==================================================================================

    @Transactional
    public UserProductResponseDto createProduct(ProductRequestDto requestDto, MultipartFile image) {
        // first check if category is existed
        CategoryEntity categoryEntity = categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
        ProductEntity entity = productMapper.mapToEntity(requestDto);
        entity.setCategory(categoryEntity);
        // upload image
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileUploadService.uploadImage(image);
            entity.setImage(imageUrl);
        }
        return productMapper.mapToDto(save(entity));
    }

    @Transactional
    public UserProductResponseDto updateProduct(ProductRequestDto requestDto, Long id, MultipartFile newImage) {
        // first check the category is existed
        ProductEntity existingEntity = productRepo.findByIdOrThrow(id);
        String oldName = existingEntity.getName();
        // second check if category id is changed
        if (requestDto.getCategoryId() != null) {
            CategoryEntity categoryEntity = categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
            existingEntity.setCategory(categoryEntity);
        }
        // check about new Image
        if (newImage != null && !newImage.isEmpty()) {
            // check if product already has image
            if (existingEntity.getImage() != null) {
                fileUploadService.deleteImage(existingEntity.getImage());
            }
            // add new Image
            String newImageurl = fileUploadService.uploadImage(newImage);
            existingEntity.setImage(newImageurl);
        }
        // update the remaining entity data
        productMapper.updateEntityFromDto(requestDto, existingEntity);
        //// remove this product cache
        Cache productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(id);
        }
        return productMapper.mapToDto(save(existingEntity));
    }

    // ==================================================================================
    // 3. DELETE & RESTORE METHODS
    // ==================================================================================

    @Transactional
    public void deleteProduct(Long id) {
        ProductEntity entity = productRepo.findByIdOrThrow(id);
        String name = entity.getName();
        delete(id);
        //// remove this product cache
        Cache productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(id);
        }
    }

    @Transactional
    // HARD DELETE
    public void forceDeleteProduct(Long id) {
        ProductEntity entity = productRepo.findByIdOrThrow(id);
        String name = entity.getName();
        productRepo.getImageUrlByIdEvenIfDeleted(id)
                .ifPresent(imageUrl -> fileUploadService.deleteImage(imageUrl));
        productRepo.hardDeleteProduct(id);
        //// remove this product cache
        Cache productCache = cacheManager.getCache("product");
        if (productCache != null) {
            productCache.evict(id);
            productCache.evict(name);
        }
    }

    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public void restoreProduct(Long id) {
        productRepo.restoreProduct(id);
    }
}