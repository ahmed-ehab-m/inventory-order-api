package com.global.order_api.feature.order;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class OrderSpecification {
    public static Specification<OrderEntity> buildFilter(OrderFilterRequest filter) {
        return (root, query, criteriaBuilder) ->
        {
            List<Predicate> predicates = new ArrayList<>();
            /////////////////
            ///  1=>get the keyword
            if (StringUtils.hasText(filter.getKeyWord())) {
                String keyWord = filter.getKeyWord();
                String likePattern = "%" + keyWord + "%";

                List<Predicate> keywordPredicates = new ArrayList<>();

                /// order id
                if (keyWord.matches("\\d+")) {
                    keywordPredicates.add(criteriaBuilder.equal(root.get("id"), Long.valueOf(keyWord)))
                }
                /// user details
                /// join operations => because search in 2 tables User ,Order
                /// get all orders even if no user (Guest) or no products (soft delete) (DefenseProgramming)
                /// to get all orders even if problem occurs in other tables
                /// left is orders , right is users
                Join<Object, Object> userJoin = root.join("user", JoinType.LEFT);
                /// User Email
                keywordPredicates.add(criteriaBuilder.like(userJoin.get("email"), likePattern));
                /// User Name
                keywordPredicates.add(criteriaBuilder.like(userJoin.get("name"), likePattern));
                /// User location
                keywordPredicates.add(criteriaBuilder.like(userJoin.get("location"), likePattern));

                ////// Order items
                //// 1=> join with order item
                Join<Object, Object> itemJoin = root.join("order_item", JoinType.LEFT);
                //// 2=> join with product
                Join<Object, Object> productJoin = itemJoin.join("order_item", JoinType.LEFT);
                keywordPredicates.add(criteriaBuilder.like(productJoin.get("name"), likePattern));

                /// or => any field matched
                predicates.add(criteriaBuilder.or(keywordPredicates.toArray(new Predicate[0])));
                /// Preventing repetition
                query.distinct(true);
            }
            /// 2=> Order Status
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            /////////////////
            /// 3=> Min Price
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalPrice"), filter.getMinPrice()));
            }

            /////////////////
            /// 4 =>  Max Price
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalPrice"), filter.getMaxPrice()));
            }
            /////////
            //// 5 => From Date
            if (filter.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("created_at"),
                        /// ex 1:30 PM yesterDay in DB => 01:30:00 2026-04-28 (LocalDateTime)
                        /// front-end send => 2026-04-28
                        /// java , db want time not only day  so java add 00:00:00
                        /// atStartOfDay => 2026-04-28 00:00:00 first second of day
                        filter.getFromDate().atStartOfDay()));
            }
            /////////////////
            /// 6 => To Date
            if (filter.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"),
                        ///  2026-04-28 23:59:59 last second of day
                        filter.getToDate().atTime(23, 59, 59)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));

        };
    }
}