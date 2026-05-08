package com.global.order_api.feature.category;

import java.util.List;

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

public class CategoryService extends BaseService<CategoryEntity, Long> {
	
	private final CategoryRepo categoryRepo;
	private final CategoryMapper categoryMapper;
	private final ProductRepo productRepo;

	public CategoryService(BaseRepo<CategoryEntity, Long> baseRepo, CategoryRepo categoryRepo,
			CategoryMapper categoryMapper ,ProductRepo productRepo) {
		super(baseRepo);
		this.categoryRepo = categoryRepo;
		this.categoryMapper = categoryMapper;
		this.productRepo=productRepo;
	}
	
	////////////////////////////
	// read Methods
	public CategoryResponseDto findCategoryById(Long id)
	{
		CategoryEntity entity =categoryRepo.findByIdOrThrow(id);
		return categoryMapper.mapToDto(entity);
	}

	/// to get the identical name not containing
	/// for internal logic => if i want to get the category to link product
	/// for front-end go to mobiles page
	/// so front-end should send the identical name
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
	//hard delete method and move products into Uncategorized
	@Transactional
	public void deleteCategory(Long id) {
		Long DEFAULT_CATEGORY_ID=999L;
		
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
