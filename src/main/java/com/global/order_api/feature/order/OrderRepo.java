package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends BaseRepo<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    // ==================================================================================
    //                                1. READ METHODS
    // ==================================================================================

    /// / GET METHODS
    /// we comment findAll because => we extends JpaSpecificationExecutor
    /// / which make findAll work as pagination function
    /// / indexing //////////
    /// id pk =>cluster index => very fast
    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    /// get order even if soft-delete true
    /// check status and to make hibernate return status (projection) even if soft-deleted
    /// / indexing //////////
    /// id pk =>cluster index => very fast
    @Query(value = "SELECT status FROM orders WHERE id = :id", nativeQuery = true)
    Optional<String> findOrderStatusByIdIncludingDeleted(@Param("id") Long id);

//  @Query(value = "SELECT * FROM orders WHERE id = :id",nativeQuery = true)
//  Optional<OrderEntity>findByIdIncludingDeleted(@Param("id") Long id);


    // ==================================================================================
    //                                2. WRITE, DELETE & RESTORE METHODS
    // ==================================================================================

    /// WRITING METHODS
    /// Hard Delete
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM order_item WHERE order_id = :id", nativeQuery = true)
    void hardDeleteOrderItems(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE from orders where id = :id", nativeQuery = true)
    void hardDelete(@Param("id") Long id);

    /// Restore Order
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE orders set is_deleted=false where id = :id", nativeQuery = true)
    void restoreOrder(@Param("id") Long id);


    // ==================================================================================
    //                                3. SCHEDULING METHODS
    // ==================================================================================

    /// for Scheduling => remove PENDING ORDERS after 24 Hours
    List<OrderEntity> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time);

}