package com.global.order_api.feature.product;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.FileStorageException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class AdminProductControllerTest {

    private static final String ENTITY_KEY = "entity.product";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProductService adminProductService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppTranslator appTranslator;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Admin Product Tests (GET)")
    class GetAdminProductTests {

        /// // Get Admin Products Page
        @Test
        void getAdminProductsPage_ShouldReturnPagedProducts() throws Exception {
            /// create fake filter
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setPage(0);
            filter.setSize(10);

            AdminProductResponseDto fakeDto = new AdminProductResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Laptop");
            fakeDto.setStockCount(50);

            Page<AdminProductResponseDto> page = new PageImpl<>(List.of(fakeDto));
            /// fake page response mock to be as result from service layer
            PageResponse<AdminProductResponseDto> fakePage = PageResponse.from(page, List.of(fakeDto));

            String fakeMessage = "Products retrieved successfully";

            when(adminProductService.getAdminProductsPage(ArgumentMatchers.any(ProductFilterRequest.class)))
                    .thenReturn(fakePage);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/admin/products")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.data[0].name").value("Laptop"))
                    .andExpect(jsonPath("$.data.data[0].stockCount").value(50))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        /// // Get Soft Deleted Products
        @Test
        void getDeletedProducts_ShouldReturnPagedDeletedProducts() throws Exception {
            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setName("Deleted Laptop");

            PageResponse<UserProductResponseDto> fakePage = PageResponse.from(
                    new PageImpl<>(List.of(fakeDto)), List.of(fakeDto)
            );

            String fakeMessage = "Deleted products retrieved successfully";

            when(adminProductService.getDeletedProducts(ArgumentMatchers.any(ProductFilterRequest.class)))
                    .thenReturn(fakePage);
            when(appTranslator.getTranslatedAction(eq("success.deleted_retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/admin/products/deleted")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sortDirection", "DESC")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.data[0].name").value("Deleted Laptop"));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Product Tests (POST)")
    class CreateProductTests {

        /// // Create Product - Success
        @Test
        void createProduct_WithValidDataAndImage_ShouldReturnCreated() throws Exception {
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("New Phone");
            requestDto.setCategoryId(2L);
            requestDto.setPrice(BigDecimal.valueOf(15000));
            requestDto.setStockCount(10);

            UserProductResponseDto responseDto = new UserProductResponseDto();
            responseDto.setId(5L);
            responseDto.setName("New Phone");

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

            when(adminProductService.createProduct(ArgumentMatchers.any(ProductRequestDto.class), ArgumentMatchers.any(MultipartFile.class)))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(multipart("/api/v1/admin/products")
                            .file(dataPart)
                            .file(imagePart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.name").value("New Phone"));
        }

        /// // Create Product - Validation Failed (Empty Name)
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

            mockMvc.perform(multipart("/api/v1/admin/products")
                            .file(dataPart)
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists());
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////UPDATE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Update Product Tests (PUT)")
    class UpdateProductTests {

        /// // Update Product - Success
        @Test
        void updateProduct_WithValidData_ShouldReturnUpdated() throws Exception {
            Long productId = 1L;
            ProductRequestDto requestDto = new ProductRequestDto();
            requestDto.setName("Updated Phone");
            requestDto.setPrice(BigDecimal.valueOf(15000));
            requestDto.setCategoryId(1L);
            requestDto.setStockCount(10);

            UserProductResponseDto responseDto = new UserProductResponseDto();
            responseDto.setId(productId);
            responseDto.setName("Updated Phone");

            String fakeMessage = "Product updated successfully";

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json", objectMapper.writeValueAsBytes(requestDto)
            );

            when(adminProductService.updateProduct(ArgumentMatchers.any(ProductRequestDto.class), eq(productId), ArgumentMatchers.isNull()))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            /// in test we must to send files use POST
            /// and in my controller is PUT
            /// so using with convert POST => PUT
            mockMvc.perform(multipart("/api/v1/admin/products/{id}", productId)
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

        /// // Update Product - Invalid File Type - Should Return BadRequest (Or appropriate error)
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

            String errorKey = "error.invalid.file.type";
            String fakeErrorMessage = "Invalid file format. Only images are allowed.";

            when(adminProductService.updateProduct(ArgumentMatchers.any(), eq(productId), ArgumentMatchers.any()))
                    .thenThrow(new FileStorageException(fakeErrorMessage));

            /// lenient() => because if this method doesn't call not throw an EX
            lenient().when(appTranslator.translateMessage(anyString()))
                    .thenReturn(fakeErrorMessage);
            lenient().when(appTranslator.translateMessage(anyString(), ArgumentMatchers.any()))
                    .thenReturn(fakeErrorMessage);
            lenient().when(appTranslator.translateMessage(anyString(), ArgumentMatchers.isNull()))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(multipart("/api/v1/admin/products/{id}", productId)
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

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Product Tests (DELETE & RESTORE)")
    class DeleteProductTests {

        /// // Soft Delete
        @Test
        void deleteProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product deleted successfully";

            doNothing().when(adminProductService).deleteProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Soft Delete - When Id Not Found - Should Return NotFound (404)
        @Test
        void deleteProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(adminProductService).deleteProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/products/{id}", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Hard Delete
        @Test
        void forceDeleteProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product permanently deleted";

            doNothing().when(adminProductService).forceDeleteProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/products/{id}/force", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Hard Delete - When Id Not Found - Should Return NotFound (404)
        @Test
        void forceDeleteProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(adminProductService).forceDeleteProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/products/{id}/force", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Restore Product
        @Test
        void restoreProduct_ShouldReturnOk() throws Exception {
            Long productId = 1L;
            String fakeMessage = "Product restored successfully";

            doNothing().when(adminProductService).restoreProduct(productId);
            when(appTranslator.getTranslatedAction(eq("success.restored"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/admin/products/{id}/restore", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Restore Product - When Id Not Found - Should Return NotFound (404)
        @Test
        void restoreProduct_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidId = 99L;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Product with id " + invalidId + " could not be found.";

            doThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidId}))
                    .when(adminProductService).restoreProduct(invalidId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/admin/products/{id}/restore", invalidId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) // 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }
    }
}