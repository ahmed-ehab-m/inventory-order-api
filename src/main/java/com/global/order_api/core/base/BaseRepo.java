package com.global.order_api.core.base;

import com.global.order_api.core.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

// T generic for entity
@NoRepositoryBean // 
public interface BaseRepo<T extends BaseEntity<ID>, ID extends Number> extends JpaRepository<T, ID> {

    // Apply DRY , exception consistency , service now focus only logic
    // optional => contains the object or Null
    // function return optional to prevent NullPointerException (before Java
    // here we throw an exception if null
    default T findByIdOrThrow(ID id) {
        return findById(id).orElseThrow(() ->
                new ResourceNotFoundException("error.resource.not.found", new Object[]{id}));
        // add it into array of object because id is a generic
    }

//	@Override
//	@Query("SELECT e FROM #{#entityName} e WHERE e.isDeleted = false")
//	Page<T> findAll(Pageable pageable);
}
