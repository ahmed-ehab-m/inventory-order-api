package com.global.order_api.feature.category;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.DuplicateRecordException;
import com.global.order_api.core.exception.ResourceNotFoundException;

@Service // 
public class CategoryService extends BaseService<CategoryEntity, Long> {
	
	private final CategoryRepo categoryRepo;
	private final CategoryMapper categoryMapper;

	public CategoryService(BaseRepo<CategoryEntity, Long> baseRepo, CategoryRepo categoryRepo,
			CategoryMapper categoryMapper) {
		super(baseRepo);
		this.categoryRepo = categoryRepo;
		this.categoryMapper = categoryMapper;
	}
	
	////////////////////////////
	// read Methods
	public CategoryResponseDto findCategoryById(Long id)
	{
		CategoryEntity entity =findById(id);
		return categoryMapper.mapToDto(entity);
	}
	
	public CategoryResponseDto findCategoryByName(String name)
	{
		CategoryEntity entity =categoryRepo.findByName(name)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "name", name));
		return categoryMapper.mapToDto(entity);
	}
	
	// smart method for pagination
	// take filter => smart object contains page number , size ,sort type , keyword
	// return pageResponse we created in core folder
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
	//soft delete method
	public void deleteCategory(Long id) {
	    delete(id);
    }
}
