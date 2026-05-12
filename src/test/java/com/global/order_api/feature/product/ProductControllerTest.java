package com.global.order_api.feature.product;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.FileStorageException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;


@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppTranslator appTranslator;


    private static final String ENTITY_KEY = "entity.product";

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Product Tests (GET)")
    class GetProductTests {

        ///// Find by ID Exists
        @Test
        void getProductsByIdWithCategory_WhenIdExists_ShouldReturnProduct() throws Exception {
            long productId = 1L;
            /// use it as return from service layer
            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setId(productId);
            fakeDto.setName("Laptop");

            String fakeMessage = "Product retrieved successfully";

            when(productService.getProductByIdWithCategory(productId)).thenReturn(fakeDto);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.id").value(productId))
                    .andExpect(jsonPath("$.data.name").value("Laptop"));
        }

        ///// Find by ID Does Not Exist
        @Test
        void getProductsByIdWithCategory_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
            long productId = 99L;
            String fakeMessage = "Product with id " + productId + " could not be found.";
            String errorKey = "error.resource.not.found";

            when(productService.getProductByIdWithCategory(productId))
                    .thenThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", productId}));

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Get Product by Name
        @Test
        void getProductByName_WhenExists_ShouldReturnProduct() throws Exception {
            String productName = "TV";
            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName(productName);

            String fakeMessage = "Product retrieved successfully";

            when(productService.getProductByName(productName)).thenReturn(fakeDto);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/products/name/{name}", productName)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(productName));
        }

        ///// Get All Products Page
        @Test
        void getProductsPage_ShouldReturnPagedProducts() throws Exception {
            /// create fake filter
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setPage(0);
            filter.setSize(10);

            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Laptop");


            Page<ProductResponseDto> page = new PageImpl<>(List.of(fakeDto));
            /// fake page response mock to be as result from service layer
            PageResponse<ProductResponseDto> fakePage = PageResponse.from(page, List.of(fakeDto));

            String fakeMessage = "Products retrieved successfully";

            when(productService.getProductsPage(ArgumentMatchers.any(ProductFilterRequest.class)))
                    .thenReturn(fakePage);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/products")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.data[0].name").value("Laptop"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        ///// Get Soft Deleted Products
        @Test
        void getDeletedProducts_ShouldReturnPagedDeletedProducts() throws Exception {
            ProductResponseDto fakeDto = new ProductResponseDto();
            fakeDto.setName("Deleted Laptop");

            PageResponse<ProductResponseDto> fakePage = PageResponse.from(
                    new PageImpl<>(List.of(fakeDto)), List.of(fakeDto)
            );

            String fakeMessage = "Deleted products retrieved successfully";

            when(productService.getDeletedProducts(ArgumentMatchers.any(ProductFilterRequest.class)))
                    .thenReturn(fakePage);
            when(appTranslator.getTranslatedAction(eq("success.deleted_retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/products/deletedProducts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.data[0].name").value("Deleted Laptop"));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Product Tests (POST)")
    class CreateProductTests {

        ///// Create Product - Success
        @Test
        void createProduct_WithValidDataAndImage_ShouldReturnCreated() throws Exception {
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("New Phone");
            requestDto.setCategoryId(2L);
            requestDto.setPrice(BigDecimal.valueOf(15000));
            requestDto.setStockCount(10);

            ProductResponseDto responseDto = new ProductResponseDto();
            responseDto.setId(5L);
            responseDto.setName("New Phone");
            requestDto.setStockCount(10);
            requestDto.setPrice(BigDecimal.valueOf(15000));


            String fakeMessage = "Product created successfully";

            //// because request is multipart request
            /// we need to send json as a data part not a normal body block
            /// "data" => the name specified in controller
            /// ""=> empty because this is a json not file
            ////writeValueAsBytes convert our data to bytes to be send in request
            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json", objectMapper.writeValueAsBytes(requestDto)
            );

            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "phone.jpg", "image/jpeg", "fake_image_bytes".getBytes()
            );

            when(productService.createProduct(ArgumentMatchers.any(ProductRequestDto.class), ArgumentMatchers.any(MultipartFile.class)))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(multipart("/api/v1/products")
                            .file(dataPart)
                            .file(imagePart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.name").value("New Phone"));
        }

        ///// Create Product - Validation Failed (Empty Name)
        @Test
        void createProduct_WithEmptyName_ShouldReturnBadRequest() throws Exception {
            ProductRequestDto invalidRequest = new ProductRequestDto();
            invalidRequest.setName("");
            invalidRequest.setPrice(new BigDecimal("-10"));
            invalidRequest.setStockCount(-5);
            invalidRequest.setCategoryId(null);

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json", objectMapper.writeValueAsBytes(invalidRequest)
            );

            String fakeMessage = "Validation failed.";
            when(appTranslator.translateMessage(eq("error.validation"))).thenReturn(fakeMessage);

            mockMvc.perform(multipart("/api/v1/products")
                            .file(dataPart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////UPDATE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Update Product Tests (PUT)")
    class UpdateProductTests {

        ///// Update Product - Success
        @Test
        void updateProduct_WithValidData_ShouldReturnUpdated() throws Exception {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("Updated Phone");
            requestDto.setPrice(BigDecimal.valueOf(15000));
            requestDto.setCategoryId(1L);
            requestDto.setStockCount(10);


            ProductResponseDto responseDto = new ProductResponseDto();
            responseDto.setId(productId);
            responseDto.setName("Updated Phone");

            String fakeMessage = "Product updated successfully";

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json", objectMapper.writeValueAsBytes(requestDto)
            );

            when(productService.updateProduct(ArgumentMatchers.any(ProductRequestDto.class), eq(productId), ArgumentMatchers.isNull()))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            /// in test we must to send files use POST
            /// and in my controller is PUT
            /// so using with convert POST => PUT
            mockMvc.perform(multipart("/api/v1/products/{id}", productId)
                            .file(dataPart)
                            .with(request -> {
                                request.setMethod(HttpMethod.PUT.name());
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.name").value("Updated Phone"));
        }

        ///// Update Product - Invalid File Type - Should Return BadRequest (Or appropriate error)
        @Test
        void updateProduct_WithTextFileInsteadOfImage_ShouldReturnError() throws Exception {
            Long productId = 1L;

            ProductRequestDto validRequest = new ProductRequestDto();
            validRequest.setName("Valid Phone");
            validRequest.setPrice(new BigDecimal("100"));
            validRequest.setStockCount(10);
            validRequest.setCategoryId(1L);

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json", objectMapper.writeValueAsBytes(validRequest)
            );

            MockMultipartFile invalidFilePart = new MockMultipartFile(
                    "image", "hack.txt", "text/plain", "This is a virus".getBytes()
            );

            String fakeErrorMessage = "Invalid file format. Only images are allowed.";

            when(productService.updateProduct(ArgumentMatchers.any(), eq(productId), ArgumentMatchers.any()))
                    .thenThrow(new FileStorageException(fakeErrorMessage));

            when(appTranslator.translateMessage(ArgumentMatchers.anyString()))
                    .thenReturn(fakeErrorMessage);

            // 2. Act & Assert
            mockMvc.perform(multipart("/api/v1/products/{id}", productId)
                            .file(dataPart)
                            .file(invalidFilePart)
                            .with(request -> {
                                request.setMethod(HttpMethod.PUT.name());
                                return request;
                            })
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Product Tests (DELETE & RESTORE)")
    class DeleteProductTests {

        ///// Soft Delete
        @Test
        void deleteProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product deleted successfully";

            doNothing().when(productService).deleteProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Soft Delete - When Id Not Found - Should Return NotFound (404)
        @Test
        void deleteProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(productService).deleteProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            // 2. Act & Assert
            mockMvc.perform(delete("/api/v1/products/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Hard Delete
        @Test
        void forceDeleteProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product permanently deleted";

            doNothing().when(productService).forceDeleteProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/products/{id}/force", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Hard Delete - When Id Not Found - Should Return NotFound (404)
        @Test
        void forceDeleteProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            // 1. Arrange
            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(productService).forceDeleteProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            // 2. Act & Assert
            mockMvc.perform(delete("/api/v1/products/{id}/force", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Restore Product
        @Test
        void restoreProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product restored successfully";

            doNothing().when(productService).restoreProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.restored"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/products/{id}/restore", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        ///// Restore Product - When Id Not Found - Should Return NotFound (404)
        @Test
        void restoreProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(productService).restoreProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            // 2. Act & Assert
            mockMvc.perform(put("/api/v1/products/{id}/restore", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }
    }


}