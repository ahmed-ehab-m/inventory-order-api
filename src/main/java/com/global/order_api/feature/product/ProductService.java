package com.global.order_api.feature.product;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.service.FileUploadService;
import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.category.CategoryRepo;
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
public class ProductService extends BaseService<ProductEntity, Long> {

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final ProductMapper productMapper;
    private final FileUploadService fileUploadService;
    /// to control my cache
    private final CacheManager cacheManager;

    public ProductService(ProductRepo productRepo,
                          ProductMapper productMapper, CategoryRepo categoryRepo, FileUploadService fileUploadService,
                          CacheManager cacheManager) {
        super(productRepo);
        this.productRepo = productRepo;
        this.productMapper = productMapper;
        this.categoryRepo = categoryRepo;
        this.fileUploadService = fileUploadService;
        this.cacheManager = cacheManager;
    }

    ////////////////////CACHING//////////////////////
    /// PRODUCTS PAGE TTL => 15 M
    /// PRODUCT TTL => 60 M
    ///  Product metadata => name , description , image ,price , category id
    ///  metadata => READ-HEAVY , WRTIE-RARE (as Category)
    ///  metaData CACHE STRATEGY => READING = CACHE-ASIDE || WRITING = WRITE-AROUND (DB)

    ///  product stockCount => READ-HEAVY , WRTIE-HEAVY
    ///  stockCount CACHE STRATEGY => READING = CACHE-ASIDE (cache) || WRITING = WRITE-AROUND (DB) (lock)
    ///  so we separate our product to 2 endpoints or services
    ///  first for rare writing data => name , description , image ,price , category id
    ///  second for stockCount only  low ttl
    ///  and this not big traffic on db because
    ///     very simple query => SELECT stock_count FROM products WHERE id = 1;
    ///     microCaching => 5 seconds => in 60 seconds db receives only 12 query
    /// /////////////////////////
    /// READ METHODS
    /// ////////////
    @Cacheable(value = "product", key = "#id")
    public UserProductResponseDto getProductByIdWithCategory(Long id) {
        ProductEntity productEntity = productRepo.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.mapToDto(productEntity);
    }
    //////////////////
//        public PageResponse<UserProductResponseDto> getProductByName(String name ,ProductFilterRequest filter)
//        {
//            Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
//            //Pageable => take all user input
//            // will be translated to SQL
//            Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
//            Page<ProductEntity> productPage = productRepo.searchByNameFullText(name, pageable);
//            List<UserProductResponseDto> dtoList=productMapper.mapToDtoList(productPage.getContent());
//            return PageResponse.from(productPage, dtoList);
//
//        }

    /// ////////////////
    /// LIGHTWEIGHT STOCK READ (Micro-Caching)
    /// ///////////////
    @Cacheable(value = "productStock", key = "#id")
    public int getProductStockCount(Long id) {
        return productRepo.findStockCountById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    /// ///////////////
    /// USER METHOD
    // smart method for pagination
    // take filter => smart object contains page number , size ,sort type , keyword
    // return pageResponse we created in core folder
    @Cacheable(
            value = "productsPage",
            key = "'defaultHomePage'",
            condition = "#filter.page == 0 &&" +
                    " (#filter.searchKeyword == null || #filter.searchKeyword.isEmpty())" +
                    " && #filter.categoryId == null")
    public PageResponse<UserProductResponseDto> getProductsPage(ProductFilterRequest filter) {

        // take user input => "ASC" OR "DESC" from headers
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        //Pageable => take all user input
        // will be translated to SQL
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        Page<ProductEntity> productPage;
        /// check search word to call full text index
        if (filter.getSearchKeyword() != null && !filter.getSearchKeyword().isBlank()) {
            productPage = productRepo.searchActiveByNameFullText(filter.getSearchKeyword(), pageable);
        } else {
            Specification<ProductEntity> spec = ProductSpecification.buildFilter(filter, false);
            productPage = productRepo.findAll(spec, pageable);
        }
        List<UserProductResponseDto> dtoList = productMapper.mapToDtoList(productPage.getContent());
        return PageResponse.from(productPage, dtoList);
    }

    /// ///////////////////////////
    /// ///////////////
    /// ADMIN METHODS (No Caching - Real-Time Data)
    /// ///////////////
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

    /// /////////////////////
    @Cacheable(
            value = "productsPage",
            key = "'deletedProductsPage'",
            condition = "#filter.page == 0 &&" +
                    " (#filter.searchKeyword == null || #filter.searchKeyword.isEmpty())" +
                    " && #filter.categoryId == null")
    public PageResponse<UserProductResponseDto> getDeletedProducts(ProductFilterRequest filter) {

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        //Pageable => take all user input
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
    ////////////

    /// /////////////////
    /// WRITE METHODS
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

    /// /////////////
    @CacheEvict(value = "product", key = "#id")
    public void restoreProduct(Long id) {
        productRepo.restoreProduct(id);
    }

    /// /////////////////////////
    @Transactional
    public void reduceStock(Long productId, int requestedQuantity) {
        /// 1=> get our product
        ProductEntity product = productRepo.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        /// 2=> check the requested quantity if not available in Stock
        if (requestedQuantity > product.getStockCount()) {
            throw new BusinessLogicException("error.insufficient.stock",
                    new Object[]{requestedQuantity, product.getStockCount()});
        }
        /// 3=> reduce stock count and save
        product.setStockCount(product.getStockCount() - requestedQuantity);
        productRepo.save(product);
    }

    /// DELETE METHODS
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


}
