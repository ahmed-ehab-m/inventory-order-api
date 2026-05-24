    package com.global.order_api.feature.product;
    
    import java.util.List;

    import org.springframework.cache.annotation.CacheEvict;
    import org.springframework.cache.annotation.Cacheable;
    import org.springframework.cache.annotation.Caching;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.web.multipart.MultipartFile;
    
    import com.global.order_api.core.base.BaseRepo;
    import com.global.order_api.core.base.BaseService;
    import com.global.order_api.core.base.PageResponse;
    import com.global.order_api.core.exception.ResourceNotFoundException;
    import com.global.order_api.core.service.FileUploadService;
    import com.global.order_api.feature.category.CategoryEntity;
    import com.global.order_api.feature.category.CategoryRepo;
    
    
    @Service
    public class ProductService extends BaseService<ProductEntity, Long> {
        
        private final ProductRepo productRepo;
        private final CategoryRepo categoryRepo;
        private final ProductMapper productMapper;
        private final FileUploadService fileUploadService;
        
        public ProductService(ProductRepo productRepo,
                ProductMapper productMapper, CategoryRepo categoryRepo ,FileUploadService fileUploadService) {
            super(productRepo);
            this.productRepo = productRepo;
            this.productMapper = productMapper;
            this.categoryRepo=categoryRepo;
            this.fileUploadService=fileUploadService;
        }
        
        ////////////////////////////
        /// READ METHODS
        ///////////////
        @Cacheable(value = "products",key = "#id")
        public ProductResponseDto getProductByIdWithCategory(Long id)
        {
            ProductEntity productEntity = productRepo.findByIdWithCategory(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
            return productMapper.mapToDto(productEntity);
        }
        //////////////////
        @Cacheable(value = "products",key = "#name")
        public ProductResponseDto getProductByName(String name)
        {
            ProductEntity productEntity = productRepo.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "name", name));
            return productMapper.mapToDto(productEntity);
        }
        //////////////////
        @Cacheable(
                value = "productsPage",
                key = "#filter.generateCacheKey()"
        )
        // smart method for pagination
        // take filter => smart object contains page number , size ,sort type , keyword
        // return pageResponse we created in core folder
        public PageResponse<ProductResponseDto> getProductsPage(ProductFilterRequest filter) {
        // take user input => "ASC" OR "DESC" from headers
            Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
            //Pageable => take all user input 
            // will be translated to SQL 
            Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
            // holds data + meta data about it
            Specification<ProductEntity> spec = ProductSpecification.buildFilter(filter);
            Page<ProductEntity> productPage = productRepo.findAll(spec, pageable);
            List<ProductResponseDto> dtoList=productMapper.mapToDtoList(productPage.getContent());
            return PageResponse.from(productPage, dtoList);      
        }
        ////////////////////////
        @Cacheable(
                value  = "deletedProductsPage",
                key = "#filter.generateCacheKey()"
        )
        public PageResponse<ProductResponseDto>  getDeletedProducts(ProductFilterRequest filter)
        {
            Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
            //Pageable => take all user input 
            // will be translated to SQL 
            Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
            // holds data + meta data about it
            Specification<ProductEntity> spec = ProductSpecification.buildFilter(filter);
            Page<ProductEntity> productPage = productRepo.findAllDeletedProducts(spec,pageable);
            List<ProductResponseDto> dtoList=productMapper.mapToDtoList(productPage.getContent());
            return PageResponse.from(productPage, dtoList); 
        }
        ////////////
        
        ////////////////////
        ///WRITE METHODS
        @CacheEvict(value = "productsPage",allEntries = true)
        @Transactional
        public ProductResponseDto createProduct(ProductRequestDto requestDto , MultipartFile image)
        {
            // first check if category is existed
            CategoryEntity categoryEntity = categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
            ProductEntity entity=productMapper.mapToEntity(requestDto);
            entity.setCategory(categoryEntity);
            // upload image
            if (image != null && !image.isEmpty()) {
                String imageUrl = fileUploadService.uploadImage(image);
                entity.setImage(imageUrl);
            }
            return productMapper.mapToDto(save(entity));
        }

        @Caching(evict = {
                @CacheEvict(value = "productsPage", allEntries = true),
                @CacheEvict(value = "products", key = "#id"),
                @CacheEvict(value = "products", allEntries = true)
        })
        @Transactional
        public ProductResponseDto updateProduct(ProductRequestDto requestDto,Long id ,MultipartFile newImage)
        {
            // first check the category is existed
            ProductEntity existingEntity= productRepo.findByIdOrThrow(id);
            // second check if category id is changed
            if(requestDto.getCategoryId() !=null)
            {
                CategoryEntity categoryEntity =categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
                existingEntity.setCategory(categoryEntity);
            }
            // check about new Image
            if(newImage !=null && !newImage.isEmpty())
            {
                // check if product already has image
                if(existingEntity.getImage() !=null)
                {
                    fileUploadService.deleteImage(existingEntity.getImage());
                }
                // add new Image
                String newImageurl=fileUploadService.uploadImage(newImage);
                existingEntity.setImage(newImageurl);
            }
            // update the remaining entity data
            productMapper.updateEntityFromDto(requestDto, existingEntity);
            return productMapper.mapToDto(save(existingEntity));
        }
        ////////////////

        @Caching(evict = {
                @CacheEvict(value = "productsPage", allEntries = true),
                @CacheEvict(value = "deletedProductsPage", allEntries = true),
               @CacheEvict(value = "products", allEntries = true)
        })
        public void restoreProduct(Long id)
        {
            productRepo.restoreProduct(id);
        }
        ///DELETE METHODS
        @Caching(evict = {
                @CacheEvict(value = "productsPage", allEntries = true),
                @CacheEvict(value = "deletedProductsPage", allEntries = true),
                @CacheEvict(value = "products", allEntries = true)
        })
        @Transactional
        public void deleteProduct(Long id) {
            delete(id);
        }
        
        @Transactional
        @Caching(evict = {
                @CacheEvict(value = "productsPage", allEntries = true),
                @CacheEvict(value = "deletedProductsPage", allEntries = true),
                @CacheEvict(value = "products", allEntries = true)
        })
        // HARD DELETE
        public void forceDeleteProduct(Long id)
        {
            productRepo.findByIdOrThrow(id);
            productRepo.getImageUrlByIdEvenIfDeleted(id)
                            .ifPresent(imageUrl -> fileUploadService.deleteImage(imageUrl));
            productRepo.hardDeleteProduct(id);
        }
        
        
        
    }
