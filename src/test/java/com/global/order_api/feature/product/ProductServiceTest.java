package com.global.order_api.feature.product;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.service.FileUploadService;
import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.category.CategoryRepo;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

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
    private CategoryRepo categoryRepo;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private ProductService productService;


    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Product Tests (GET)")
    class GetProductTests {

        ///// Find by ID Exists - RETURN PRODUCT
        @Test
        void getProductByIdWithCategory_WhenExists_ShouldReturnProduct() {
            Long id = 1L;
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(id);
            fakeEntity.setName("Laptop");

            /// Note: No need to mock Category object here.
            /// The Database JOIN is already verified in the Repo layer tests.
            /// This Service Unit Test only verifies the delegation to Repo and Mapper.

            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setId(id);
            fakeDto.setName("Laptop");

            when(productRepo.findByIdWithCategory(id)).thenReturn(Optional.of(fakeEntity));
            when(productMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

            ProductResponseDto result = productService.getProductByIdWithCategory(id);

            assertNotNull(result);
            assertEquals("Laptop", result.getName());
            verify(productRepo, times(1)).findByIdWithCategory(id);
            verify(productMapper, times(1)).mapToDto(fakeEntity);
        }

        ///// Find by ID Does not Exist - NOT FOUND
        @Test
        void getProductByIdWithCategory_WhenIdNotFound_ShouldThrowException() {
            Long invalidId = 999L;

            when(productRepo.findByIdWithCategory(invalidId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getProductByIdWithCategory(invalidId));

            verify(productRepo, times(1)).findByIdWithCategory(invalidId);
            verify(productMapper, never()).mapToDto(any());
        }

        ///// Find by Name Exists - RETURN PRODUCT
        @Test
        void getProductByName_WhenExists_ShouldReturnProduct() {
            String productName = "Apple iPhone";

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName(productName);

            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName(productName);

            when(productRepo.findByName(productName)).thenReturn(Optional.of(fakeEntity));
            when(productMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

            ProductResponseDto result = productService.getProductByName(productName);

            assertNotNull(result);
            assertEquals(productName, result.getName());

            verify(productRepo, times(1)).findByName(productName);
            verify(productMapper, times(1)).mapToDto(fakeEntity);
        }

        ///// Find by Name Does not Exist - NOT FOUND
        @Test
        void getProductByName_WhenNameNotFound_ShouldThrowException() {
            String invalidName = "Unknown Product";

            when(productRepo.findByName(invalidName)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.getProductByName(invalidName));

            verify(productRepo, times(1)).findByName(invalidName);
            verify(productMapper, never()).mapToDto(any());
        }

        ///// Get Products Page with Filters - RETURN PAGE RESPONSE
        @Test
        void getProductsPage_ShouldReturnPagedProducts() {
            // 1. create fake filter
            ProductFilterRequest filter = new ProductFilterRequest();
            // 2. create Fake products
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Phone");

            // 3. create fake page contains products to use it to be the return from repo
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setName("Phone");

            /// specification => is Lambda Expression
            /// so in Java any 2 Lambdas are not same even if they have same content
            /// and when() use equals() which compares addresses
            /// so if i built Specification 1 then service code will create Specification 2 (not the same)
            /// and important thing => we don't have to test spec here
            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            // 4. Act
            PageResponse<ProductResponseDto> result = productService.getProductsPage(filter);

            // 5. Assert
            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            assertEquals("Phone", result.getData().get(0).getName());

            // 6. Verify
            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }

        ///// Get Products Page with Filters but DB is Empty - RETURN EMPTY PAGE RESPONSE
        @Test
        void getProductsPage_WhenNoProducts_ShouldReturnEmptyPage() {
            // 1. Arrange Filter Request
            ProductFilterRequest filter = new ProductFilterRequest();


            // 2. create empty page returned from repo layer
            Page<ProductEntity> emptyMockPage = new PageImpl<>(List.of());

            when(productRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);

            /// mapper should return empty list
            when(productMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            // 3. Act
            PageResponse<ProductResponseDto> result = productService.getProductsPage(filter);

            // 4. Assert
            assertNotNull(result);
            assertTrue(result.getData().isEmpty()); /// check empty page is true
            assertEquals(0, result.getTotalElements()); /// check meta data

            // 5. Verify
            verify(productRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(productMapper, times(1)).mapToDtoList(emptyMockPage.getContent());
        }

        ///// Get Deleted Products Page with Filters - RETURN PAGE RESPONSE
        @Test
        void getDeletedProducts_ShouldReturnPagedDeletedProducts() {
            // 1. Arrange Filter Request
            ProductFilterRequest filter = new ProductFilterRequest();

            // 2. Arrange Fake Data
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setName("Deleted Phone");
            fakeEntity.setDeleted(true);
            Page<ProductEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setName("Deleted Phone");


            when(productRepo.findAllDeletedProducts(any(Specification.class), any(Pageable.class)))
                    .thenReturn(mockEntityPage);
            when(productMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            // 3. Act
            PageResponse<ProductResponseDto> result = productService.getDeletedProducts(filter);

            // 4. Assert
            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            assertEquals("Deleted Phone", result.getData().get(0).getName());

            // 5. Verify
            verify(productRepo, times(1)).findAllDeletedProducts(any(Specification.class), any(Pageable.class));
            verify(productRepo, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        ///// Get Deleted Products Page with Filters - RETURN EMPTY PAGE RESPONSE
        @Test
        void getDeletedProductsPage_WhenNoProducts_ShouldReturnEmptyPage() {
            // 1. Arrange Filter Request
            ProductFilterRequest filter = new ProductFilterRequest();

            // 2. create empty page returned from repo layer
            Page<ProductEntity> emptyMockPage = new PageImpl<>(List.of());

            when(productRepo.findAllDeletedProducts(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);

            /// mapper should return empty list
            when(productMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            // 3. Act
            PageResponse<ProductResponseDto> result = productService.getDeletedProducts(filter);
            // 4. Assert
            assertNotNull(result);
            assertTrue(result.getData().isEmpty()); /// check empty page is true
            assertEquals(0, result.getTotalElements()); /// check meta data

            // 5. Verify
            verify(productRepo, times(1)).findAllDeletedProducts(any(Specification.class), any(Pageable.class));
            verify(productMapper, times(1)).mapToDtoList(emptyMockPage.getContent());
        }

        //////////////// GET PRODUCT STOCK COUNT States /////////////
        ///// Get Product Stock Count - SUCCESS
        @Test
        void getProductStockCount_WhenProductExists_ShouldReturnStockCount() {
            /// 1=> Setup Data
            Long productId = 1L;
            int expectedStock = 50;

            /// 2=> Mocking the Repo
            when(productRepo.findStockCountById(productId)).thenReturn(Optional.of(expectedStock));

            /// 3=> Test our function
            int result = productService.getProductStockCount(productId);

            /// 4=> Assert & Verify
            assertEquals(expectedStock, result);

            verify(productRepo, times(1)).findStockCountById(productId);
        }

        ///// Get Product Stock Count - FAIL: Product Not Found
        @Test
        void getProductStockCount_WhenProductDoesNotExist_ShouldThrowResourceNotFoundException() {
            /// 1=> Setup Data
            Long nonExistingId = 999L;

            /// 2=> Mocking the Repo
            when(productRepo.findStockCountById(nonExistingId)).thenReturn(Optional.empty());

            /// 3=> Test our function & Assert Exception
            assertThrows(ResourceNotFoundException.class, () -> {
                productService.getProductStockCount(nonExistingId);
            });

            /// 4=> Verify Interactions
            verify(productRepo, times(1)).findStockCountById(nonExistingId);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Product Tests (POST)")
    class CreateProductTests {

        ///// Create Product WITH Image - RETURN CREATED PRODUCT
        @Test
        void createProduct_WithImage_ShouldUploadImageAndSaveProduct() {
            // 1. Arrange Request & Mocks
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("New TV");
            requestDto.setCategoryId(2L);

            /// MockMultipartFile => to mock that => user upload an image without open postman
            /// and upload real image
            /// take 4 parameters is the same parameters which in HTTP REQUEST
            /// image is parameter name in controller layer
            /// tv.jpg => image name
            /// image/jpeg => MIME TYPE to make server understands that file is an image
            /// "fake image data".getBytes => content of image (bytes 0 , 1 )
            /// so make "fake image data" is a random test and convert it into bytes
            /// to make service see it a real file
            MultipartFile mockImage = new MockMultipartFile(
                    "image", "tv.jpg", "image/jpeg", "fake image data".getBytes()
            );

            CategoryEntity category = new CategoryEntity();
            category.setId(2L);

            ProductEntity mappedEntity = new ProductEntity();
            mappedEntity.setName("New TV");

            ProductResponseDto responseDto = new ProductResponseDto();
            responseDto.setName("New TV");

            // Mock behaviors
            when(categoryRepo.findByIdOrThrow(2L)).thenReturn(category);
            when(productMapper.mapToEntity(requestDto)).thenReturn(mappedEntity);
            /// fake url for testing uploading image
            when(fileUploadService.uploadImage(mockImage)).thenReturn("http://cloudinary.com/tv.jpg");
            /// using any() => because service create new entity in her internal code
            /// so addresses are different
            when(productRepo.save(any(ProductEntity.class))).thenReturn(mappedEntity);
            when(productMapper.mapToDto(mappedEntity)).thenReturn(responseDto);

            // 2. Act
            ProductResponseDto result = productService.createProduct(requestDto, mockImage);

            // 3. Assert & Verify
            assertNotNull(result);
            assertEquals("New TV", result.getName());

            verify(categoryRepo, times(1)).findByIdOrThrow(2L);
            verify(fileUploadService, times(1)).uploadImage(mockImage);
            verify(productRepo, times(1)).save(mappedEntity);
        }

        ///// Create Product WITHOUT Image - RETURN CREATED PRODUCT
        @Test
        void createProduct_WithoutImage_ShouldSaveProductWithoutUpload() {
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(2L);
            CategoryEntity category = new CategoryEntity();

            when(categoryRepo.findByIdOrThrow(2L)).thenReturn(category);
            when(productMapper.mapToEntity(requestDto)).thenReturn(new ProductEntity());
            when(productRepo.save(any(ProductEntity.class))).thenReturn(new ProductEntity());
            when(productMapper.mapToDto(any())).thenReturn(new ProductResponseDto());

            // Act with null image
            productService.createProduct(requestDto, null);

            // Verify uploadImage was NEVER called
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, times(1)).save(any(ProductEntity.class));
        }

        ///// Create Product - Category Not Found - Should Throw Exception
        @Test
        void createProduct_WhenCategoryDoesNotExist_ShouldThrowException() {
            // 1. Arrange
            Long invalidCategoryId = 99L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(invalidCategoryId);

            MultipartFile mockImage = new MockMultipartFile(
                    "image", "tv.jpg", "image/jpeg", "data".getBytes()
            );

            when(categoryRepo.findByIdOrThrow(invalidCategoryId))
                    .thenThrow(new ResourceNotFoundException("Category", "id", invalidCategoryId));

            // 2. Act & Assert
            assertThrows(ResourceNotFoundException.class, () ->
                    productService.createProduct(requestDto, mockImage));

            // 3. Verify
            verify(categoryRepo, times(1)).findByIdOrThrow(invalidCategoryId);

            /// check image state
            verify(productMapper, never()).mapToEntity(any());
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////UPDATE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Update Product Tests (PUT)")
    class UpdateProductTests {

        ///// Update Product WITH New Image (Deletes old image) - RETURN UPDATED PRODUCT
        @Test
        void updateProduct_WithNewImage_ShouldDeleteOldAndUploadNew() {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(5L);

            MultipartFile mockNewImage = new MockMultipartFile(
                    "image", "new.jpg", "image/jpeg", "data".getBytes()
            );

            /// old product in DB
            ProductEntity existingEntity = new ProductEntity();
            existingEntity.setImage("http://old-image-url.com"); // Has old image

            CategoryEntity newCategory = new CategoryEntity();

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingEntity);
            when(categoryRepo.findByIdOrThrow(5L)).thenReturn(newCategory);

            /// return new image and get image url
            doNothing().when(fileUploadService).deleteImage("http://old-image-url.com");
            when(fileUploadService.uploadImage(mockNewImage)).thenReturn("http://new-image-url.com");

            when(productRepo.save(existingEntity)).thenReturn(existingEntity);
            //// new ProductResponseDto => for simplicity and we don't have to check return result
            when(productMapper.mapToDto(existingEntity)).thenReturn(new ProductResponseDto());

            productService.updateProduct(requestDto, productId, mockNewImage);

            verify(fileUploadService, times(1)).deleteImage("http://old-image-url.com");
            verify(fileUploadService, times(1)).uploadImage(mockNewImage);
            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingEntity);
            verify(productRepo, times(1)).save(existingEntity);
        }

        ///// Update Product without changing image - Product Updated
        @Test
        void updateProduct_WithoutNewImage_ShouldOnlyUpdateFields() {
            // 1. Arrange
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("Updated Name Only");
            requestDto.setCategoryId(1L);

            ProductEntity existingProduct = new ProductEntity();
            existingProduct.setImage("http://old-image.com");

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);
            when(productRepo.save(existingProduct)).thenReturn(existingProduct);
            when(productMapper.mapToDto(existingProduct)).thenReturn(new ProductResponseDto());

            // 2. Act: بنبعت الصورة null
            productService.updateProduct(requestDto, productId, null);

            // 3. Assert & Verify
            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingProduct);

            verify(fileUploadService, never()).deleteImage(anyString());
            verify(fileUploadService, never()).uploadImage(any());

            verify(productRepo, times(1)).save(existingProduct);
        }

        ///// Update Product(without image) with uploading image  - Product Updated
        @Test
        void updateProduct_WithNewImageWhenNoOldImageExists_ShouldOnlyUploadNewImage() {
            // 1. Arrange
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(1L);


            MultipartFile mockNewImage = new MockMultipartFile(
                    "image", "new_pic.jpg", "image/jpeg", "data".getBytes()
            );
            /// old product without image
            ProductEntity existingProduct = new ProductEntity();
            existingProduct.setImage(null);


            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);

            when(fileUploadService.uploadImage(mockNewImage)).thenReturn("http://new-image.com");
            when(productRepo.save(existingProduct)).thenReturn(existingProduct);
            when(productMapper.mapToDto(existingProduct)).thenReturn(new ProductResponseDto());

            // 2. Act
            productService.updateProduct(requestDto, productId, mockNewImage);

            // 3. Verify
            //// very important  delete image never called in our test scenario
            verify(fileUploadService, never()).deleteImage(anyString());

            verify(fileUploadService, times(1)).uploadImage(mockNewImage);
            verify(productMapper, times(1)).updateEntityFromDto(requestDto, existingProduct);
            verify(productRepo, times(1)).save(existingProduct);
        }

        ///// Update Product But not found in DB - Throw Exception
        @Test
        void updateProduct_WhenProductIdDoesNotExist_ShouldThrowException() {
            // 1. Arrange
            Long invalidId = 888L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(1L);


            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            // 2. Act & Assert
            assertThrows(ResourceNotFoundException.class, () ->
                    productService.updateProduct(requestDto, invalidId, null));

            // 3. Verify
            verify(categoryRepo, never()).findByIdOrThrow(anyLong());
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }

        ///// Update Product But category not found in DB - Throw Exception
        @Test
        void updateProduct_WhenNewCategoryDoesNotExist_ShouldThrowException() {
            // 1. Arrange
            Long productId = 1L;
            Long invalidCategoryId = 99L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setCategoryId(invalidCategoryId);

            ProductEntity existingProduct = new ProductEntity();

            when(productRepo.findByIdOrThrow(productId)).thenReturn(existingProduct);
            when(categoryRepo.findByIdOrThrow(invalidCategoryId))
                    .thenThrow(new ResourceNotFoundException("Category", "id", invalidCategoryId));

            // 2. Act & Assert
            assertThrows(ResourceNotFoundException.class, () ->
                    productService.updateProduct(requestDto, productId, null));

            // 3. Verify
            verify(fileUploadService, never()).uploadImage(any());
            verify(productRepo, never()).save(any());
        }

        ///// Restore Product - Should Call Repo Restore
        ///// Restore Product - Should Call Repo Restore
        @Test
        void restoreProduct_ShouldInvokeRepoRestore() {
            Long productId = 1L;

            doNothing().when(productRepo).restoreProduct(productId);

            // 1. Act
            productService.restoreProduct(productId);

            // 2. Assert & Verify
            verify(productRepo, times(1)).restoreProduct(productId);
        }

        @Test
        void restoreProduct_WhenIdDoesNotExist_ShouldThrowException() {
            Long invalidId = 99L;

            doThrow(new ResourceNotFoundException("Product", "id", invalidId))
                    .when(productRepo).restoreProduct(invalidId);

            assertThrows(ResourceNotFoundException.class, () ->
                    productService.restoreProduct(invalidId));

            verify(productRepo, times(1)).restoreProduct(anyLong());
        }

        //////////////// REDUCE STOCK States /////////////
        ///// Reduce Stock - SUCCESS
        @Test
        void reduceStock_WhenExistsAndStockSufficient_ShouldReduceAndSave() {
            /// 1=> Setup Data
            Long productId = 1L;
            int requestedQuantity = 2;

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);
            fakeEntity.setName("Laptop");
            fakeEntity.setStockCount(5);

            /// 2=> Mocking the Repo
            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.of(fakeEntity));

            /// 3=> Test our function
            productService.reduceStock(productId, requestedQuantity);

            /// 4=> Assert & Verify
            assertEquals(3, fakeEntity.getStockCount());

            verify(productRepo, times(1)).findByIdForUpdate(productId);

            verify(productRepo, times(1)).save(fakeEntity);
        }

        ///// Reduce Stock - FAIL: Insufficient Stock
        @Test
        void reduceStock_WhenRequestedQuantityGreaterThanStock_ShouldThrowBusinessLogicException() {
            /// 1=> Setup Data
            Long productId = 1L;
            int requestedQuantity = 10;

            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);
            fakeEntity.setStockCount(5);

            /// 2=> Mocking the Repo
            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.of(fakeEntity));

            /// 3=> Test our function & Assert Exception
            assertThrows(BusinessLogicException.class, () -> {
                productService.reduceStock(productId, requestedQuantity);
            });

            /// 4=> Verify Interactions
            verify(productRepo, times(1)).findByIdForUpdate(productId);
            verify(productRepo, never()).save(any());
        }

        ///// Reduce Stock - FAIL: Product Not Found
        @Test
        void reduceStock_WhenProductNotFound_ShouldThrowResourceNotFoundException() {
            /// 1=> Setup Data
            Long productId = 999L;
            int requestedQuantity = 2;

            /// 2=> Mocking the Repo
            when(productRepo.findByIdForUpdate(productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                productService.reduceStock(productId, requestedQuantity);
            });

            /// 4=> Verify Interactions
            verify(productRepo, times(1)).findByIdForUpdate(productId);
            verify(productRepo, never()).save(any());
        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Product Tests (DELETE)")
    class DeleteProductTests {

        ///// Hard Delete - Should delete image from Cloudinary then delete product
        @Test
        void forceDeleteProduct_WhenImageExists_ShouldDeleteImageAndProduct() {
            Long productId = 1L;
            /// fake url
            String imageUrl = "http://cloudinary.com/image.jpg";

            // Mock repo to return image URL inside Optional
            when(productRepo.getImageUrlByIdEvenIfDeleted(productId)).thenReturn(Optional.of(imageUrl));
            doNothing().when(fileUploadService).deleteImage(imageUrl);
            doNothing().when(productRepo).hardDeleteProduct(productId);

            // Act
            productService.forceDeleteProduct(productId);

            // Verify
            verify(productRepo, times(1)).getImageUrlByIdEvenIfDeleted(productId);
            verify(fileUploadService, times(1)).deleteImage(imageUrl); // Confirm image was deleted
            verify(productRepo, times(1)).hardDeleteProduct(productId);
        }

        ///// Hard Delete - No image - Should only delete product
        @Test
        void forceDeleteProduct_WhenNoImage_ShouldOnlyDeleteProduct() {
            Long productId = 1L;

            // Mock repo to return empty Optional => product without image
            when(productRepo.getImageUrlByIdEvenIfDeleted(productId)).thenReturn(Optional.empty());
            doNothing().when(productRepo).hardDeleteProduct(productId);

            // Act
            productService.forceDeleteProduct(productId);

            // Verify
            verify(fileUploadService, never()).deleteImage(anyString()); // Ensure no delete image call
            verify(productRepo, times(1)).hardDeleteProduct(productId);
        }

        ///// Hard Delete - When Product ID Does Not Exist - Should Throw Exception
        @Test
        void forceDeleteProduct_WhenProductIdDoesNotExist_ShouldThrowException() {
            // 1. Arrange
            Long invalidId = 999L;

            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            // 2. Act & Assert
           assertThrows(ResourceNotFoundException.class, () ->
                    productService.forceDeleteProduct(invalidId));

            // 3. Verify
            verify(productRepo, times(1)).findByIdOrThrow(invalidId);
            verify(productRepo, never()).getImageUrlByIdEvenIfDeleted(anyLong());
            verify(fileUploadService, never()).deleteImage(anyString());
            verify(productRepo, never()).hardDeleteProduct(anyLong());
        }

        ///// Soft Delete - When Product Exists - Should Delete Product
        @Test
        void deleteProduct_WhenExists_ShouldDeleteProduct() {
            // 1. Arrange
            Long productId = 1L;
            ProductEntity fakeEntity = new ProductEntity();
            fakeEntity.setId(productId);

            when(productRepo.findByIdOrThrow(productId)).thenReturn(fakeEntity);
            doNothing().when(productRepo).deleteById(productId);
            productService.deleteProduct(productId);

            /// product service constructor take base repo
            /// and category repo and product repo extends base repo
            /// so Mockito use reflection and choose random one and if he use category repo will cause an error
            verify(productRepo, times(1)).findByIdOrThrow(productId);
            verify(productRepo, times(1)).deleteById(productId);
        }

        ///// Soft Delete - When Id Not Found - Should Throw Exception
        @Test
        void deleteProduct_WhenIdNotFound_ShouldThrowException() {
            // 1. Arrange
            Long invalidId = 99L;

            when(productRepo.findByIdOrThrow(invalidId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", invalidId));

            // 2. Act & Assert
            assertThrows(ResourceNotFoundException.class, () ->
                    productService.deleteProduct(invalidId));

            // 3. Verify
            verify(productRepo, times(1)).findByIdOrThrow(invalidId);
            verify(productRepo, never()).delete(any(ProductEntity.class));
        }
    }
}