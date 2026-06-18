package com.global.order_api.feature.product;

import com.global.order_api.core.base.PageResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class ProductControllerTest {

    private static final String ENTITY_KEY = "entity.product";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

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
    @DisplayName("1. Get Product Tests (GET)")
    class GetProductTests {

        /// // Find by ID Exists
        @Test
        void getProductsByIdWithCategory_WhenIdExists_ShouldReturnProduct() throws Exception {
            long productId = 1L;
            /// use it as return from service layer
            UserProductResponseDto fakeDto = new UserProductResponseDto();
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

        /// // Find by ID Does Not Exist
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

        /// // Get All Products Page
        @Test
        void getProductsPage_ShouldReturnPagedProducts() throws Exception {
            /// create fake filter
            ProductFilterRequest filter = new ProductFilterRequest();
            filter.setPage(0);
            filter.setSize(10);

            UserProductResponseDto fakeDto = new UserProductResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Laptop");


            Page<UserProductResponseDto> page = new PageImpl<>(List.of(fakeDto));
            /// fake page response mock to be as result from service layer
            PageResponse<UserProductResponseDto> fakePage = PageResponse.from(page, List.of(fakeDto));

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

        @Test
        void getProductStockCount_WhenProductExists_ShouldReturnApiResponseWithStock() throws Exception {
            /// 1=> Setup Data
            Long productId = 1L;
            int expectedStock = 50;
            String expectedMessage = "Stock Count retrieved successfully";

            /// 2=> Mock Dependencies
            when(productService.getProductStockCount(productId)).thenReturn(expectedStock);

            when(appTranslator.getTranslatedAction(eq("success.retrieved"), anyString()))
                    .thenReturn(expectedMessage);

            /// 3=> Perform Request & 4=> Assertions
            mockMvc.perform(get("/api/v1/products/{id}/stock", productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.data").value(expectedStock));

            /// 5=> Verify Mock Interactions
            verify(productService, times(1)).getProductStockCount(productId);
            verify(appTranslator, times(1)).getTranslatedAction(eq("success.retrieved"), anyString());
        }

        /// // Get Product Stock Count - FAIL: Product Not Found
        @Test
        void getProductStockCount_WhenProductDoesNotExist_ShouldReturn404NotFound() throws Exception {
            /// 1=> Setup Data
            Long nonExistingId = 999L;

            /// 2=> Mock Dependencies
            when(productService.getProductStockCount(nonExistingId))
                    .thenThrow(new ResourceNotFoundException("Product", "id", nonExistingId));

            /// 3=> Perform Request & 4=> Assertions
            mockMvc.perform(get("/api/v1/products/{id}/stock", nonExistingId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            /// 5=> Verify Mock Interactions
            verify(productService, times(1)).getProductStockCount(nonExistingId);
            verify(appTranslator, never()).getTranslatedAction(anyString(), anyString());
        }
    }
}