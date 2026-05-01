package com.global.order_api.core.base;

import java.util.List;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseService<T extends BaseEntity<ID> , ID extends Number> {
	private final BaseRepo<T, ID> baseRepo;
	
	
	// read methods
	
	public T findById(ID id)
	{
		return baseRepo.findByIdOrThrow(id);
	}
	public List<T> findAll()
	{
		return baseRepo.findAll();
	}
	
	// write methods
	
	public T save(T entity)
	{
		return baseRepo.save(entity);
	}
	public T update(T entity)
	{
		baseRepo.findByIdOrThrow(entity.getId());
		return baseRepo.save(entity);
	}
	
	// delete
	public void delete(ID id) {
		baseRepo.findByIdOrThrow(id);
		baseRepo.deleteById(id);
    }
}
