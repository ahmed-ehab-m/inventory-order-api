package com.global.order_api.feature.order.service;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.order.specification.OrderFilterRequest;
import com.global.order_api.feature.order.dto.OrderResponseDto;
import com.global.order_api.feature.order.entity.OrderEntity;
import com.global.order_api.feature.order.enums.OrderStatus;
import com.global.order_api.feature.order.mapper.OrderMapper;
import com.global.order_api.feature.order.repo.OrderRepo;
import com.global.order_api.feature.order.specification.OrderSpecification;
import com.global.order_api.feature.payment.PaymentEntity;
import com.global.order_api.feature.payment.PaymentRepo;
import com.global.order_api.feature.payment.PaymentStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminOrderService extends BaseService<OrderEntity, Long> {

    private final OrderMapper orderMapper;
    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;
    private final OrderService orderService; // To reuse processOrderCancellation

    public AdminOrderService(BaseRepo<OrderEntity, Long> baseRepo, OrderMapper orderMapper, OrderRepo orderRepo, PaymentRepo paymentRepo, OrderService orderService) {
        super(baseRepo);
        this.orderMapper = orderMapper;
        this.orderRepo = orderRepo;
        this.paymentRepo = paymentRepo;
        this.orderService = orderService;
    }


    // ==================================================================================
    //                                1. READING METHODS (ADMIN)
    // ==================================================================================

    /// Get order By Id to get order details for Admin
    public OrderResponseDto getOrderById(Long orderId) {
        OrderEntity order = orderRepo.findByIdOrThrow(orderId);
        return orderMapper.mapToDto(order);
    }

    /// Get All Orders
    /// smart method for pagination
    /// / GET ALL ORDERS FOR ADMIN
    public PageResponse<OrderResponseDto> getAllOrders(OrderFilterRequest filter) {
        ///// 1=> take user input => "ASC" OR "DESC" from headers
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());
        ////// 2=> Pageable => take all user input page , size , sort
        ///// will be translated to SQL
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        ///// holds data + meta data about it
        Specification<OrderEntity> finalSpec = OrderSpecification.buildFilter(filter);

        //// 3=> call Repo
        Page<OrderEntity> orderEntityPage = orderRepo.findAll(finalSpec, pageable);
        ///// map Entities to Dtos
        List<OrderResponseDto> orderResponseDtoList = orderMapper.mapToDtoList(orderEntityPage.getContent());
        return PageResponse.from(orderEntityPage, orderResponseDtoList);
    }


    // ==================================================================================
    //                                2. WRITING METHODS (ADMIN)
    // ==================================================================================

    @Transactional
    public OrderResponseDto updateOrderStatusByAdmin(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepo.findByIdOrThrow(orderId);
        /// admin cancel user order
        if (newStatus == OrderStatus.CANCELLED) {
            if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.DELIVERED) {
                orderService.processOrderCancellation(order);
            }
        } else {
            /// edit payment table
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED) {
                List<PaymentEntity> payments = paymentRepo.findByOrderId(orderId);
                for (PaymentEntity payment : payments) {
                    /// get pending status
                    if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                        payment.setPaymentStatus(PaymentStatus.SUCCESS);
                    }
                }
                paymentRepo.saveAll(payments);
            }
        }
        OrderEntity updatedOrder = orderRepo.save(order);
        return orderMapper.mapToDto(updatedOrder);
    }

    @Caching(evict = {
            @CacheEvict(value = "ordersPage", allEntries = true),
            @CacheEvict(value = "orders", key = "#orderId")
    })
    /// hard-delete
    @Transactional
    public void hardDeleteOrder(Long orderId) {
        /// 1=> get status
        String orderStatus = orderRepo.findOrderStatusByIdIncludingDeleted(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        /// 2=> Business Logic Check
        if (!orderStatus.equals("CANCELLED")) {
            throw new BusinessLogicException("error.order.hard.delete");
        }
        orderRepo.hardDeleteOrderItems(orderId);
        orderRepo.hardDelete(orderId);
    }
}