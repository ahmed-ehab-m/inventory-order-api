package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Repository
public interface OrderRepo extends BaseRepo<OrderEntity,Long> , JpaSpecificationExecutor<OrderEntity> {


    //// GET METHODS
    /// we comment findAll because => we extends JpaSpecificationExecutor
    //// which make findAll work as pagination function
    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    /// get order even if soft-delete true
    /// check status and to make hibernate return entity even if soft-deleted
    @Query(value = "SELECT status FROM orders WHERE id = :id", nativeQuery = true)
    Optional<String> findOrderStatusByIdIncludingDeleted(@Param("id") Long id);
//    @Query(value = "SELECT * FROM orders WHERE id = :id",nativeQuery = true)
//    Optional<OrderEntity>findByIdIncludingDeleted(@Param("id") Long id);
    /// WRITING METHODS
    /// Hard Delete
    @Modifying
    @Query(value = "DELETE FROM order_item WHERE order_id = :id", nativeQuery = true)
    void hardDeleteOrderItems(@Param("id") Long id);


    @Modifying
    @Query(value = "DELETE from orders where id = :id",nativeQuery = true)
    void hardDelete(@Param("id")Long id);

    /// Restore Order
    @Modifying
    @Query(value = "UPDATE orders set is_deleted=false where id = :id",nativeQuery = true)
    void restoreOrder(@Param("id")Long id);

}

