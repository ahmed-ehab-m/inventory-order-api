package com.global.order_api.feature.category;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// make slicing testing => turn on only web layer and ignore other layers
/// tell spring we will test this controller to save memory of other controllers
@WebMvcTest(value = AdminCategoryController.class,
        /// exclude security layer
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class AdminCategoryControllerTest {

    /// like postman to make test requests to our endpoints
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCategoryService adminCategoryService;

    /// objectMapper is the primary class internal jackson library
    /// he is responsible for translating
    /// so MockMvc doesn't know how to send Java object
    /// so we send request in json format
    /// so we call objectMapper to make Serialization like {"name":"labtop"}
    @Autowired
    private ObjectMapper objectMapper;
    /// translator from java/json to json/java
    @MockitoBean
    private AppTranslator appTranslator;

    /// Mock redis
    /// becausr our test request will enter rate limiter interceptor
    /// then interceptor call redis
    /// and we in test environment so result is error
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        /// mock this first request
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    /// ////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Admin Category Tests (GET)")
    class GetAdminCategoryTests {

        /// ////////////// GET ALL CATEGORIES (Paginated)/////////////
        @Test
        void findAllCategories_ShouldReturnAllCategoriesWithSuccessMessage() throws Exception {
            /// 1=> create fake user filter
            CategoryFilterRequestDto filterRequestDto = new CategoryFilterRequestDto();
            filterRequestDto.setPage(0);
            filterRequestDto.setSize(10);

            /// 2=> mock the return from service PageResponse<CategoryResponseDto>
            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setId(1L);
            fakeResponseDto.setName("Books");

            List<CategoryResponseDto> dtos = List.of(fakeResponseDto);

            Page<CategoryResponseDto> page = new PageImpl<>(dtos);
            PageResponse<CategoryResponseDto> fakePage = PageResponse.from(
                    page, dtos
            );

            /// 3=> give our scenario to our mocks
            when(adminCategoryService.getCategoriesPage(ArgumentMatchers.any(CategoryFilterRequestDto.class)))
                    .thenReturn(fakePage);

            /// expected Message
            String fakeMessage = "Category retrieved successfully";
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), anyString()))
                    .thenReturn(fakeMessage);

            /// 4=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/admin/categories")
                            /// Query params not body
                            .param("page", String.valueOf(filterRequestDto.getPage()))
                            .param("size", String.valueOf(filterRequestDto.getSize()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    /// go to data to find id and name fields
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.data[0].id").value(1))
                    .andExpect(jsonPath("$.data.data[0].name").value("Books"))
                    /// test pagination
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.currentPage").value(0));
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Category Tests (POST)")
    class CreateCategoryTest {
        /// // Create Category with new name - Category Created
        @Test
        void createCategory_WhenNameDoesNotExist_ShouldReturnCategoryWithSuccessMessage() throws Exception {
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName("Mobiles");

            /// 2=> create fake response from service
            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setName("Mobiles");
            fakeResponseDto.setId(7L);
            /// 3=> expected Message
            String fakeMessage = "Category created successfully";

            /// 4=> give our scenario to our mocks
            when(adminCategoryService.createCategory(fakeRequestDto)).thenReturn(fakeResponseDto);

            when(appTranslator.getTranslatedAction(eq("success.created"), eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(post("/api/v1/admin/categories")
                            /// convert dto to json
                            /// content add it into body of http request
                            /// in controller endpoint (@RequestBody) so jackson make Deserialization
                            .content(objectMapper.writeValueAsString(fakeRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isCreated())
                    /// check our message
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Create Category with another category name - Category NOT Created
        @Test
        void createCategory_WhenNameExists_ShouldReturnNotCreated() throws Exception {
            String categoryName = "Books";
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(categoryName);

            /// 2=> expected Message
            String errorKey = "error.resource.duplicate";
            String fakeMessage = "Category with name " + categoryName + " already exists.";

            /// 4=> give our scenario to our mocks
            /// problem here => after serialization controller make new object with these data
            /// and mockito compare addresses no values
            /// so using any
            when(adminCategoryService.createCategory(ArgumentMatchers.any(CategoryRequestDto.class))).thenThrow(
                    new DuplicateRecordException("Category", "name", categoryName)
            );

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(post("/api/v1/admin/categories")
                            /// convert dto to json
                            /// content add it into body of http request
                            /// in controller endpoint (@RequestBody) so jackson make Deserialization
                            .content(objectMapper.writeValueAsString(fakeRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());
        }

        /// // Create Category with empty name - Category NOT Created
        @Test
        void createCategory_WhenNameIsEmpty_ShouldReturnBadRequest() throws Exception {

            CategoryRequestDto invalidRequestDto = new CategoryRequestDto();
            invalidRequestDto.setName(""); /// empty name
            /// request will not go to service layer because we use @Valid
            String fakeMessage = "Validation failed.";
            String errorKey = "error.validation";
            when(appTranslator.translateMessage(eq(errorKey)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/admin/categories")
                            .content(objectMapper.writeValueAsString(invalidRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest()) /// 400
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.timeStamp").exists());
        }
    }

    /// ////////////////////////////////////////
    @Nested
    @DisplayName("3. Update Category Tests (PUT)")
    class UpdateCategoryTest {
        /// // Update Category with same or Different name - Category Updated
        @Test
        void updateCategory_WhenSameOrDifferentName_ShouldReturnCategoryWithSuccessMessage() throws Exception {
            String categoryNameExists = "Books";
            Long categoryId = 1L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(categoryNameExists);
            fakeRequestDto.setDescription("new description");

            /// 2=> create fake response from service
            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setName("Mobiles");
            fakeResponseDto.setId(categoryId);
            fakeResponseDto.setDescription("new description");
            /// 3=> expected Message
            String fakeMessage = "Category updated successfully";
            /// 4=> give our scenario to our mocks
            when(adminCategoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenReturn(fakeResponseDto);

            when(appTranslator.getTranslatedAction(eq("success.updated"), eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                            /// convert dto to json
                            /// content add it into body of http request
                            /// in controller endpoint (@RequestBody) so jackson make Deserialization
                            .content(objectMapper.writeValueAsString(fakeRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        /// // Update Category But Id Not Found - Category Not Updated
        @Test
        void updateCategory_WhenIdNotFound_ShouldReturnNotFound() throws Exception {
            String newCategoryName = "TVs";
            Long categoryId = 11L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(newCategoryName);
            String fakeMessage = "Category with id " + categoryId + " could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args = new Object[]{"id", categoryId};

            /// 2=> give our scenario to our mocks
            when(adminCategoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenThrow(
                    new ResourceNotFoundException(errorKey, args)
            );
            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                            .content(objectMapper.writeValueAsString(fakeRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print()) /// to see details in console when test passed
            /// check our return ApiResponse
            /// check status code
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());
        }

        /// // Update Category with another category name - Category Not Updated
        @Test
        void updateCategory_WhenNameAlreadyExists_ShouldReturnNotUpdated() throws Exception {
            String newCategoryName = "Books";
            Long categoryId = 2L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(newCategoryName);
            String errorKey = "error.resource.duplicate";
            String fakeMessage = "Category with name " + newCategoryName + " already exists.";

            /// 2=> give our scenario to our mocks
            when(adminCategoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenThrow(
                    new DuplicateRecordException("Category", "name", newCategoryName)
            );
            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/admin/categories/{id}", categoryId)
                            .content(objectMapper.writeValueAsString(fakeRequestDto))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print()) /// to see details in console when test passed
            /// check our return ApiResponse
            /// check status code
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());
        }
    }

    /// ////////////////////////////////////////
    @Nested
    @DisplayName("4. Delete Category Tests (DELETE)")
    class DeleteCategoryTests {
        /// // Delete Category by Id  - Category Deleted
        @Test
        void deleteCategory_WhenIdFoundInDB_ShouldDeleteCategory() throws Exception {
            Long categoryId = 1L;
            String fakeMessage = "Category deleted successfully";

            doNothing().when(adminCategoryService).deleteCategory(categoryId);

            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(delete("/api/v1/admin/categories/{id}", categoryId)
                            /// convert dto to json
                            /// content add it into body of http request
                            /// in controller endpoint (@RequestBody) so jackson make Deserialization)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message
                    .andExpect(jsonPath("$.message").value(fakeMessage));

        }

        /// // Delete Category by Id not found in DB  - Category Not Deleted
        @Test
        void deleteCategory_WhenIdNotFoundInDB_ShouldReturnNotDeleted() throws Exception {
            Long categoryId = 10L;

            String fakeMessage = "Category with id " + categoryId + " could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args = new Object[]{"id", categoryId};

            /// 2=> give our scenario to our mocks
            doThrow(new ResourceNotFoundException(errorKey, args))
                    .when(adminCategoryService)
                    .deleteCategory(eq(categoryId));

            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(delete("/api/v1/admin/categories/{id}", categoryId)
                            /// convert dto to json
                            /// content add it into body of http request
                            /// in controller endpoint (@RequestBody) so jackson make Deserialization)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound()) /// 404
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());

        }
    }
}