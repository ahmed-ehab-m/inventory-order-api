package com.global.order_api.feature.category;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.product.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/// JUnit doesn't understand Mockito Library annotations
/// so ExtendWith => means JUnit will use external power
/// this power = Mockito library
@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    /// UNIT TESTING
    @Mock
    /// create an copy of repo
    /// this copy her code will not be executed
    private CategoryRepo categoryRepo;

    @Mock
    private ProductRepo productRepo;

    /// No mock for base service => partial Mocking
    /// because category service extends from base service

    @Mock
    private CategoryMapper categoryMapper;

    private AdminCategoryService adminCategoryService;

    @BeforeEach
    void setUp() {
        adminCategoryService = new AdminCategoryService(categoryRepo, categoryRepo, categoryMapper, productRepo);
    }

    /// @Nested => to create inner classes in primary test class
    /// ////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Category Tests (GET)")
    class GetCategoryTests {

        /// ////////////// GET ALL CATEGORIES/////////////
        /// Without Keyword
        @Test
        void findCategoriesPage_WithoutKeyword_ShouldReturnCategories() {
            /// function take filter DTO and return PageResponse of CategoryResponseDTO
            /// so we have to create filter dto - list of entities and pageimpl
            /// 1=> so mock user request filter because service function requires filter
            CategoryFilterRequestDto mockFilter = new CategoryFilterRequestDto();
            mockFilter.setPage(0);
            mockFilter.setSize(10);

            /// 2=> create fake entities to add it into list
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName("Electronics");

            /// 3=> create a fakeList of fakeEntity to add it into page
            List<CategoryEntity> entityList = List.of(fakeEntity);
            /// pageImpl => because Page in an interface we can't create an object
            /// so we use the implementation
            /// pageImpl => take list and he add meta data
            /// will be return from repo
            Page<CategoryEntity> mockEntityPage = new PageImpl<>(entityList);

            /// 4=> create fake dto and fake dto list
            /// will be return from category mapper
            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Electronics");
            List<CategoryResponseDto> fakeDtoList = List.of(fakeDto);

            /// 5=> call mocks
            when(categoryRepo.findAll(any(Pageable.class))).thenReturn(mockEntityPage);
            when(categoryMapper.mapToDtoList(entityList)).thenReturn(fakeDtoList);

            /// 6=> call our function for testing
            PageResponse<CategoryResponseDto> result = adminCategoryService.getCategoriesPage(mockFilter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            /// to check we return the right data
            assertEquals("Electronics", result.getData().getFirst().getName());

            /// any() => only specifies the data type and don't care about internal data
            /// code in service => Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
            /// Mockito using equals() to check if test pass or fail
            /// so i have to create same return object from service here
            /// so ew use any to check only return type is the same
            /// any() => return Dummy value of Data Type to prevent Ide error
            /// and open ArgumentMatcherStorage => to call mockito
            /// to stop equals() for this parameter since they the same data type
            verify(categoryRepo, times(1)).findAll(any(Pageable.class));
            verify(categoryRepo, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        /// Blank Keyword
        @Test
        void findCategoriesPage_WithBlankKeyword_ShouldReturnAllCategories() {
            CategoryFilterRequestDto mockFilter = new CategoryFilterRequestDto();
            mockFilter.setPage(0);
            mockFilter.setSize(10);
            mockFilter.setSearchKeyword("   ");

            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName("Electronics");

            List<CategoryEntity> entityList = List.of(fakeEntity);
            Page<CategoryEntity> mockEntityPage = new PageImpl<>(entityList);

            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Electronics");
            List<CategoryResponseDto> fakeDtoList = List.of(fakeDto);

            when(categoryRepo.findAll(any(Pageable.class))).thenReturn(mockEntityPage);
            when(categoryMapper.mapToDtoList(entityList)).thenReturn(fakeDtoList);

            PageResponse<CategoryResponseDto> result = adminCategoryService.getCategoriesPage(mockFilter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            assertEquals("Electronics", result.getData().getFirst().getName());

            verify(categoryRepo, times(1)).findAll(any(Pageable.class));
            verify(categoryRepo, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }

        /// With Keyword
        @Test
        void findCategoriesPage_WithKeyword_ShouldReturnCategories() {
            /// function take filter DTO and return PageResponse of CategoryResponseDTO
            /// so we have to create filter dto - list of entities and pageimpl
            /// 1=> so mock user request filter because service function requires filter
            CategoryFilterRequestDto mockFilter = new CategoryFilterRequestDto();
            String keyword = "Books";
            mockFilter.setPage(0);
            mockFilter.setSize(10);
            mockFilter.setSearchKeyword(keyword);

            /// 2=> create fake entities to add it into list
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName("Books collection");

            /// 3=> create a fakeList of fakeEntity to add it into page
            List<CategoryEntity> entityList = List.of(fakeEntity);
            /// pageImpl => because Page in an interface we can't create an object
            /// so we use the implementation
            /// pageImpl => take list and he add meta data
            /// will be return from repo
            Page<CategoryEntity> mockEntityPage = new PageImpl<>(entityList);

            /// 4=> create fake dto and fake dto list
            /// will be return from category mapper
            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Books collection");
            List<CategoryResponseDto> fakeDtoList = List.of(fakeDto);

            /// 5=> call mocks

            /// eq => Equals means i want this value not any value
            /// to prevent if in service code we pass wrong value like send getSortBy()
            /// insteadof search key word so we validate that we pass the right value
            /// because mockito rules => the function take real values or using any not MIX
            /// but eq make our value is a Matcher like any()

            when(categoryRepo.findByNameContainingIgnoreCase(eq(keyword), any(Pageable.class)))
                    .thenReturn(mockEntityPage);
            when(categoryMapper.mapToDtoList(entityList)).thenReturn(fakeDtoList);

            /// 6=> call our function for testing
            PageResponse<CategoryResponseDto> result = adminCategoryService.getCategoriesPage(mockFilter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            /// to check we return the right data
            assertEquals("Books collection", result.getData().getFirst().getName());

            verify(categoryRepo, never()).findAll(any(Pageable.class));
            verify(categoryRepo, times(1)).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create Category Tests (POST)")
    class CreateCategoryTest {
        /// // Create Category with new name - Category Created
        @Test
        void createCategory_WhenNewName_ShouldCreateCategory() {
            /// 1=> create fake dto
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName("TVs");

            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setName("TVs");
            fakeResponseDto.setId(7L);

            /// 2=> create fake entity to use in mapper
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setName("TVs");
            fakeEntity.setId(7L);

            ///3=> call repo and mapper
            when(categoryMapper.mapToEntity(fakeRequestDto)).thenReturn(fakeEntity);
            when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
            when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);

            /// 4=> call our function for testing
            CategoryResponseDto result = adminCategoryService.createCategory(fakeRequestDto);
            assertNotNull(result);
            assertEquals("TVs", result.getName());
            assertEquals(7L, result.getId());

            /// 5=> check our work flow
            verify(categoryRepo, times(1)).existsByName("TVs");
            verify(categoryMapper, times(1)).mapToEntity(fakeRequestDto);
            verify(categoryRepo, times(1)).save(fakeEntity);
            verify(categoryMapper, times(1)).mapToDto(fakeEntity);

        }

        /// // Create Category with another category name - Category NOT Created
        @Test
        void createCategory_WhenDuplicateName_ShouldThrowException() {
            String duplicateName = "Books";
            /// 1=> create fake dto
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(duplicateName); /// name already exists in our DB

            //// no need to create entity because we use only name

            ///2=> call repo
            when(categoryRepo.existsByName(duplicateName)).thenReturn(true);

            /// 4=> call our function for testing
            assertThrows(DuplicateRecordException.class, () ->
                    adminCategoryService.createCategory(fakeRequestDto));

            /// 5=> check our work flow
            verify(categoryRepo, times(1)).existsByName(duplicateName);
            verify(categoryMapper, never()).mapToDto(any());
            verify(categoryRepo, never()).save(any());
        }
    }

    /// ////////////////////////////////////////
    @Nested
    @DisplayName("3. Update Category Tests (PUT)")
    class UpdateCategoryTest {
        /// // Update Category with same name - Category Updated
        @Test
        void updateCategory_WhenSameName_ShouldUpdateCategory() {
            /// 1=> create fake request dto
            String newName = "TVs";
            String newDescription = "TVs new description";
            Long oldId = 1L;
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(newName);
            fakeRequestDto.setDescription(newDescription);

            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setName(newName);
            fakeResponseDto.setId(oldId);
            fakeResponseDto.setDescription(newDescription);

            /// 2=> create fake entity to use in mapper
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setName(newName);
            fakeEntity.setId(oldId);
            fakeEntity.setDescription(newDescription);

            ///3=> call repo and mapper
            when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
//        when(categoryRepo.existsByName(newName)).thenReturn(false);
            /// when is implemented to return a value not void
            /// donothing => means that don't return any thing
            doNothing().when(categoryMapper).updateEntityFromDto(fakeRequestDto, fakeEntity);
            when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
            when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);


            /// 4=> call our function for testing
            CategoryResponseDto result = adminCategoryService.updateCategory(fakeRequestDto, oldId);
            assertNotNull(result);
            assertEquals(newName, result.getName());
            assertEquals(oldId, result.getId());

            /// 5=> check our work flow
            verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
            verify(categoryMapper, times(1)).updateEntityFromDto(fakeRequestDto, fakeEntity);
            verify(categoryRepo, times(1)).save(fakeEntity);
            verify(categoryMapper, times(1)).mapToDto(fakeEntity);
        }

        /// // Update Category with  Different name - Category Updated
        @Test
        void updateCategory_WhenDifferentName_ShouldUpdateCategory() {
            /// 1=> create fake request dto
            String newName = "TVs";
            Long oldId = 1L;
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(newName);

            CategoryResponseDto fakeResponseDto = new CategoryResponseDto();
            fakeResponseDto.setName(newName);
            fakeResponseDto.setId(oldId);

            /// 2=> create fake entity to use in mapper
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setName("old tv description");
            fakeEntity.setId(oldId);

            ///3=> call repo and mapper
            when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
            when(categoryRepo.existsByName(newName)).thenReturn(false);
            /// when is implemented to return a value not void
            /// donothing => means that don't return any thing
            doNothing().when(categoryMapper).updateEntityFromDto(fakeRequestDto, fakeEntity);
            when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
            when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);


            /// 4=> call our function for testing
            CategoryResponseDto result = adminCategoryService.updateCategory(fakeRequestDto, oldId);
            assertNotNull(result);
            assertEquals(newName, result.getName());
            assertEquals(oldId, result.getId());

            /// 5=> check our work flow
            verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
            verify(categoryRepo, times(1)).existsByName(newName);
            verify(categoryMapper, times(1)).updateEntityFromDto(fakeRequestDto, fakeEntity);
            verify(categoryRepo, times(1)).save(fakeEntity);
            verify(categoryMapper, times(1)).mapToDto(fakeEntity);
        }

        /// // Update Category with another category name - Category Not Updated
        @Test
        void updateCategory_WhenNameAlreadyExists_ShouldThrowException() {
            /// 1=> create fake request dto
            String nameAnotherCategory = "new  Books";
            String oldNameCategoryEntity = "Books";

            Long oldId = 1L;
            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName(nameAnotherCategory);

            /// 2=> create fake entity to use in mapper
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setName(oldNameCategoryEntity);
            fakeEntity.setId(oldId);

            ///3=> call repo and mapper
            when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
            when(categoryRepo.existsByName(nameAnotherCategory)).thenReturn(true);


            /// 4=> call our function for testing
            assertThrows(DuplicateRecordException.class,
                    () -> adminCategoryService.updateCategory(fakeRequestDto, oldId));


            /// 5=> check our work flow
            verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
            verify(categoryRepo, times(1)).existsByName(nameAnotherCategory);
            verify(categoryMapper, never()).updateEntityFromDto(fakeRequestDto, fakeEntity);
            verify(categoryRepo, never()).save(fakeEntity);
            verify(categoryMapper, never()).mapToDto(fakeEntity);
        }

        /// // Update Category but ID not Found in DB - Category Not Updated
        @Test
        void updateCategory_WhenIDNotFound_ShouldThrowException() {
            /// 1=> create Invalid Category Id
            Long invalidCategoryId = 100L;

            CategoryRequestDto fakeRequestDto = new CategoryRequestDto();
            fakeRequestDto.setName("new category");

            ///2=> call repo and mapper
            /// problem => in category service we call findById() this method in BaseService
            /// findById()=> call BaseRepo => return (findByIdOrThrow custom Method we created)
            /// problem is here mockito may fail take this function if internal call
            /// so we force mockito to throw exception and never execute the function
            /// because when() java must call method first
            /// and this method is default and mockito may allow real code to run
            doThrow(new ResourceNotFoundException("category not found with id: "
                    + invalidCategoryId))
                    .when(categoryRepo).findByIdOrThrow(eq(invalidCategoryId));
//            when(categoryRepo.findById(ArgumentMatchers.any())).thenThrow(
//                    new ResourceNotFoundException("category not found with id: "
//                    +invalidCategoryId));

            /// 3=> call our function for testing
            assertThrows(ResourceNotFoundException.class,
                    () -> adminCategoryService.updateCategory(fakeRequestDto, invalidCategoryId));


            /// 4=> check our work flow
            verify(categoryRepo, times(1)).findByIdOrThrow(ArgumentMatchers.any());
            verify(categoryRepo, never()).existsByName("new CATEGORY");
            verify(categoryMapper, never()).updateEntityFromDto(any(CategoryRequestDto.class), any(CategoryEntity.class));
            verify(categoryRepo, never()).save(any(CategoryEntity.class));
            verify(categoryMapper, never()).mapToDto(any(CategoryEntity.class));
        }

    }

    /// ////////////////////////////////////////
    @Nested
    @DisplayName("4. Delete Category Tests (DELETE)")
    class DeleteCategoryTests {

        /// // Delete Category by Id  - Category Deleted
        @Test
        void deleteCategory_WhenFoundInDB_ShouldDeleteCategory() {
            Long id = 1L;
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(id);

            when(categoryRepo.findByIdOrThrow(id)).thenReturn(fakeEntity);
            doNothing().when(productRepo).moveProductsToDefaultCategory(id);

            adminCategoryService.deleteCategory(id);
            /// no assert here because return type is void
            verify(categoryRepo, times(1)).findByIdOrThrow(id);
            verify(categoryRepo, times(1)).delete(fakeEntity);
            verify(productRepo, times(1)).moveProductsToDefaultCategory(id);
        }

        /// // Delete Default Category - Category Not Deleted
        @Test
        void deleteCategory_WhenDefaultCategory_ShouldThrowException() {
            Long id = 999L;
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(id);

            assertThrows(BusinessLogicException.class, () ->
                    adminCategoryService.deleteCategory(id));
            /// no assert here because return type is void
            verify(categoryRepo, never()).findByIdOrThrow(id);
            verify(categoryRepo, never()).delete(fakeEntity);
            verify(productRepo, never()).moveProductsToDefaultCategory(id);
        }

        /// // Delete Category by Id not found in DB  - Category Not Deleted
        @Test
        void deleteCategory_WhenNotFoundInDB_ShouldDeleteCategory() {
            Long id = 10L; /// not found in our DB

            when(categoryRepo.findByIdOrThrow(id)).thenThrow(
                    ResourceNotFoundException.class
            );
            assertThrows(ResourceNotFoundException.class, () ->
                    adminCategoryService.deleteCategory(id));

            /// no assert here because return type is void
            verify(categoryRepo, times(1)).findByIdOrThrow(id);
            verify(categoryRepo, never()).delete(any());
            verify(productRepo, never()).moveProductsToDefaultCategory(id);
        }
    }
}