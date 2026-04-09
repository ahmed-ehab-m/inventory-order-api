package com.global.order_api.feature.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.service.FileUploadService;
import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.category.CategoryRepo;


@Service
public class ProductService extends BaseService<ProductEntity, Long> {
	
	private final ProductRepo productRepo;
	private final CategoryRepo categoryRepo;
	private final ProductMapper productMapper;
	private final FileUploadService fileUploadService;
	
	public ProductService(BaseRepo<ProductEntity, Long> baseRepo, ProductRepo productRepo,
			ProductMapper productMapper, CategoryRepo categoryRepo ,FileUploadService fileUploadService) {
		super(baseRepo);
		this.productRepo = productRepo;
		this.productMapper = productMapper;
		this.categoryRepo=categoryRepo;
		this.fileUploadService=fileUploadService;
	}
	
	////////////////////////////
	/// READ METHODS
	///////////////
	public ProductResponseDto getProductByIdWithCategory(Long id)
	{
		ProductEntity productEntity = productRepo.findByIdWithCategory(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "name", id));
		return productMapper.mapToDto(productEntity);
	}
	//////////////////
	public ProductResponseDto getProductByName(String name)
	{
		ProductEntity productEntity = productRepo.findByName(name)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "name", name));
		return productMapper.mapToDto(productEntity);
	}
	//////////////////
	// smart method for pagination
	// take filter => smart object contains page number , size ,sort type , keyword
	// return pageResponse we created in core folder
	public PageResponse<ProductResponseDto> getProductsPage(ProductFilterRequest filter) {
	// take user input => "ASC" OR "DESC" from headers
		Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
		//Pageable => take all user input 
		// will be translated to SQL 
		Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
		// holds data + meta data about it
		Page<ProductEntity> productPage;
		if(filter.getSearchKeyword() !=null && !filter.getSearchKeyword().isBlank())
		{
			productPage=productRepo.findByNameContainingIgnoreCase(filter.getSearchKeyword(), pageable);
		}
		// if user want all categories
		else
		{
			productPage=productRepo.findAll(pageable);
		}
		List<ProductResponseDto> dtoList=productMapper.mapToDtoList(productPage.getContent());
		return PageResponse.from(productPage, dtoList);      
	}
	
	////////////////////

	public PageResponse<ProductResponseDto> getProductByCategoryId(ProductFilterRequest filter,Long id)
	{
		// first check if category is existed or not
	    categoryRepo.findByIdOrThrow(id);
		Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
		//Pageable => take all user input 
		// will be translated to SQL 
		Pageable pageable=PageRequest.of(filter.getPage(), filter.getSize(), sort);
		Page<ProductEntity> productPage;
		productPage = productRepo.findByCategoryId(id,pageable);
		List<ProductResponseDto> dtoList=productMapper.mapToDtoList(productPage.getContent());
		return PageResponse.from(productPage, dtoList);      
	}
	
	///WRITE METHODS
	@Transactional
	public ProductResponseDto createProduct(ProductRequestDto requestDto , MultipartFile image)
	{
		// first check if category is existed
		CategoryEntity categoryEntity = categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
		ProductEntity entity=productMapper.mapToEntity(requestDto);
		entity.setCategory(categoryEntity);
		// upload image
		if (image != null && !image.isEmpty()) {
	        String imageUrl = fileUploadService.uploadImage(image);
	        entity.setImage(imageUrl);
	    }
		return productMapper.mapToDto(save(entity));
	}
	
	@Transactional
	public ProductResponseDto updateProduct(ProductRequestDto requestDto,Long id ,MultipartFile newImage)
	{
		// first check the category is existed
		ProductEntity existingEntity= productRepo.findByIdOrThrow(id);
		// second check if category id is changed
		if(requestDto.getCategoryId() !=null)
		{
			CategoryEntity categoryEntity =categoryRepo.findByIdOrThrow(requestDto.getCategoryId());
			existingEntity.setCategory(categoryEntity);
		}
		// check about new Image
		if(newImage !=null && !newImage.isEmpty())
		{
			// check if product already has image
			if(existingEntity.getImage() !=null)
			{
				fileUploadService.deleteImage(existingEntity.getImage());
			}
			// add new Image
			String newImageurl=fileUploadService.uploadImage(newImage);
			existingEntity.setImage(newImageurl);
		}
		// update the remaining entity data
		productMapper.updateEntityFromDto(requestDto, existingEntity);
		return productMapper.mapToDto(save(existingEntity));
	}
	
	///DELETE METHODS
	@Transactional
	public void deleteProduct(Long id) {
	    delete(id);
    }
	
	// HARD DELETE
	public void forceDeleteProduct(Long id)
	{
		String imageUrl=productRepo.getImageUrlByIdEvenIfDeleted(id);
		if (imageUrl != null) {
			fileUploadService.deleteImage(imageUrl);
		}
		productRepo.hardDeleteProduct(id);
	}
	
}
