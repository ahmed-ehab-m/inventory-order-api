package com.global.order_api.feature.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.global.order_api.feature.category.CategoryEntity;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

/// FOR ADVANCED FILTERS IN RUN TIME

public class ProductSpecification {
	public static Specification<ProductEntity> buildFilter(ProductFilterRequest filter)
	{
		// root => Entity
		// criteriaBuilder => holds all tools , Arthimatic + Logical  operations 
		// query => the full SQL statement
		return(root,query,criteriaBuilder)->
		{
			// LIST contains Predicate => DYNNAMIC WHERE CLAUSES
			// Predicate => interface , holds type of action sql
			// , column name , value , result
			List<Predicate> predicates = new ArrayList<>();
			// KEYWORD SEARCH
			if(filter.getSearchKeyword() !=null && !filter.getSearchKeyword().isBlank())
			{
				String keyWord="%"+filter.getSearchKeyword().toLowerCase()+"%";
				predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),keyWord));
						
			}
			///////////////////////////////////
			if(filter.getCategoryId() !=null)
			{
				// 1 join with categories table
				Join<ProductEntity, CategoryEntity> categoryJoin=root.join("category");
				// get where id equals
				predicates.add(criteriaBuilder.equal(categoryJoin.get("id"), filter.getCategoryId()));
			}
			//////////////////////////////////
			if(filter.getMinPrice() !=null)
			{
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
			}
			
			//////////////////////////////////
			if(filter.getMaxPrice() !=null)
			{
			predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
			}
			//////////////////////////////////
			if(filter.getInStockOnly() !=null && filter.getInStockOnly())
			{
				predicates.add(criteriaBuilder.greaterThan(root.get("stockCount"), 0));
			}
			predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
