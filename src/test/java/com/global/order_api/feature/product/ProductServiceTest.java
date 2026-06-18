package com.global.order_api.feature.product;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Product Tests (GET)")
    class GetProductTests {

        /// // Find by ID Exists - RETURN PRODUCT
        @Test
        void getProductByIdWithCategory_WhenExists_ShouldReturnProduct() {
            Long id = 1L;
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(id);
            fakeEntity.setName("Laptop");

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setId(id);
            fakeDto.setName("Laptop");

            when(productRepo.findByIdWithCategory(id)).thenReturn(Optional.of(fakeEntity));
            when(productMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

            UserProductResponseDto result = productService.getProductByIdWithCategory(id);

            assertNotNull(result);
            assertEquals("Laptop", result.getName());
            verify(productRepo, times(1)).findByIdWithCategory(id);
            verify(productMapper, times(1)).mapToDto(fakeEntity);
        }

        /// // Find by ID Does not Exist - NOT FOUND
        @Test
        void getProductByIdWithCategory_WhenIdNotFound_ShouldThrowException() {
            Long invalidId = 999L;

            when(productRepo.findByIdWithCategory(invalidId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getProductByIdWithCategory(invalidId));

            verify(productRepo, times(1)).findByIdWithCategory(invalidId);
            verify(productMapper, never()).mapToDto(any());
        }

        /// // 1. Get Products Page WITHOUT Keyword (Should call Specification)
        @Test
        void getProductsPage_WhenNoKeyword_ShouldCallFindAllWithSpecification() {
            ProductFilterRequest filter = new ProductFilterRequest();

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Phone");
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setName("Phone");

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserProductResponseDto> result = productService.getProductsPage(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            assertEquals("Phone", result.getData().get(0).getName());

            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(productRepo, never()).searchActiveByNameFullText(anyString(), any(Pageable.class));
        }

        /// // 2. Get Products Page WITH Keyword (Should call Full Text Search)
        @Test
        void getProductsPage_WhenKeywordExists_ShouldCallFullTextSearch() {
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setSearchKeyword("smart");

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Smartphone");
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setName("Smartphone");

            when(productRepo.searchActiveByNameFullText(eq("smart"), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserProductResponseDto> result = productService.getProductsPage(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());

            verify(productRepo, times(1)).searchActiveByNameFullText(eq("smart"), any(Pageable.class));
            verify(productRepo, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        /// // 3. Get Products Page - Empty DB
        @Test
        void getProductsPage_WhenNoProducts_ShouldReturnEmptyPage() {
            ProductFilterRequest filter = new ProductFilterRequest();
            Page<ProductEntity> emptyMockPage = new PageImpl<>(List.of());

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);
            when(productMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            PageResponse<UserProductResponseDto> result = productService.getProductsPage(filter);

            assertNotNull(result);
            assertTrue(result.getData().isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }

        /// ///////////// GET PRODUCT STOCK COUNT States /////////////
        /// // Get Product Stock Count - SUCCESS
        @Test
        void getProductStockCount_WhenProductExists_ShouldReturnStockCount() {
            Long productId = 1L;
            int expectedStock = 50;

            when(productRepo.findStockCountById(productId)).thenReturn(Optional.of(expectedStock));

            int result = productService.getProductStockCount(productId);

            assertEquals(expectedStock, result);

            verify(productRepo, times(1)).findStockCountById(productId);
        }

        /// // Get Product Stock Count - FAIL: Product Not Found
        @Test
        void getProductStockCount_WhenProductDoesNotExist_ShouldThrowResourceNotFoundException() {
            Long nonExistingId = 999L;

            when(productRepo.findStockCountById(nonExistingId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                productService.getProductStockCount(nonExistingId);
            });

            verify(productRepo, times(1)).findStockCountById(nonExistingId);
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. System Write Methods (Order Actions)")
    class SystemWriteMethods {

        /// ///////////// REDUCE STOCK States /////////////
        /// // Reduce Stock - SUCCESS
        @Test
        void reduceStock_WhenExistsAndStockSufficient_ShouldReduceAndSave() {
            Long productId = 1L;
            int requestedQuantity = 2;

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);
            fakeEntity.setName("Laptop");
            fakeEntity.setStockCount(5);

            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.of(fakeEntity));

            productService.reduceStock(productId, requestedQuantity);

            assertEquals(3, fakeEntity.getStockCount());

            verify(productRepo, times(1)).findByIdForUpdate(productId);
            verify(productRepo, times(1)).save(fakeEntity);
        }

        /// // Reduce Stock - FAIL: Insufficient Stock
        @Test
        void reduceStock_WhenRequestedQuantityGreaterThanStock_ShouldThrowBusinessLogicException() {
            Long productId = 1L;
            int requestedQuantity = 10;

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);
            fakeEntity.setStockCount(5);

            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.of(fakeEntity));

            assertThrows(BusinessLogicException.class, () -> {
                productService.reduceStock(productId, requestedQuantity);
            });

            verify(productRepo, times(1)).findByIdForUpdate(productId);
            verify(productRepo, never()).save(any());
        }

        /// // Reduce Stock - FAIL: Product Not Found
        @Test
        void reduceStock_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
            Long productId = 999L;
            int requestedQuantity = 2;

            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                productService.reduceStock(productId, requestedQuantity);
            });

            verify(productRepo, times(1)).findByIdForUpdate(productId);
            verify(productRepo, never()).save(any());
        }
    }
}