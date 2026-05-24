package com.global.order_api.feature.category;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.util.List;


/// make slicing testing => turn on only web layer and ignore other layers
/// tell spring we will test this controller to save memory of other controllers
@WebMvcTest(value = CategoryController.class ,
        /// exclude security layer
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class CategoryControllerTest {

    /// like postman to make test requests to our endpoints
    @Autowired
    private MockMvc mockMvc;

    /// create fake object and add it into spring context to inject it
    @MockitoBean
    private CategoryService categoryService;

    /// objectMapper is the primary class internal jackson library
    /// he is responsible for translating
    /// so MockMvc doesn't know how to send Java object
    /// so we send request in json format
    /// so we call objectMapper to make Serialization like {"name":"labtop"}
    @Autowired
    private ObjectMapper objectMapper; /// translator from java/json to json/java
    @MockitoBean
    private AppTranslator appTranslator;

    /// function header =>  throws Exception => because mockmvc.perform()
    /// because we simulate http request journey and go into layers like
    /// servelt, controller ,jackson so may any exception occur
    /// so if any exception occur => Junit catch it and failed test
    /// exception here =>unhandled Exception
    /// if my code cause an exception and not added in GlobalExceptionHandler
    /// or invalid url or invalid json
    /// production code => we use try-catch
    /// test mode => throw exception and fail test to solve the problem
    /// @Nested => to create inner classes in primary test class

    ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Category Tests (GET)")
    class GetCategoryTests{

        //////////////// GET CATEGORY BY ID States/////////////
        ///// Find by ID Exists - RETURN CATEGORY
        @Test
        void findCategoryById_WhenIdExists_ShouldReturnCategoryWithSuccessMessage() throws Exception
        {
            long categoryId=1L;
            /// 1=> create expected response from service
            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(categoryId);
            fakeDto.setName("Electronics");

            /// expected Message
            String fakeMessage = "Category retrieved successfully";

            /// 2=> give our scenario to our mocks
            when(categoryService.findCategoryById(categoryId)).thenReturn(fakeDto);

            when(appTranslator.getTranslatedAction(eq("success.retrieved"),anyString()))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/categories/{id}",categoryId)
                            .contentType(MediaType.APPLICATION_JSON))
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    /// go to data to find id and name fields
                    .andExpect(jsonPath("$.data.id").value(categoryId))
                    .andExpect(jsonPath("$.data.name").value("Electronics"));
        }

        ///// Find by ID Does not Exist - NOT FOUND
        @Test
        void findCategoryById_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception
        {
            long categoryId=20L;
            /// expected Message
            String fakeMessage = "Category with id "+categoryId+" could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args=new Object[]{"id",categoryId};
            /// 2=> give our scenario to our mocks
            when(categoryService.findCategoryById(categoryId)).thenThrow(
                    new ResourceNotFoundException(errorKey,args)
            );
            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/categories/{id}",categoryId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print()) /// to see details in console when test passed
            /// check our return ApiResponse
            /// check status code
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());
        }

        ///////////////// GET ALL CATEGORIES/////////////
        @Test
        void findAllCategories_ShouldReturnAllCategoriesWithSuccessMessage() throws Exception
        {
            /// 1=> create fake user filter
            CategoryFilterRequestDto filterRequestDto=new CategoryFilterRequestDto();
            filterRequestDto.setPage(0);
            filterRequestDto.setSize(10);

            /// 2=> mock the return from service PageResponse<CategoryResponseDto>
            CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
            fakeResponseDto.setId(1l);
            fakeResponseDto.setName("Books");

            List<CategoryResponseDto> dtos=List.of(fakeResponseDto);

            Page<CategoryResponseDto> page=new PageImpl<>(dtos);
            PageResponse<CategoryResponseDto> fakePage= PageResponse.from(
                    page,dtos
            );

            /// 3=> give our scenario to our mocks
            when(categoryService.getCategoriesPage(ArgumentMatchers.any(CategoryFilterRequestDto.class)))
                    .thenReturn(fakePage);

            /// expected Message
            String fakeMessage = "Category retrieved successfully";
            when(appTranslator.getTranslatedAction(eq("success.retrieved"),anyString()))
                    .thenReturn(fakeMessage);

            /// 4=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/categories/page")
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

        ///////////////// GET ALL CATEGORIES FOR USERS (Public) /////////////
        @Test
        void getCategories_ShouldReturnAllCategoriesListWithSuccessMessage() throws Exception
        {
            /// 1=> create fake data to be returned by service
            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setId(1L);
            fakeResponseDto.setName("Electronics");

            List<CategoryResponseDto> fakeCategoriesList = List.of(fakeResponseDto);

            /// 2=> give our scenario to our mocks
            when(categoryService.findAllCategories())
                    .thenReturn(fakeCategoriesList);

            /// expected Message
            String fakeMessage = "Categories retrieved successfully";
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), anyString()))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of public endpoint (No query params needed here)
            mockMvc.perform(get("/api/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    /// 4=> check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message and success flag
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.success").value(true))
                    /// check the data array itself
                    /// Since it's a List directly, data is an array, not an object containing a list
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Electronics"));
        }

        ////////////////////////////////////////////////////////////////
        //////////////// GET CATEGORY BY Name States/////////////
        /// Get Category by Name - RETURN CATEGORY
        @Test
        void findCategoryByName_WhenNameExists_ShouldReturnCategoryWithSuccessMessage() throws Exception
        {
            String categoryName="Mobiles";
            /// 1=> create expected response from service
            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName(categoryName);

            /// expected Message
            String fakeMessage = "Category retrieved successfully";

            /// 2=> give our scenario to our mocks
            when(categoryService.findCategoryByName(categoryName)).thenReturn(fakeDto);

            when(appTranslator.getTranslatedAction(eq("success.retrieved"),eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/categories/name/{name}",categoryName)
                            .contentType(MediaType.APPLICATION_JSON))
                    /// check our return ApiResponse
                    /// check status code
                    .andExpect(status().isOk())
                    /// check our message
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    /// go to data to find id and name fields
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").value(categoryName));
        }

        /// Get Category by Name not in DB - Category NOT FOUND
        @Test
        void findCategoryByName_WhenNameDoesNotExist_ShouldReturnNotFound() throws Exception
        {
            String categoryName="TV";
            /// expected Message
            String fakeMessage = "Category with name "+categoryName+" could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args=new Object[]{"name",categoryName};
            /// 2=> give our scenario to our mocks
            when(categoryService.findCategoryByName(categoryName)).thenThrow(
                    new ResourceNotFoundException(errorKey,args)
            );
            /// method in GlobalExceptionHandler and base repo
            /// pass any not args => because mockito compare using equals()
            /// so it compares addresses not values
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(get("/api/v1/categories/name/{name}",categoryName)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print()) /// to see details in console when test passed
            /// check our return ApiResponse
            /// check status code
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.timeStamp").exists());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Category Tests (POST)")
    class CreateCategoryTest
    {
        ///// Create Category with new name - Category Created
        @Test
        void createCategory_WhenNameDoesNotExist_ShouldReturnCategoryWithSuccessMessage() throws Exception
        {
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
            fakeRequestDto.setName("Mobiles");

            /// 2=> create fake response from service
            CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
            fakeResponseDto.setName("Mobiles");
            fakeResponseDto.setId(7L);
            /// 3=> expected Message
            String fakeMessage = "Category created successfully";

            /// 4=> give our scenario to our mocks
            when(categoryService.createCategory(fakeRequestDto)).thenReturn(fakeResponseDto);

            when(appTranslator.getTranslatedAction(eq("success.created"),eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(post("/api/v1/categories")
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

        ///// Create Category with another category name - Category NOT Created
        @Test
        void createCategory_WhenNameExists_ShouldReturnNotCreated() throws Exception
        {
            String categoryName="Books";
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
            fakeRequestDto.setName(categoryName);

//        /// 2=> create fake response from sevice
//        CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
//        fakeResponseDto.setName(categoryName)r;
//        fakeResponseDto.setId(7L);
            /// 2=> expected Message
            String errorKey = "error.resource.duplicate";
            String fakeMessage = "Category with name "+categoryName+" already exists.";

            /// 4=> give our scenario to our mocks
            /// problem here => after serialization controller make new object with these data
            /// and mockito compare addresses no values
            /// so using any
            when(categoryService.createCategory(ArgumentMatchers.any(CategoryRequestDto.class))).thenThrow(
                    new DuplicateRecordException("Category","name",categoryName)
            );

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(post("/api/v1/categories")
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

        ///// Create Category with empty name - Category NOT Created
        @Test
        void createCategory_WhenNameIsEmpty_ShouldReturnBadRequest() throws Exception
        {

            CategoryRequestDto invalidRequestDto=new CategoryRequestDto();
            invalidRequestDto.setName(""); /// empty name
        /// request will not go to service layer because we use @Valid
            String fakeMessage="Validation failed.";
            String errorKey="error.validation";
            when(appTranslator.translateMessage(eq(errorKey)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(post("/api/v1/categories")
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

    ///////////////////////////////////////////
    @Nested
    @DisplayName("3. Update Category Tests (PUT)")
    class UpdateCategoryTest
    {
        ///// Update Category with same or Different name - Category Updated
        @Test
        void updateCategory_WhenSameOrDifferentName_ShouldReturnCategoryWithSuccessMessage() throws Exception
        {
            String categoryNameExists="Books";
            Long categoryId=1L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
            fakeRequestDto.setName(categoryNameExists);
            fakeRequestDto.setDescription("new description");

            /// 2=> create fake response from service
            CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
            fakeResponseDto.setName("Mobiles");
            fakeResponseDto.setId(categoryId);
            fakeResponseDto.setDescription("new description");
            /// 3=> expected Message
            String fakeMessage = "Category updated successfully";
            /// 4=> give our scenario to our mocks
            when(categoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenReturn(fakeResponseDto);

            when(appTranslator.getTranslatedAction(eq("success.updated"),eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/categories/{id}",categoryId)
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

        ///// Update Category But Id Not Found - Category Not Updated
        @Test
        void updateCategory_WhenIdNotFound_ShouldReturnNotFound() throws Exception
        {
            String newCategoryName="TVs";
            Long categoryId=11L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
            fakeRequestDto.setName(newCategoryName);
            String fakeMessage = "Category with id "+categoryId+" could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args=new Object[]{"id",categoryId};

            /// 2=> give our scenario to our mocks
            when(categoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenThrow(
                    new ResourceNotFoundException(errorKey,args)
            );
            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/categories/{id}",categoryId)
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

        ///// Update Category with another category name - Category Not Updated
        @Test
        void updateCategory_WhenNameAlreadyExists_ShouldReturnNotUpdated() throws Exception
        {
            String newCategoryName="Books";
            Long categoryId=2L;
            /// 1=> create fake request dta
            CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
            fakeRequestDto.setName(newCategoryName);
            String errorKey = "error.resource.duplicate";
            String fakeMessage = "Category with name "+newCategoryName+" already exists.";

            /// 2=> give our scenario to our mocks
            when(categoryService.updateCategory(ArgumentMatchers.any(CategoryRequestDto.class),
                    eq(categoryId))).thenThrow(
                    new DuplicateRecordException("Category","name",newCategoryName)
            );
            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);
            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(put("/api/v1/categories/{id}",categoryId)
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

    ///////////////////////////////////////////
    @Nested
    @DisplayName("4. Delete Category Tests (DELETE)")
    class DeleteCategoryTests {
        ///// Delete Category by Id  - Category Deleted
        @Test
        void deleteCategory_WhenIdFoundInDB_ShouldDeleteCategory() throws Exception
        {
            Long categoryId=1L;
            String fakeMessage = "Category deleted successfully";

            doNothing().when(categoryService).deleteCategory(categoryId);

            when(appTranslator.getTranslatedAction(eq("success.deleted"),eq("entity.category")))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(delete("/api/v1/categories/{id}",categoryId)
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

        ///// Delete Category by Id not found in DB  - Category Not Deleted
        @Test
        void deleteCategory_WhenIdNotFoundInDB_ShouldReturnNotDeleted() throws Exception
        {
            Long categoryId=10L;

            String fakeMessage = "Category with id "+categoryId+" could not be found.";
            String errorKey = "error.resource.not.found";
            Object[] args=new Object[]{"id",categoryId};

            /// 2=> give our scenario to our mocks
            doThrow( new ResourceNotFoundException(errorKey,args))
                    .when(categoryService)
                    .deleteCategory(eq(categoryId));

            /// method in GlobalExceptionHandler and base repo
            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeMessage);

            /// 3=> test our function
            /// pass url of endpoint
            mockMvc.perform(delete("/api/v1/categories/{id}",categoryId)
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