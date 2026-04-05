package com.global.order_api.feature.category;

import java.util.List;

import org.springframework.stereotype.Service;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
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

	// read methods
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
	
	public List<CategoryResponseDto> findAllCategories() {
	        return categoryMapper.mapToDtoList(findAll());
	}
	
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
		// check name of category must be unique
		if(existingEntity.getName().equals(requestDto.getName()) && categoryRepo.existsByName(requestDto.getName()))
		{
			throw new DuplicateRecordException("Category", "name", requestDto.getName());
		}
		// update the entity data
		categoryMapper.updateEntityFromDto(requestDto, existingEntity);
		return categoryMapper.mapToDto(save(existingEntity));
	}
	
	//soft delete method
	public void deleteCategory(Long id) {
	    delete(id);
    }
}
