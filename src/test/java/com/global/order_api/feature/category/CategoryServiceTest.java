package com.global.order_api.feature.category;

import com.global.order_api.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/// JUnit doesn't understand Mockito Library annotations
/// so ExtendWith => means JUnit will use external power
/// this power = Mockito library
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    /// UNIT TESTING
    @Mock
    /// create an copy of repo
    /// this copy her code will not be executed
    private CategoryRepo categoryRepo;

    /// No mock for base service => partial Mocking
    /// because category service extends from base service

    @Mock
    private CategoryMapper categoryMapper;

    /// create real object from this service
    /// (manually no need to use spring context) all mocks we defined it above in this class
    /// call our layer  which layer's functions will be tested
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepo, categoryRepo, categoryMapper);
    }

    /// @Nested => to create inner classes in primary test class
    /// ////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Category Tests (GET)")
    class GetCategoryTests {
        /// ///////////// GET CATEGORY BY ID States/////////////
        /// // Find by ID Exists - RETURN CATEGORY
        /// from JUnit library
        /// to tell ide this function is a test function
        /// run this function and return result (green or red)
        /// Roy Osherove method name
        /// MethodName_StateUnderTest_ExpectedBehavior
        @Test
        void findCategoryById_WhenExists_ShouldReturnCategory() {
            /// 1=> create fake entity and fake DTO
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName("Mobiles");

            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Mobiles");

            /// 2=> call mocks and pass fake objects
            /// when => like if-then
            /// take condition then return result we specified (scenario created to right test)
            /// without it => object will return default value is Null for objects
            when(categoryRepo.findByIdOrThrow(1L)).thenReturn(fakeEntity);
            when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

            /// 3=> call our function for testing
            CategoryResponseDto result = categoryService.findCategoryById(1L);

            /// 4=> call assert
            /// check null => because if mapper or service has bad code then return null
            assertNotNull(result);
            /// check the id and name are the same return id and name
            assertEquals(1L, result.getId());
            assertEquals("Mobiles", result.getName());

            /// 5=> our cameras (Work Flow)
            /// Behavior Verification  (like camera in our code)
            /// to see what happened
            /// here to verify that service call repo only one
            /// to fix performance issue if i call db more than one in service code
            /// so i will go to service code and enhance it (this i don't have to go to dm many times)
            verify(categoryRepo, times(1)).findByIdOrThrow(1L);
            verify(categoryMapper, times(1)).mapToDto(fakeEntity);
        }

        /// // Find by ID Does not Exist - NOT FOUND
        @Test
        void findCategoryById_WhenIdNotFound_ShouldThrowException() {
            /// 1=> create invalid id
            Long inValidID = 10L;

            /// 2=> call Mock Repo and make script to it using when
            when(categoryRepo.findByIdOrThrow(inValidID))
                    .thenThrow(new ResourceNotFoundException("category not found with id: " + inValidID));

            /// 3=> call our method which we will test it from the layer
            /// this method throws ResourceNotFoundException if id not found in DB (expected)
            ///(expected,actual)
            /// assert => methods to check and compare expected result and actual result
            /// assert internal code is if-condition
            /// assert if condition fails throws AssertionError which Junit know that test is failed
            assertThrows(ResourceNotFoundException.class, () ->
                    categoryService.findCategoryById(inValidID));

            /// 4=> WorkFlow
            verify(categoryRepo, times(1)).findByIdOrThrow(inValidID);
            /// verify that mapper doesn't execute maptoDto
            /// to fix issues early => if any another developer put repo code in try,catch
            /// and not provide a simple message to user
            /// so code should stop if throws exception not to go to mapper
            /// because if code go to mapper the result => NullPointerException
            /// any()=> mapper doesn't received any mockEntity
            /// to fix issue if any developer put repo in try-catch
            /// then pass new entity to mapper by mistake
            verify(categoryMapper, never()).mapToDto(any());
        }

        /// USER CATEGORIES
        @Test
        void findAllCategories_WhenDataExists_ShouldReturnListOfCategories() {
            /// 1=> create fake entities
            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setId(1L);
            fakeEntity.setName("Electronics");
            List<CategoryEntity> entityList = List.of(fakeEntity);

            /// 2=> create fake DTOs
            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setId(1L);
            fakeDto.setName("Electronics");
            List<CategoryResponseDto> dtoList = List.of(fakeDto);

            /// 3=> call mocks
            when(categoryRepo.findAll()).thenReturn(entityList);
            when(categoryMapper.mapToDtoList(entityList)).thenReturn(dtoList);

            /// 4=> call our function for testing
            List<CategoryResponseDto> result = categoryService.findAllCategories();

            /// 5=> assertions
            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals("Electronics", result.get(0).getName());

            /// 6=> verify interactions
            verify(categoryRepo, times(1)).findAll();
            verify(categoryMapper, times(1)).mapToDtoList(entityList);
        }

        @Test
        void findAllCategories_WhenNoData_ShouldReturnEmptyList() {
            /// 1=> create empty lists to simulate empty database
            List<CategoryEntity> emptyEntityList = List.of(); // Empty list from DB
            List<CategoryResponseDto> emptyDtoList = List.of(); // Empty list from Mapper

            /// 2=> call mocks
            when(categoryRepo.findAll()).thenReturn(emptyEntityList);
            when(categoryMapper.mapToDtoList(emptyEntityList)).thenReturn(emptyDtoList);

            /// 3=> call our function for testing
            List<CategoryResponseDto> result = categoryService.findAllCategories();

            /// 4=> assertions
            // The result must not be null, it should be just empty
            assertNotNull(result);
            assertTrue(result.isEmpty());

            /// 5=> verify interactions
            verify(categoryRepo, times(1)).findAll();
            verify(categoryMapper, times(1)).mapToDtoList(emptyEntityList);
        }

        /// /////////////////////////////////////////////////////////////
        /// ///////////// GET CATEGORY BY Name States/////////////
        /// Get Category by Name - RETURN CATEGORY
        @Test
        void findCategoryByName_WhenExists_ShouldReturnCategory() {

            String validName = "Books";

            CategoryEntity fakeEntity = new CategoryEntity();
            fakeEntity.setName(validName);
            fakeEntity.setId(1L);

            CategoryResponseDto fakeDto = new CategoryResponseDto();
            fakeDto.setName(validName);
            fakeDto.setId(1L);

            when(categoryRepo.findByName(validName))
                    .thenReturn(Optional.of(fakeEntity));
            when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

            CategoryResponseDto result = categoryService.findCategoryByName(validName);
            assertNotNull(result);
            assertEquals(validName, result.getName());
            assertEquals(1L, result.getId());
            verify(categoryRepo, times(1)).findByName(validName);
            verify(categoryMapper, times(1)).mapToDto(fakeEntity);

        }

        /// Get Category by Name not in DB - Category NOT FOUND
        @Test
        void findCategoryByName_WhenNameNotFound_ShouldThrowException() {
            String invalidName = "TV";

            when(categoryRepo.findByName(invalidName)).thenThrow(
                    new ResourceNotFoundException("not found category with this name" + invalidName)
            );

            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.findCategoryByName(invalidName));

            verify(categoryRepo, times(1)).findByName(invalidName);
            verify(categoryMapper, never()).mapToDto(any());

        }
    }
}