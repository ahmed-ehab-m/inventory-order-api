package com.global.order_api.feature.product.specification;

import com.global.order_api.feature.category.entity.CategoryEntity;
import com.global.order_api.feature.product.entity.ProductEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/// FOR ADVANCED FILTERS IN RUN TIME

public class ProductSpecification {
    public static Specification<ProductEntity> buildFilter(ProductFilterRequest filter, boolean isDeleted) {
        // root => Entity
        // criteriaBuilder => holds all tools , Arthimatic + Logical  operations
        // query => the full SQL statement
        return (root, query, criteriaBuilder) ->
        {

            /// join fetch => to get category in same query to  avoid n+1 problem
            /// when i ask spring to get page
            /// spring make first query to get data and second to COUNT return Long
            /// if we don't create this condition , spring will FETCH category in 2 queries
            /// so in COUNT query => QuerySyntaxException
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                /// left => to get all products even if product's category is Uncategorized
                root.fetch("category", JoinType.LEFT);
            }
            // LIST contains Predicate => DYNNAMIC WHERE CLAUSES
            // Predicate => interface , holds type of action sql
            // , column name , value , result
            List<Predicate> predicates = new ArrayList<>();
            ///////////////////////////////////
            if (filter.getCategoryId() != null) {
                // 1 join with categories table
                Join<ProductEntity, CategoryEntity> categoryJoin = root.join("category");
                // get where id equals
                predicates.add(criteriaBuilder.equal(categoryJoin.get("id"), filter.getCategoryId()));
            }
            //////////////////////////////////
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }

            //////////////////////////////////
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }
            //////////////////////////////////
            if (filter.getInStockOnly() != null && filter.getInStockOnly()) {
                predicates.add(criteriaBuilder.greaterThan(root.get("stockCount"), 0));
            }
            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));
            /// comment because we use @filter above product entity
//			predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
