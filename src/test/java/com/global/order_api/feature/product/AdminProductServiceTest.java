package com.global.order_api.feature.product;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
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
import com.global.order_api.feature.product.service.AdminProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryRepo categoryRepo;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private AdminProductService adminProductService;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Admin Product Tests (GET)")
    class GetAdminProductTests {

        @Test
        void getAdminProductsPage_ShouldCorrectlyMapStockCount() {
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortDirection("ASC");
            filter.setSortBy("id");

            ProductEntity product = new ProductEntity();
            product.setId(1L);
            product.setName("Laptop");
            product.setStockCount(75);

            Page<ProductEntity> productPage = new PageImpl<>(List.of(product));

            AdminProductResponseDto adminDto = new AdminProductResponseDto();
            adminDto.setName("Laptop");
            adminDto.setStockCount(75);

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(productPage);
            when(productMapper.mapToAdminDtoList(anyList())).thenReturn(List.of(adminDto));

            PageResponse<AdminProductResponseDto> response = adminProductService.getAdminProductsPage(filter);

            assertNotNull(response);
            assertEquals(1, response.getData().size());
            assertEquals(75, response.getData().get(0).getStockCount());
        }

        @Test
        void getAdminProductsPage_WhenKeywordExists_ShouldCallFullTextSearch() {
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setSearchKeyword("laptop");

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Laptop Pro");
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            AdminProductResponseDto fakeDto = new AdminProductResponseDto();
            fakeDto.setName("Laptop Pro");

            when(productRepo.searchActiveByNameFullText(eq("laptop*"), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToAdminDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<AdminProductResponseDto> result = adminProductService.getAdminProductsPage(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            verify(productRepo, times(1)).searchActiveByNameFullText(eq("laptop*"), any(Pageable.class));
            verify(productRepo, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        /// // 1. Get Deleted Products WITHOUT Keyword
        @Test
        void getDeletedProducts_WhenNoKeyword_ShouldCallFindAllWithSpecification() {
            ProductFilterRequest filter = new ProductFilterRequest();

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Deleted Phone");
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setName("Deleted Phone");

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserProductResponseDto> result = adminProductService.getDeletedProducts(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());

            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(productRepo, never()).searchDeletedByNameFullText(anyString(), any(Pageable.class));
        }

        /// // 2. Get Deleted Products WITH Keyword
        @Test
        void getDeletedProducts_WhenKeywordExists_ShouldCallDeletedFullTextSearch() {
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setSearchKeyword("broken");

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Broken Screen");
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setName("Broken Screen");

            when(productRepo.searchActiveByNameFullText(eq("broken*"), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            PageResponse<UserProductResponseDto> result = adminProductService.getDeletedProducts(filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());

            verify(productRepo, times(1)).searchActiveByNameFullText(eq("broken*"), any(Pageable.class));
            verify(productRepo, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        void getDeletedProductsPage_WhenNoProducts_ShouldReturnEmptyPage() {
            ProductFilterRequest filter = new ProductFilterRequest();
            Page<ProductEntity> emptyMockPage = new PageImpl<>(List.of());

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);
            when(productMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            PageResponse<UserProductResponseDto> result = adminProductService.getDeletedProducts(filter);

            assertNotNull(result);
            assertTrue(result.getData().isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Product Tests (POST)")
    class CreateProductTests {

        /// // Create Product WITH Image - RETURN CREATED PRODUCT
        @Test
        void createProduct_WithImage_ShouldUploadImageAndSaveProduct() {
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("New TV");
            requestDto.setCategoryId(2L);

            MultipartFile mockImage = new MockMultipartFile(
                    "image", "tv.jpg", "image/jpeg", "fake image data".getBytes()
            );

            CategoryEntity category = new CategoryEntity();
            category.setId(2L);

            ProductEntity mappedEntity = new ProductEntity();
            mappedEntity.setName("New TV");

            UserProductResponseDto responseDto = new UserProductResponseDto();
            responseDto.setName("New TV");

            when(categoryRepo.findByIdOrThrow(2L)).thenReturn(category);
            when(productMapper.mapToEntity(requestDto)).thenReturn(mappedEntity);
            when(fileUploadService.uploadImage(mockImage)).thenReturn("http://cloudinary.com/tv.jpg");
            when(productRepo.save(any(ProductEntity.class))).thenReturn(mappedEntity);
            when(productMapper.mapToDto(mappedEntity)).thenReturn(responseDto);

            UserProductResponseDto result = adminProductService.createProduct(requestDto, mockImage);

            assertNotNull(result);
            assertEquals("New TV", result.getName());

            verify(categoryRepo, times(1)).findByIdOrThrow(2L);
            verify(fileUploadService, times(1)).uploadImage(mockImage);
            verify(productRepo, times(1)).save(mappedEntity);
        }

        /// // Create Product WITHOUT Image - RETURN CREATED PRODUCT
        @Test
        void createProduct_WithoutImage_ShouldSaveProductWithoutUpload() {
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(2L);
            CategoryEntity category = new CategoryEntity();

            when(categoryRepo.findByIdOrThrow(2L)).thenReturn(category);
            when(productMapper.mapToEntity(requestDto)).thenReturn(new ProductEntity());
            when(productRepo.save(any(ProductEntity.class))).thenReturn(new ProductEntity());
            when(productMapper.mapToDto(any())).thenReturn(new UserProductResponseDto());

            adminProductService.createProduct(requestDto, null);

            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, times(1)).save(any(ProductEntity.class));
        }

        /// // Create Product - Category Not Found - Should Throw Exception
        @Test
        void createProduct_WhenCategoryDoesNotExist_ShouldThrowException() {
            Long invalidCategoryId = 99L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(invalidCategoryId);

            MultipartFile mockImage = new MockMultipartFile(
                    "image", "tv.jpg", "image/jpeg", "data".getBytes()
            );

            when(categoryRepo.findByIdOrThrow(invalidCategoryId))
                    .thenThrow(new ResourceNotFoundException("Category", "id", invalidCategoryId));

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.createProduct(requestDto, mockImage));

            verify(categoryRepo, times(1)).findByIdOrThrow(invalidCategoryId);
            verify(productMapper, never()).mapToEntity(any());
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////UPDATE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Update Product Tests (PUT)")
    class UpdateProductTests {

        /// // Update Product WITH New Image (Deletes old image) - RETURN UPDATED PRODUCT
        @Test
        void updateProduct_WithNewImage_ShouldDeleteOldAndUploadNew() {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(5L);

            MultipartFile mockNewImage = new MockMultipartFile(
                    "image", "new.jpg", "image/jpeg", "data".getBytes()
            );

            ProductEntity existingEntity = new ProductEntity();
            existingEntity.setImage("http://old-image-url.com");

            CategoryEntity newCategory = new CategoryEntity();

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingEntity);
            when(categoryRepo.findByIdOrThrow(5L)).thenReturn(newCategory);

            doNothing().when(fileUploadService).deleteImage("http://old-image-url.com");
            when(fileUploadService.uploadImage(mockNewImage)).thenReturn("http://new-image-url.com");

            when(productRepo.save(existingEntity)).thenReturn(existingEntity);
            when(productMapper.mapToDto(existingEntity)).thenReturn(new UserProductResponseDto());

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.updateProduct(requestDto, productId, mockNewImage);

            verify(fileUploadService, times(1)).deleteImage("http://old-image-url.com");
            verify(fileUploadService, times(1)).uploadImage(mockNewImage);
            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingEntity);
            verify(productRepo, times(1)).save(existingEntity);
            verify(mockCache, times(1)).evict(productId);
        }

        /// // Update Product without changing image - Product Updated
        @Test
        void updateProduct_WithoutNewImage_ShouldOnlyUpdateFields() {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("Updated Name Only");
            requestDto.setCategoryId(1L);

            ProductEntity existingProduct = new ProductEntity();
            existingProduct.setImage("http://old-image.com");

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);
            when(productRepo.save(existingProduct)).thenReturn(existingProduct);
            when(productMapper.mapToDto(existingProduct)).thenReturn(new UserProductResponseDto());

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.updateProduct(requestDto, productId, null);

            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingProduct);
            verify(fileUploadService, never()).deleteImage(anyString());
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, times(1)).save(existingProduct);
            verify(mockCache, times(1)).evict(productId);
        }

        /// // Update Product(without image) with uploading image  - Product Updated
        @Test
        void updateProduct_WithNewImageWhenNoOldImageExists_ShouldOnlyUploadNewImage() {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(1L);

            MultipartFile mockNewImage = new MockMultipartFile(
                    "image", "new_pic.jpg", "image/jpeg", "data".getBytes()
            );
            ProductEntity existingProduct = new ProductEntity();
            existingProduct.setImage(null);

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);
            when(fileUploadService.uploadImage(mockNewImage)).thenReturn("http://new-image.com");
            when(productRepo.save(existingProduct)).thenReturn(existingProduct);
            when(productMapper.mapToDto(existingProduct)).thenReturn(new UserProductResponseDto());

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.updateProduct(requestDto, productId, mockNewImage);

            verify(fileUploadService, never()).deleteImage(anyString());
            verify(fileUploadService, times(1)).uploadImage(mockNewImage);
            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingProduct);
            verify(productRepo, times(1)).save(existingProduct);
            verify(mockCache, times(1)).evict(productId);
        }

        /// // Update Product But not found in DB - Throw Exception
        @Test
        void updateProduct_WhenProductIdDoesNotExist_ShouldThrowException() {
            Long invalidId = 888L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(1L);

            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.updateProduct(requestDto, invalidId, null));

            verify(categoryRepo, never()).findByIdOrThrow(anyLong());
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }

        /// // Update Product But category not found in DB - Throw Exception
        @Test
        void updateProduct_WhenNewCategoryDoesNotExist_ShouldThrowException() {
            Long productId = 1L;
            Long invalidCategoryId = 99L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(invalidCategoryId);

            ProductEntity existingProduct = new ProductEntity();

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);
            when(categoryRepo.findByIdOrThrow(invalidCategoryId))
                    .thenThrow(new ResourceNotFoundException("Category", "id", invalidCategoryId));

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.updateProduct(requestDto, productId, null));

            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }

        /// // Restore Product - Should Call Repo Restore
        @Test
        void restoreProduct_ShouldInvokeRepoRestore() {
            Long productId = 1L;

            doNothing().when(productRepo).restoreProduct(productId);

            adminProductService.restoreProduct(productId);

            verify(productRepo, times(1)).restoreProduct(productId);
        }

        @Test
        void restoreProduct_WhenIdDoesNotExist_ShouldThrowException() {
            Long invalidId = 99L;

            doThrow(new ResourceNotFoundException("Product", "id", invalidId))
                    .when(productRepo).restoreProduct(invalidId);

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.restoreProduct(invalidId));

            verify(productRepo, times(1)).restoreProduct(anyLong());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Product Tests (DELETE)")
    class DeleteProductTests {

        /// // Hard Delete - Should delete image from Cloudinary then delete product
        @Test
        void forceDeleteProduct_WhenImageExists_ShouldDeleteImageAndProduct() {
            Long productId = 1L;
            String imageUrl = "http://cloudinary.com/image.jpg";
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("OldTV");

            when(productRepo.findByIdOrThrow(productId)).thenReturn(fakeEntity);
            when(productRepo.getImageUrlByIdEvenIfDeleted(productId)).thenReturn(Optional.of(imageUrl));
            doNothing().when(fileUploadService).deleteImage(imageUrl);
            doNothing().when(productRepo).hardDeleteProduct(productId);

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.forceDeleteProduct(productId);

            verify(productRepo, times(1)).getImageUrlByIdEvenIfDeleted(productId);
            verify(fileUploadService, times(1)).deleteImage(imageUrl);
            verify(productRepo, times(1)).hardDeleteProduct(productId);
            verify(mockCache, times(1)).evict(productId);
            verify(mockCache, times(1)).evict("OldTV");
        }

        /// // Hard Delete - No image - Should only delete product
        @Test
        void forceDeleteProduct_WhenNoImage_ShouldOnlyDeleteProduct() {
            Long productId = 1L;
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("OldTV");

            when(productRepo.findByIdOrThrow(productId)).thenReturn(fakeEntity);
            when(productRepo.getImageUrlByIdEvenIfDeleted(productId)).thenReturn(Optional.empty());
            doNothing().when(productRepo).hardDeleteProduct(productId);

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.forceDeleteProduct(productId);

            verify(fileUploadService, never()).deleteImage(anyString());
            verify(productRepo, times(1)).hardDeleteProduct(productId);
        }

        /// // Hard Delete - When Product ID Does Not Exist - Should Throw Exception
        @Test
        void forceDeleteProduct_WhenProductIdDoesNotExist_ShouldThrowException() {
            Long invalidId = 999L;

            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.forceDeleteProduct(invalidId));

            verify(productRepo, times(1)).findByIdOrThrow(invalidId);
            verify(productRepo, never()).getImageUrlByIdEvenIfDeleted(anyLong());
            verify(fileUploadService, never()).deleteImage(anyString());
            verify(productRepo, never()).hardDeleteProduct(anyLong());
        }

        /// // Soft Delete - When Product Exists - Should Delete Product
        @Test
        void deleteProduct_WhenExists_ShouldDeleteProduct() {
            Long productId = 1L;
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);
            fakeEntity.setName("Phone");

            when(productRepo.findByIdOrThrow(productId)).thenReturn(fakeEntity);
            doNothing().when(productRepo).deleteById(productId);

            Cache mockCache = mock(Cache.class);
            when(cacheManager.getCache("product")).thenReturn(mockCache);

            adminProductService.deleteProduct(productId);

            verify(productRepo, atLeastOnce()).findByIdOrThrow(productId);
            verify(productRepo, times(1)).deleteById(productId);
            verify(mockCache, times(1)).evict(productId);
        }

        /// // Soft Delete - When Id Not Found - Should Throw Exception
        @Test
        void deleteProduct_WhenIdNotFound_ShouldThrowException() {
            Long invalidId = 99L;

            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            assertThrows(ResourceNotFoundException.class, () ->
                    adminProductService.deleteProduct(invalidId));

            verify(productRepo, times(1)).findByIdOrThrow(invalidId);
            verify(productRepo, never()).delete(any(ProductEntity.class));
        }
    }
}