package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.cart.CartEntity;
import com.global.order_api.feature.cart.CartItemEntity;
import com.global.order_api.feature.cart.CartRepo;
import com.global.order_api.feature.payment.*;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.product.ProductRepo;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRepo;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
public class OrderService extends BaseService<OrderEntity, Long> {

    private final OrderMapper orderMapper;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final CartRepo cartRepo;
    private final PaymentService paymentService;

    @Value("${app.order.cleanup.hours-limit}")
    private int hoursLimit;

    public OrderService(BaseRepo<OrderEntity, Long> baseRepo, OrderMapper orderMapper, OrderRepo orderRepo, UserRepo userRepo, CartRepo cartRepo, PaymentService paymentService) {
        super(baseRepo);
        this.orderMapper = orderMapper;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.cartRepo = cartRepo;
        this.paymentService = paymentService;
    }

    // ==================================================================================
    //                                CACHING STRATEGY NOTES
    // ==================================================================================
    /// USER ORDERS PAGE TTL => Problem => Order status changes
    /// so
    /// ADMIN ALL ORDERS => NO CACHING => VERY HEAVY WRITING
    /// === Data Normalization in Cache ===
    /// problem => we want to know if admin change product's data to cache right data not old data in cart
    /// so we create new DTOS (RawCart , RawCartItem) only for caching
    /// We cache only => user id (for redis debugging) , cart id , items
    /// items => cart item id , product id ,quantity  (not fully product data)
    ///
    /// so if admin change any product data , in product service we clear cahce of this product
    /// then cart cache we get cache miss then we go to db to get new data again
    /// CACHE STRATEGY => READING = CACHE-ASIDE || WRITING = WRITE-AROUND (DB)


    // ==================================================================================
    //                                1. READING METHODS (USER)
    // ==================================================================================

    /// Get order By Id to get order details for user
    public OrderResponseDto getUserOrderById(Long userId, Long orderId) {
        OrderEntity order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderMapper.mapToDto(order);
    }

    /// / GET ONLY USER ORDERS
    /// add user id to separate general orders and user orders
    public PageResponse<OrderResponseDto> getUserOrders(Long userId, OrderFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy());

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        //// user get only his orders
        Specification<OrderEntity> userSpec = (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);

