package com.global.order_api.feature.category;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.product.ProductRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    /// create real object from this service
    /// inject all mocks we defined it above in this class
    /// call our layer  which layer's functions will be tested
    private CategoryService categoryService;


    //////////////////////////CATEGORY ID////////////////////////////////
    /// from JUnit library
    /// to tell ide this function is a test function
    /// run this function and return result (green or red)
    @Test
    /// Roy Osherove method name
    /// MethodName_StateUnderTest_ExpectedBehavior

    void findCategoryById_WhenIdNotFound_ShouldThrowException() {
        /// 1=> create invalid id
        Long inValidID=10L;

        /// 2=> call Mock Repo and make script to it using when
        /// when => like if-then
        /// take condition then return result we specified (scenario created to right test)
        /// without it => object will return default value is Null for objects
        when(categoryRepo.findByIdOrThrow(inValidID))
                .thenThrow(new ResourceNotFoundException("category not found with id: "+inValidID));

        /// 3=> call our method which we will test it from the layer
        /// this method throws ResourceNotFoundException if id not found in DB (expected)
        ///(expected,actual)
        /// assert => methods to check and compare expected result and actual result
        /// assert internal code is if-condition
        /// assert if condition fails throws AssertionError which Junit know that test is failed
        assertThrows(ResourceNotFoundException.class ,()->
                categoryService.findCategoryById(inValidID));

        /// 4=> Behavior Verification  (like camera in our code)
        /// to see what happened
        /// here to verify that service call repo only one
        /// to fix performance issue if i call db more than one in service code
        /// so i will go to service code and enhance it (this i don't have to go to dm many times)
        verify(categoryRepo,times(1)).findByIdOrThrow(inValidID);

        /// verify that mapper doesn't execute maptoDto
        /// to fix issues early => if any another developer put repo code in try,catch
        /// and not provide a simple message to user
        /// so code should stop if throws exception not to go to mapper
        /// because if code go to mapper the result => NullPointerException
        /// any()=> mapper doesn't received any mockEntity
        /// to fix issue if any developer put repo in try-catch
        /// then pass new entity to mapper by mistake
        verify(categoryMapper,never()).mapToDto(any());
    }

    //////
    /// here we test if db return the right entity
    /// is this function will pass it to mapper and return it to user correctly or not
    @Test
    void findCategoryById_WhenExists_ShouldReturnCategory()
    {
        /// 1=> create fake entity and fake DTO
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setId(1L);
        fakeEntity.setName("Mobiles");

        CategoryResponseDto fakeDto=new CategoryResponseDto();
        fakeDto.setId(1L);
        fakeDto.setName("Mobiles");

        /// 2=> call mocks and pass fake objects
        when(categoryRepo.findByIdOrThrow(1L)).thenReturn(fakeEntity);
        when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

        /// 3=> call our function for testing
        CategoryResponseDto result=categoryService.findCategoryById(1L);

        /// 4=> call assert
        /// check null => because if mapper or service has bad code then return null
        assertNotNull(result);
        /// check the id and name are the same return id and name
        assertEquals(1L,result.getId());
        assertEquals("Mobiles",result.getName());

        /// 5=> our cameras (Work Flow)
        verify(categoryRepo, times(1)).findByIdOrThrow(1L);
        verify(categoryMapper, times(1)).mapToDto(fakeEntity);
    }


    //////////////////////////CATEGORY NAME////////////////////////////////
    @Test
    void findCategoryByName_WhenNameNotFound_ShouldThrowException()
    {
        String invalidName="TV";

        when(categoryRepo.findByName(invalidName)).thenThrow(
                new ResourceNotFoundException("not found category with this name" +invalidName)
        );

        assertThrows(ResourceNotFoundException.class,
                ()->categoryService.findCategoryByName(invalidName));

        verify(categoryRepo,times(1)).findByName(invalidName);
        verify(categoryMapper,never()).mapToDto(any());

    }
    ////////////////////
    @Test
    void findCategoryByName_WhenExists_ShouldReturnCategory()
    {

        String validName="Books";

        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setName(validName);
        fakeEntity.setId(1L);

        CategoryResponseDto fakeDto=new CategoryResponseDto();
        fakeDto.setName(validName);
        fakeDto.setId(1L);

        when(categoryRepo.findByName(validName))
                .thenReturn(Optional.of(fakeEntity));
        when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeDto);

        CategoryResponseDto result=categoryService.findCategoryByName(validName);
        assertNotNull(result);
        assertEquals(validName,result.getName());
        assertEquals(1L,result.getId());
        verify(categoryRepo, times(1)).findByName(validName);
        verify(categoryMapper, times(1)).mapToDto(fakeEntity);

    }

    ////////////////////////GET ALL CATEGORIES///////////////////////////
    @Test
    void findCategoriesPage_WithoutKeyword_ShouldReturnCategories()
    {
        /// function take filter DTO and return PageResponse of CategoryResponseDTO
        /// so we have to create filter dto - list of entities and pageimpl
        /// 1=> so mock user request filter because service function requires filter
        CategoryFilterRequestDto mockFilter=new CategoryFilterRequestDto();
        mockFilter.setPage(0);
        mockFilter.setSize(10);

        /// 2=> create fake entities to add it into list
        CategoryEntity fakeEntity =new CategoryEntity();
        fakeEntity.setId(1L);
        fakeEntity.setName("Electronics");

        /// 3=> create a fakeList of fakeEntity to add it into page
        List<CategoryEntity> entityList=List.of(fakeEntity);
        /// pageImpl => because Page in an interface we can't create an object
        /// so we use the implementation
        /// pageImpl => take list and he add meta data
        /// will be return from repo
        Page<CategoryEntity> mockEntityPage=new PageImpl<>(entityList);

        /// 4=> create fake dto and fake dto list
        /// will be return from category mapper
        CategoryResponseDto fakeDto = new CategoryResponseDto();
        fakeDto.setId(1L);
        fakeDto.setName("Electronics");
        List<CategoryResponseDto> fakeDtoList=List.of(fakeDto);



        /// 5=> call mocks
        when(categoryRepo.findAll(any(Pageable.class))).thenReturn(mockEntityPage);
        when(categoryMapper.mapToDtoList(entityList)).thenReturn(fakeDtoList);

        /// 6=> call our function for testing
        PageResponse<CategoryResponseDto> result=categoryService.getCategoriesPage(mockFilter);

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
    ////////////
    @Test
    void findCategoriesPage_WithKeyword_ShouldReturnCategories()

    {
        /// function take filter DTO and return PageResponse of CategoryResponseDTO
        /// so we have to create filter dto - list of entities and pageimpl
        /// 1=> so mock user request filter because service function requires filter
        CategoryFilterRequestDto mockFilter=new CategoryFilterRequestDto();
        String keyword = "Books";
        mockFilter.setPage(0);
        mockFilter.setSize(10);
        mockFilter.setSearchKeyword(keyword);

        /// 2=> create fake entities to add it into list
        CategoryEntity fakeEntity =new CategoryEntity();
        fakeEntity.setId(1L);
        fakeEntity.setName("Books collection");


        /// 3=> create a fakeList of fakeEntity to add it into page
        List<CategoryEntity> entityList=List.of(fakeEntity);
        /// pageImpl => because Page in an interface we can't create an object
        /// so we use the implementation
        /// pageImpl => take list and he add meta data
        /// will be return from repo
        Page<CategoryEntity> mockEntityPage=new PageImpl<>(entityList);

        /// 4=> create fake dto and fake dto list
        /// will be return from category mapper
        CategoryResponseDto fakeDto = new CategoryResponseDto();
        fakeDto.setId(1L);
        fakeDto.setName("Books collection");
        List<CategoryResponseDto> fakeDtoList=List.of(fakeDto);



        /// 5=> call mocks

        /// eq => Equals means i want this value not any value
        /// to prevent if in service code we pass wrong value like send getSortBy()
        /// insteadof search key word so we validate that we pass the right value
        /// because mockito rules => the function take real values or using any not MIX
        /// but eq make our value is a Matcher like any()

        when(categoryRepo.findByNameContainingIgnoreCase(eq(keyword) ,any(Pageable.class)))
                .thenReturn(mockEntityPage);
        when(categoryMapper.mapToDtoList(entityList)).thenReturn(fakeDtoList);

        /// 6=> call our function for testing
        PageResponse<CategoryResponseDto> result=categoryService.getCategoriesPage(mockFilter);

        assertNotNull(result);
        assertFalse(result.getData().isEmpty());
        /// to check we return the right data
        assertEquals("Books collection", result.getData().getFirst().getName());

        verify(categoryRepo, never()).findAll(any(Pageable.class));
        verify(categoryRepo, times(1)).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }


    //////////////////////////CREATE CATEGORY/////////////////////////////////////
    @Test
    void createCategory_WhenNewName_ShouldCreateCategory()
    {
        /// 1=> create fake dto
        CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
        fakeRequestDto.setName("TVs");

        CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
        fakeResponseDto.setName("TVs");
        fakeResponseDto.setId(7L);

        /// 2=> create fake entity to use in mapper
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setName("TVs");
        fakeEntity.setId(7L);

        ///3=> call repo and mapper
        when(categoryMapper.mapToEntity(fakeRequestDto)).thenReturn(fakeEntity);
        when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
        when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);

        /// 4=> call our function for testing
        CategoryResponseDto result=categoryService.createCategory(fakeRequestDto);
        assertNotNull(result);
        assertEquals("TVs",result.getName());
        assertEquals(7L,result.getId());

        /// 5=> check our work flow
        verify(categoryMapper,times(1)).mapToEntity(fakeRequestDto);
        verify(categoryRepo, times(1)).save(fakeEntity);
        verify(categoryMapper, times(1)).mapToDto(fakeEntity);

    }

    //////////
    @Test
    void createCategory_WhenDuplicateName_ShouldThrowException()
    {
        String duplicateName="Books";
        /// 1=> create fake dto
        CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
        fakeRequestDto.setName(duplicateName); /// name already exists in our DB

        //// no need to create entity because we use only name

        ///2=> call repo
        when(categoryRepo.existsByName(duplicateName)).thenReturn(true);

        /// 4=> call our function for testing
        assertThrows(DuplicateRecordException.class,()->
                categoryService.createCategory(fakeRequestDto));

        /// 5=> check our work flow
        verify(categoryRepo, times(1)).existsByName(duplicateName);
        verify(categoryMapper, never()).mapToDto(any());
        verify(categoryRepo, never()).save(any());
    }

    //////////////////////////UPDATE CATEGORY/////////////////////////////////////
    @Test
    void updateCategory_WhenSameName_ShouldUpdateCategory()
    {
        /// 1=> create fake request dto
        String newName="TVs";
        String newDescription="TVs new description";
        Long oldId=1L;
        CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
        fakeRequestDto.setName(newName);
        fakeRequestDto.setDescription(newDescription);

        CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
        fakeResponseDto.setName(newName);
        fakeResponseDto.setId(oldId);
        fakeResponseDto.setDescription(newDescription);

        /// 2=> create fake entity to use in mapper
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setName(newName);
        fakeEntity.setId(oldId);
        fakeEntity.setDescription(newDescription);

        ///3=> call repo and mapper
        when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
//        when(categoryRepo.existsByName(newName)).thenReturn(false);
        /// when is implemented to return a value not void
        /// donothing => means that don't return any thing
        doNothing().when(categoryMapper).updateEntityFromDto(fakeRequestDto,fakeEntity);
        when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
        when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);


        /// 4=> call our function for testing
        CategoryResponseDto result=categoryService.updateCategory(fakeRequestDto,oldId);
        assertNotNull(result);
        assertEquals(newName,result.getName());
        assertEquals(oldId,result.getId());

        /// 5=> check our work flow
        verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
        verify(categoryMapper, times(1)).updateEntityFromDto(fakeRequestDto,fakeEntity);
        verify(categoryRepo, times(1)).save(fakeEntity);
        verify(categoryMapper, times(1)).mapToDto(fakeEntity);
    }
    /////////
    @Test
    void updateCategory_WhenDifferentName_ShouldCreateCategory()
    {
        /// 1=> create fake request dto
        String newName="TVs";
        Long oldId=1L;
        CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
        fakeRequestDto.setName(newName);

        CategoryResponseDto fakeResponseDto=new CategoryResponseDto();
        fakeResponseDto.setName(newName);
        fakeResponseDto.setId(oldId);

        /// 2=> create fake entity to use in mapper
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setName("old tv description");
        fakeEntity.setId(oldId);

        ///3=> call repo and mapper
        when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
        when(categoryRepo.existsByName(newName)).thenReturn(false);
        /// when is implemented to return a value not void
        /// donothing => means that don't return any thing
        doNothing().when(categoryMapper).updateEntityFromDto(fakeRequestDto,fakeEntity);
        when(categoryRepo.save(fakeEntity)).thenReturn(fakeEntity);
        when(categoryMapper.mapToDto(fakeEntity)).thenReturn(fakeResponseDto);


        /// 4=> call our function for testing
        CategoryResponseDto result=categoryService.updateCategory(fakeRequestDto,oldId);
        assertNotNull(result);
        assertEquals(newName,result.getName());
        assertEquals(oldId,result.getId());

        /// 5=> check our work flow
        verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
        verify(categoryRepo, times(1)).existsByName(newName);
        verify(categoryMapper, times(1)).updateEntityFromDto(fakeRequestDto,fakeEntity);
        verify(categoryRepo, times(1)).save(fakeEntity);
        verify(categoryMapper, times(1)).mapToDto(fakeEntity);
    }

    /////////
    @Test
    void updateCategory_WhenNameAlreadyExists_ShouldThrowException()
    {
        /// 1=> create fake request dto
        String nameAnotherCategory="new  Books";
        String oldNameCategoryEntity="Books";

        Long oldId=1L;
        CategoryRequestDto fakeRequestDto=new CategoryRequestDto();
        fakeRequestDto.setName(nameAnotherCategory);

        /// 2=> create fake entity to use in mapper
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setName(oldNameCategoryEntity);
        fakeEntity.setId(oldId);

        ///3=> call repo and mapper
        when(categoryRepo.findByIdOrThrow(oldId)).thenReturn(fakeEntity);
        when(categoryRepo.existsByName(nameAnotherCategory)).thenReturn(true);



        /// 4=> call our function for testing
        assertThrows(DuplicateRecordException.class,
                ()-> categoryService.updateCategory(fakeRequestDto,oldId));


        /// 5=> check our work flow
        verify(categoryRepo, times(1)).findByIdOrThrow(oldId);
        verify(categoryRepo, times(1)).existsByName(nameAnotherCategory);
        verify(categoryMapper, never()).updateEntityFromDto(fakeRequestDto,fakeEntity);
        verify(categoryRepo, never()).save(fakeEntity);
        verify(categoryMapper, never()).mapToDto(fakeEntity);
    }
    //////////////////////////DELETE CATEGORY/////////////////////////////////////
    @Test
    void deleteCategory_WhenFoundInDB_ShouldDeleteCategory()
    {
        Long id=1L;
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setId(id);

        when(categoryRepo.findByIdOrThrow(id)).thenReturn(fakeEntity);
        doNothing().when(productRepo).moveProductsToDefaultCategory(id);

        categoryService.deleteCategory(id);
         /// no assert here because return type is void
        verify(categoryRepo,times(1)).findByIdOrThrow(id);
        verify(categoryRepo,times(1)).delete(fakeEntity);
        verify(productRepo,times(1)).moveProductsToDefaultCategory(id);
    }

    ////////
    @Test
    void deleteCategory_WhenDefaultCategory_ShouldThrowException()
    {
        Long id=999L;
        CategoryEntity fakeEntity=new CategoryEntity();
        fakeEntity.setId(id);

        assertThrows(IllegalArgumentException.class,()->
                categoryService.deleteCategory(id));
        /// no assert here because return type is void
        verify(categoryRepo,never()).findByIdOrThrow(id);
        verify(categoryRepo,never()).delete(fakeEntity);
        verify(productRepo,never()).moveProductsToDefaultCategory(id);
    }

    ////////
    @Test
    void deleteCategory_WhenNotFoundInDB_ShouldDeleteCategory()
    {
        Long id=10L; /// not found in our DB

        when(categoryRepo.findByIdOrThrow(id)).thenThrow(
                ResourceNotFoundException.class
        );
        assertThrows(ResourceNotFoundException.class,()->
                categoryService.deleteCategory(id));

        /// no assert here because return type is void
        verify(categoryRepo,times(1)).findByIdOrThrow(id);
        verify(categoryRepo,never()).delete(any());
        verify(productRepo,never()).moveProductsToDefaultCategory(id);
    }

}
