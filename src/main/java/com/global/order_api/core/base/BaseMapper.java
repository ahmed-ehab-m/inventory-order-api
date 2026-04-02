package com.global.order_api.core.base;

import java.util.List;

// E for Entity
// D for DTO

public interface BaseMapper<E ,D> {
	
	E mapToEntity(D dto);
	D mapToDto(E entity);
	
	List<E> mapToEntityList(List<D> dtoList);
	List<D> mapToDtoList(List<E> entityList);

}
