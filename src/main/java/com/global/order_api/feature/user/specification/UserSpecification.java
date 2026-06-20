package com.global.order_api.feature.user.specification;

import com.global.order_api.feature.user.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    /// ///////////////////////////
    public static Specification<UserEntity> buildFilter(UserFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            // List not array because list is more easily add operation
            List<Predicate> predicates = new ArrayList<>();
            ///////////
            /// USER NAME
            if (filter.getUserName() != null && !filter.getUserName().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("name"),
                        "%" + filter.getUserName() + "%"));
            }
            /// USER LOCATION
            if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
                predicates.add(criteriaBuilder.like(root.get("location"),
                        "%" + filter.getLocation() + "%"));
            }
            /// USER ROLE
            if (filter.getRole() != null) {
                predicates.add(criteriaBuilder.like(root.get("role"),
                        "%" + filter.getRole() + "%"));
            }

            /// USER DELETED STATUS
            if (filter.getIsDeleted() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDeleted"), filter.getIsDeleted()));
            }
            // and accept only Array not List
            // new Predicate[0] => because toArray() return object data type []
            // and we want Predicate so we only convert the type here
            // [0] instead of pass the real size because (Java 8) =>
            // better performance and jvm works with it smart
            return criteriaBuilder.and(predicates.toArray(predicates.toArray(new Predicate[0])));
        };
    }
}