        Specification<OrderEntity> orderSpec = OrderSpecification.buildFilter(filter);
        Specification<OrderEntity> combinedSpec = orderSpec.and(userSpec);
        Page<OrderEntity> orderEntityPage = orderRepo.findAll(combinedSpec, pageable);
        List<OrderResponseDto> orderResponseDtoList = orderMapper.mapToDtoList(orderEntityPage.getContent());
        return PageResponse.from(orderEntityPage, orderResponseDtoList);
    }


    // ==================================================================================
    //                                2. WRITING METHODS (USER)
    // ==================================================================================

    /// / Create an Order
    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequest) {
        /// 1=> get user cart
        CartEntity cartEntity = cartRepo.findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cart", "id", userId)
                );
        /// 2=> map cart Item Entities to OrderItemEntities
        List<CartItemEntity> cartItemsEntities = cartEntity.getItems();

        /// if payment type is ONLINE
        PaymentResponseDto paymentDto = null;

        /// 3=> check if cart is empty
        if (cartItemsEntities == null || cartItemsEntities.isEmpty()) {
            throw new BusinessLogicException("error.cart.empty");
        }
        /// 4 => map order entity now include => shipping address + notes
        OrderEntity orderEntity = orderMapper.mapToEntity(orderRequest);

        /// 5=> link order to his user because we ignore it in mapper
        UserEntity user = userRepo.findByIdOrThrow(userId);
        orderEntity.setUser(user);

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartItemEntity cartEntityItem : cartItemsEntities) {
            /// 6 => get product
            ProductEntity product = cartEntityItem.getProduct();
            /// 7=> get required quantity
            int quantityNeeded = cartEntityItem.getQuantity();

            /// 8=> check & decrease stockCount
            if (product.getStockCount() < quantityNeeded) {
                throw new BusinessLogicException(
                        "error.insufficient.stock",
                        new Object[]{quantityNeeded, product.getStockCount()}
                );
            }
            product.setStockCount(product.getStockCount() - quantityNeeded);
            /// 9=> fill cart item data to order item
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setProduct(cartEntityItem.getProduct());
            orderItem.setQuantity(cartEntityItem.getQuantity());
            BigDecimal currentPrice = cartEntityItem.getProduct().getPrice();
            orderItem.setPrice(currentPrice);
            /// 10=> calc total price
            totalPrice = totalPrice.add(currentPrice.multiply(
                    BigDecimal.valueOf(cartEntityItem.getQuantity())));
            /// 11=> add orders items entities to order entity
            orderEntity.addOrderItem(orderItem);
        }
        /// 12=> set total price
        orderEntity.setTotalPrice(totalPrice);
        /// 13 => set initial status
        orderEntity.setStatus(OrderStatus.PENDING);
        /// 14=> save order in db
        OrderEntity savedOrder = orderRepo.save(orderEntity);
        /// 15 => check payment type to update status
        /// ONLINE => WALLET, CARDS, FAWRY
        if (orderRequest.getPaymentType() != PaymentType.CASH) {
            /// which payment method ?
            String paymentMethod = orderRequest.getPaymentType().name();
            ///  get our link
            paymentDto = paymentService.generatePaymentLink(
                    paymentMethod,
                    userId,
                    totalPrice,
                    orderEntity.getId(),
                    orderRequest.getWalletNumber()
            );
        }
        /// CASH
        else {
            paymentService.createCashPaymentRecord(savedOrder, totalPrice);
        }
        /// 16=> clear cart
        cartRepo.delete(cartEntity);
        /// 17=> map to response dto
        OrderResponseDto responseDto = orderMapper.mapToDto(savedOrder);
        responseDto.setPaymentActionData(paymentDto);
        return responseDto;
    }

    @Transactional
    public PaymentResponseDto retryPayment(Long userId, Long orderId, String walletNumber) {
        /// 1=> get user order
        OrderEntity order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        /// 2=> validate order status must be PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessLogicException("error.order.payment.retry.state", new Object[]{order.getStatus()});
        }
        /// 3=> validate order Payment  must be ONLINE
        if (order.getPaymentType() == PaymentType.CASH) {
            throw new BusinessLogicException("error.order.payment.retry.method");
        }
        /// 4=> generate new payment link
        PaymentResponseDto paymentDto = paymentService.generatePaymentLink(
                order.getPaymentType().name(),
                userId,
                order.getTotalPrice(),
                orderId,
                walletNumber); /// CARD NOT WALLET
        return paymentDto;

    }

    // ==================================================================================
    //                                3. CANCEL & RETURN & DELETE (USER)
    // ==================================================================================

    @Transactional
    /// for user
    public void cancelOrder(Long userId, Long orderId) {
        /// 1=> get user order
        OrderEntity orderEntity = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order", "id", orderId)
                );
        /// 2=> make sure the order not already canceled
        if (orderEntity.getStatus() != OrderStatus.PENDING && orderEntity.getStatus() != OrderStatus.PROCESSING) {
            throw new BusinessLogicException("error.order.state", new Object[]{orderEntity.getStatus()});
        }
        processOrderCancellation(orderEntity);
    }

    @Transactional
    public void returnDeliveredOrder(Long userId, Long orderId) {
        /// 1=> get user order
        OrderEntity orderEntity = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order", "id", orderId)
                );

        /// 2=> make sure the order is delivered
        if (orderEntity.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessLogicException("error.order.return.state", new Object[]{orderEntity.getStatus()});
        }

        /// 3=> Process Return Logic
        /// here if online => talk to paymob
        paymentService.refundPaymentForOrder(orderEntity);

        orderEntity.setStatus(OrderStatus.REFUNDED);

        for (OrderItemEntity orderItem : orderEntity.getOrderItems()) {
            ProductEntity product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();
            product.setStockCount(product.getStockCount() + quantity);
        }
    }

    /// Soft Delete
    @Transactional
    public void softDeleteOrder(Long userId, Long orderId) {
        /// 1=> get user order and ensure it belongs to him
        OrderEntity orderEntity = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        /// 2=> Business Logic: Can't delete order in progress
        if (orderEntity.getStatus() == OrderStatus.PENDING || orderEntity.getStatus() == OrderStatus.SHIPPED) {
            throw new BusinessLogicException("error.order.state", new Object[]{orderEntity.getStatus()});
        }

        /// 3=> apply soft delete (Update Flag)
        orderRepo.delete(orderEntity);

    }

    // ==================================================================================
    //                                4. SYSTEM JOBS & HELPERS
    // ==================================================================================

    /// / Cancel an Order Core Logic
    public void processOrderCancellation(OrderEntity orderEntity) {
        /// 1=> if payment done already (online)
        paymentService.refundPaymentForOrder(orderEntity);

        /// 2=> update status of order
        orderEntity.setStatus(OrderStatus.CANCELLED);

        /// 3=> return products to stock
        for (OrderItemEntity orderItem : orderEntity.getOrderItems()) {
            ProductEntity product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();
            product.setStockCount(product.getStockCount() + quantity);
        }
    }

    @Scheduled(fixedRateString = "${app.order.cleanup.rate}")
    /// 12 hour
    @SchedulerLock(name = "cancelAbandonedOrdersLock",
            lockAtMostFor = "15m", lockAtLeastFor = "5m")
    @Transactional
    public void cancelPendingOrdersAutomatically() {
        log.info("CronJob Started: Checking for abandoned orders...");
        /// 1=> select time => any pending order in last hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(hoursLimit);
        /// 2=> get Pending orders
        List<OrderEntity> pendingOrders = orderRepo.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, oneHourAgo
        );

        if (pendingOrders.isEmpty()) {
            log.info("No pending orders found at this time.");
            return;
        }

        for (OrderEntity order : pendingOrders) {
            try {
                processOrderCancellation(order);
                log.info("Automatically cancelled abandoned order ID: {} and returned items to stock.", order.getId());
            } catch (Exception e) {
                log.error("Failed to cancel order ID: {} during scheduled cleanup. Error: {}", order.getId(), e.getMessage());
            }
        }

        log.info("CronJob Finished: Successfully cleaned up {} orders.", pendingOrders.size());
    }

}