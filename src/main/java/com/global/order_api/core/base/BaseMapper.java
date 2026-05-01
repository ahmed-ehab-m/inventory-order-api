package com.global.order_api.core.base;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.category.CategoryRequestDto;

// E for Entity
// D for DTO

public interface BaseMapper<E ,REQ,RES> {
	
	E mapToEntity(REQ dto);
	RES mapToDto(E entity);
	
	List<RES> mapToDtoList(List<E> entityList);
	
	// void because we edit the same object in memory
	// take the source , target
	// @mapping target => tell mapstruct that => don't create a new obj ,just update it
	// better from using toEntity because it creates new object and the id be null
	// and created at null , so db will insert another record not update
	
	// tell mapstruct that => if field = null in request ignore it (partial update)
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromDto(REQ dto, @MappingTarget E entity);


}
