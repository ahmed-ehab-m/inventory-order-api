package com.global.order_api.feature.product;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/// disable dirty checking to save mem ,cpu
@Transactional(readOnly = true)
public class ProductService extends BaseService<ProductEntity, Long> {

    private final ProductRepo productRepo;
    private final ProductMapper productMapper;

    public ProductService(ProductRepo productRepo, ProductMapper productMapper) {
        super(productRepo);
        this.productRepo = productRepo;
        this.productMapper = productMapper;
    }

    // ==================================================================================
    // CACHING STRATEGY NOTES
    // ==================================================================================
    /// PRODUCTS PAGE TTL => 15 M
    /// PRODUCT TTL => 60 M
    /// Product metadata => name , description , image ,price , category id
    /// metadata => READ-HEAVY , WRTIE-RARE (as Category)
    /// metaData CACHE STRATEGY => READING = CACHE-ASIDE || WRITING = WRITE-AROUND
    /// (DB)
    /// product stockCount => READ-HEAVY , WRTIE-HEAVY
    /// stockCount CACHE STRATEGY => READING = CACHE-ASIDE (cache) || WRITING =
    // WRITE-AROUND (DB) (lock)
    /// so we separate our product to 2 endpoints or services
    /// first for rare writing data => name , description , image ,price , category
    // id
    /// second for stockCount only low ttl
    /// and this not big traffic on db because
    /// very simple query => SELECT stock_count FROM products WHERE id = 1;
    /// microCaching => 5 seconds => in 60 seconds db receives only 12 query

    // ==================================================================================
    // 1. READ METHODS (USER)
    // ==================================================================================

    @Cacheable(value = "product", key = "#id")
    public UserProductResponseDto getProductByIdWithCategory(Long id) {
        ProductEntity productEntity = productRepo.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return productMapper.mapToDto(productEntity);
    }

    // smart method for pagination
    // take filter => smart object contains page number , size ,sort type , keyword
    // return pageResponse we created in core folder
    @Cacheable(value = "productsPage", key = "'defaultHomePage'", condition = "#filter.page == 0 &&" +
            " (#filter.searchKeyword == null || #filter.searchKeyword.isEmpty())" +
            " && #filter.categoryId == null")
    public PageResponse<UserProductResponseDto> getProductsPage(ProductFilterRequest filter) {

        // take user input => "ASC" OR "DESC" from headers
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        // Pageable => take all user input
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

    // ==================================================================================
    // 2. LIGHTWEIGHT STOCK READ (Micro-Caching)
    // ==================================================================================

    @Cacheable(value = "productStock", key = "#id")
    public int getProductStockCount(Long id) {
        return productRepo.findStockCountById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    // ==================================================================================
    // 3. SYSTEM WRITE METHODS (Order Actions)
    // ==================================================================================

    @Transactional
    public void reduceStock(Long productId, int requestedQuantity) {
        /// 1=> get our product
        ProductEntity product = productRepo.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        /// 2=> check the requested quantity if not available in Stock
        if (requestedQuantity > product.getStockCount()) {
            throw new BusinessLogicException("error.insufficient.stock",
                    new Object[] { requestedQuantity, product.getStockCount() });
        }
        /// 3=> reduce stock count and save
        product.setStockCount(product.getStockCount() - requestedQuantity);
        productRepo.save(product);
    }
}