package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepo extends BaseRepo<OrderEntity,Long> , JpaSpecificationExecutor<OrderEntity> {

    /// Get Methods
    /// Get All Orders + pagination + sort by time , price ,status
    /// Get by user id
    /// Writing Methods
    /// create order
    /// cancel order
    /// soft delete
    /// hard delete
}
