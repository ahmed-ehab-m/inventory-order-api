    package com.global.order_api.feature.product;
    
    import java.util.List;

    import com.global.order_api.core.exception.BusinessLogicException;
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

        ////////////////////CACHING//////////////////////
        /// PRODUCTS PAGE TTL => 15 M
        /// PRODUCT TTL => 60 M
        ///  Product metadata => name , description , image ,price , category id
        ///  metadata => READ-HEAVY , WRTIE-RARE (as Category)
        ///  metaData CACHE STRATEGY => READING = CACHE-ASIDE || WRITING = WRITE-AROUND (DB)

        ///  product stockCount => READ-HEAVY , WRTIE-HEAVY
        ///  so we separate our product to 2 endpoints or services
        ///  first for rare writing data => name , description , image ,price , category id
        ///  second for stockCount only  low ttl
        ///  and this not big traffic on db because
        ///     very simple query => SELECT stock_count FROM products WHERE id = 1;
        ///     microCaching => 5 seconds => in 60 seconds db receives only 12 query
        ///  CACHE STRATEGY => READING = READ-THROUGH || WRITING = WRITE-THROUGH
        ///  WRITE-THROUGH => no need to write in db , redis (in same time)
        /// 			  => rare editing on categories
        /// 			  => simple , save resources
        ///  CACHE-ASIDE  => resistance even if redis fail
        /// 			  => simple
        /// 			  => on-demand loading or lazy loading
        /// 			  => store only what users needed , high traffic to save resources
        ////////////////////////////
        /// READ METHODS
        ///////////////
        @Cacheable(value = "product",key = "#id")
        public ProductResponseDto getProductByIdWithCategory(Long id)
        {
            ProductEntity productEntity = productRepo.findByIdWithCategory(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
            return productMapper.mapToDto(productEntity);
        }
        //////////////////
        @Cacheable(value = "product",key = "#name")
        public ProductResponseDto getProductByName(String name)
        {
            ProductEntity productEntity = productRepo.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "name", name));
            return productMapper.mapToDto(productEntity);
        }
        ///////////////////
        /// LIGHTWEIGHT STOCK READ (Micro-Caching)
        //////////////////
        @Cacheable(value = "productStock", key = "#id")
        public int getProductStockCount(Long id) {
            return productRepo.findStockCountById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        }
        //////////////////
        // smart method for pagination
        // take filter => smart object contains page number , size ,sort type , keyword
        // return pageResponse we created in core folder
        @Cacheable(
                value = "productsPage",
                key = "'defaultHomePage'",
                condition = "#filter.page == 0 &&" +
                        " (#filter.keyword == null || #filter.keyword.isEmpty())" +
                        " && #filter.categoryId == null")
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
                value = "productsPage",
                key = "'deletedProductsPage'",
                condition = "#filter.page == 0 &&" +
                        " (#filter.keyword == null || #filter.keyword.isEmpty())" +
                        " && #filter.categoryId == null")
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

        @Transactional
        @CacheEvict(value = "product", key = "#id")
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
        @CacheEvict(value = "product", key = "#id")
        public void restoreProduct(Long id)
        {
            productRepo.restoreProduct(id);
        }

        ////////////////////////////
        @Transactional
        public void reduceStock(Long productId , int requestedQuantity)
        {
            /// 1=> get our product
            ProductEntity product= productRepo.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
            /// 2=> check the requested quantity if not available in Stock
            if(requestedQuantity > product.getStockCount())
            {
                throw new BusinessLogicException("error.insufficient.stock",
                        new Object[]{requestedQuantity, product.getStockCount()});
            }
            /// 3=> reduce stock count and save
            product.setStockCount(product.getStockCount()-requestedQuantity);
            productRepo.save(product);
        }
        ///DELETE METHODS
        @Transactional
        @CacheEvict(value = "product", key = "#id")
        public void deleteProduct(Long id) {
            delete(id);
        }
        
        @Transactional
        // HARD DELETE
        @CacheEvict(value = "product", key = "#id")
        public void forceDeleteProduct(Long id)
        {
            productRepo.findByIdOrThrow(id);
            productRepo.getImageUrlByIdEvenIfDeleted(id)
                            .ifPresent(imageUrl -> fileUploadService.deleteImage(imageUrl));
            productRepo.hardDeleteProduct(id);
        }
        
        
        
    }
