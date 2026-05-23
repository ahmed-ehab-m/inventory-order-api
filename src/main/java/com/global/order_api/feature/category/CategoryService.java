package com.global.order_api.feature.category;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.product.ProductRepo;

@Service //
@Slf4j
public class CategoryService extends BaseService<CategoryEntity, Long> {
	
	private final CategoryRepo categoryRepo;
	private final CategoryMapper categoryMapper;
	private final ProductRepo productRepo;

	/// to save our memory
	private static  final Long DEFAULT_CATEGORY_ID=999L;

	public CategoryService(BaseRepo<CategoryEntity, Long> baseRepo, CategoryRepo categoryRepo,
			CategoryMapper categoryMapper ,ProductRepo productRepo) {
		super(baseRepo);
		this.categoryRepo = categoryRepo;
		this.categoryMapper = categoryMapper;
		this.productRepo=productRepo;
	}
	//// CACHING => READING-HEAVY , WRITE-RARE
	////////////////////////////
	// read Methods
    ///
	/// caching here for Business Logic
    @Cacheable(value = "categories", key = "#id")
	public CategoryResponseDto findCategoryById(Long id)
	{
		/// avoid + to avoid String concatenation for better performance
       log.debug("go to data base not cache to get category id : {}",id);
		CategoryEntity entity =categoryRepo.findByIdOrThrow(id);
		return categoryMapper.mapToDto(entity);
	}

	/// to get the identical name not containing
	/// to enhance SEO or if reading from Excel sheet so i read names not IDs
	/// or Third-Party Integrations Like Odoo or SAP because they may send requests
	/// using name not IDs
	/// caching here for Business Logic
    @Cacheable(value = "categories", key = "#name")
	public CategoryResponseDto findCategoryByName(String name)
	{
		CategoryEntity entity =categoryRepo.findByName(name)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "name", name));
		return categoryMapper.mapToDto(entity);
	}
	
	/// smart method for pagination
	///  take filter => smart object contains page number , size ,sort type , keyword
	/// return pageResponse we created in core folder
	/// for Advanced Search Bar and Filters
	/// SpEL (Spring Expression Language)
	@Cacheable(
			value = "categoriesPage",
			key = "#filter.generateCacheKey()"
	)
	public PageResponse<CategoryResponseDto> getCategoriesPage(CategoryFilterRequestDto filter) {
		// take user input => "ASC" OR "DESC" from headers
	       Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
	       //Pageable => take all user input 
	       // will be translated to SQL 
	      Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
	      // holds data + meta data about it
	      Page<CategoryEntity> categoryPage;
	      if(filter.getSearchKeyword() !=null && !filter.getSearchKeyword().isBlank())
	      {
	    	  categoryPage=categoryRepo.findByNameContainingIgnoreCase(filter.getSearchKeyword(), pageable);
	      }
	      // if user want all categories
	      else
	      {
	    	  categoryPage=categoryRepo.findAll(pageable);
	      }
	      List<CategoryResponseDto> dtoList=categoryMapper.mapToDtoList(categoryPage.getContent());
	      return PageResponse.from(categoryPage, dtoList);
	      
	}
	
	////////////////////////////
	// write methods
	/// remove all page from cache
	/// editing methods here , very minor or low editing here so remove all
	@CacheEvict(value = "categoriesPage",allEntries = true)
	@Transactional
	public CategoryResponseDto createCategory(CategoryRequestDto requestDto)
	{
		// first check unique name
		if(categoryRepo.existsByName(requestDto.getName()))
		{
			throw new DuplicateRecordException("Category", "name", requestDto.getName());
		}
		CategoryEntity entity=categoryMapper.mapToEntity(requestDto);
		return categoryMapper.mapToDto(save(entity));
	}

    //// remove this category from cache memory using id
	@Caching(evict = {
			@CacheEvict(value = "categoriesPage", allEntries = true),
//			@CacheEvict(value = "categories", key = "#id"),
			/// problem here we remove new name !! and old name still in cache
//			@CacheEvict(value = "categories", key = "#requestDto.name")
			/// remove names + ids
			@CacheEvict(value = "categories", allEntries = true)
	})
	@Transactional
	public CategoryResponseDto updateCategory(CategoryRequestDto requestDto,Long id)
	{
		// first check the category is existed
		CategoryEntity existingEntity= findById(id);
		// check if user want to change name of category must be unique
		if(!existingEntity.getName().equalsIgnoreCase(requestDto.getName()) && categoryRepo.existsByName(requestDto.getName()))
		{
			throw new DuplicateRecordException("Category", "name", requestDto.getName());
		}
		// update the entity data
		categoryMapper.updateEntityFromDto(requestDto, existingEntity);
		return categoryMapper.mapToDto(save(existingEntity));
	}
	
	////////////////////////////
    ///
	/// remove names + ids
	@Caching(evict = {
			@CacheEvict(value = "categories", allEntries = true),
			@CacheEvict(value = "categoriesPage", allEntries = true)
	})
    //hard delete method and move products into Uncategorized
	@Transactional
	public void deleteCategory(Long id) {

		// first check about default category
		// equals because this an object not primitive
		// if i use == here i compare the memory address
		if(id.equals(DEFAULT_CATEGORY_ID))
		{
			throw new IllegalArgumentException("{validation.category.default}");
		}
		CategoryEntity existingEntity = categoryRepo.findByIdOrThrow(id);
		// move here
		productRepo.moveProductsToDefaultCategory(id);
	    categoryRepo.delete(existingEntity);
    }
	
//	/////////HARD DELETE METHOD
//	@Transactional
//	public void forceDeleteCategory(Long id) {
//		categoryRepo.hardDeleteCategory(id);
//	}
}
