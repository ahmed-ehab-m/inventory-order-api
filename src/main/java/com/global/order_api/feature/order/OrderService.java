package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.cart.CartEntity;
import com.global.order_api.feature.cart.CartItemEntity;
import com.global.order_api.feature.cart.CartRepo;
import com.global.order_api.feature.cart.CartService;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.product.ProductRepo;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRepo;
import com.global.order_api.feature.user.UserSpecification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService extends BaseService<OrderEntity,Long> {

    private final OrderMapper orderMapper;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final CartRepo cartRepo;

    public OrderService(BaseRepo<OrderEntity, Long> baseRepo, OrderMapper orderMapper, OrderRepo orderRepo, UserRepo userRepo, ProductRepo productRepo, CartService cartService, CartRepo cartRepo) {
        super(baseRepo);
        this.orderMapper = orderMapper;
        this.orderRepo = orderRepo;
        this.userRepo = userRepo;
        this.cartRepo = cartRepo;
    }
    ////////////////////CACHING//////////////////////
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
    ////////////////////////////////////////////////


    ////////////////////////////////////////////////
    /// READING METHODS
    /// Get order By Id to get order details for Admin
    public OrderResponseDto getOrderById(Long orderId) {
        OrderEntity order = orderRepo.findByIdOrThrow(orderId);
        return orderMapper.mapToDto(order);
    }
    /// Get order By Id to get order details for user
    public OrderResponseDto getUserOrderById(Long userId, Long orderId) {
        OrderEntity order = orderRepo.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderMapper.mapToDto(order);
    }
    /// Get All Orders
    /// smart method for pagination
    //// GET ALL ORDERS FOR ADMIN
    public PageResponse<OrderResponseDto> getAllOrders(OrderFilterRequest filter)
    {
        ///// 1=> take user input => "ASC" OR "DESC" from headers
        Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());
        ////// 2=> Pageable => take all user input page , size , sort
        ///// will be translated to SQL
        Pageable pageable= PageRequest.of(filter.getPage(),filter.getSize(),sort);
        ///// holds data + meta data about it
        Specification<OrderEntity> finalSpec= OrderSpecification.buildFilter(filter);

        //// 3=> call Repo
        Page<OrderEntity> orderEntityPage=orderRepo.findAll(finalSpec,pageable);
        ///// map Entities to Dtos
        List<OrderResponseDto> orderResponseDtoList=orderMapper.mapToDtoList(orderEntityPage.getContent());
        return PageResponse.from(orderEntityPage,orderResponseDtoList);
    }

    //// GET ONLY USER ORDERS
    /// add user id to separate general orders and user orders
    public PageResponse<OrderResponseDto> getUserOrders(Long userId, OrderFilterRequest filter)
    {
        Sort sort=Sort.by(Sort.Direction.fromString(filter.getSortDirection()),filter.getSortBy());

        Pageable pageable= PageRequest.of(filter.getPage(),filter.getSize(),sort);
        //// user get only his orders
        Specification<OrderEntity> userSpec= (root,query,cb)->
                cb.equal(root.get("user").get("id"),userId);

        Specification<OrderEntity> orderSpec= OrderSpecification.buildFilter(filter);
        Specification<OrderEntity> combinedSpec=orderSpec.and(userSpec);
        Page<OrderEntity> orderEntityPage=orderRepo.findAll(combinedSpec,pageable);
        List<OrderResponseDto> orderResponseDtoList=orderMapper.mapToDtoList(orderEntityPage.getContent());
        return PageResponse.from(orderEntityPage,orderResponseDtoList);
    }

    ////////////////////////////////////////////////
    /// WRITING METHODS
    //// Create an Order
    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequest)
    {
        /// 1=> get user cart
        CartEntity cartEntity=cartRepo.findByUserId(userId)
                .orElseThrow(
                        ()-> new ResourceNotFoundException("Cart","id",userId)
                );
        /// 2=> map cart Item Entity to OrderItemEntity
        List<CartItemEntity> cartItemsEntities=cartEntity.getItems();

        /// 3=> check if cart is empty
        if(cartItemsEntities ==null || cartItemsEntities.isEmpty())
        {
            throw new BusinessLogicException("error.cart.empty");
        }
        /// 4 => map order entity now include => shipping address + notes
        OrderEntity orderEntity = orderMapper.mapToEntity(orderRequest);

        /// 5=> link order to his user because we ignore it in mapper
        UserEntity user=userRepo.findByIdOrThrow(userId);
        orderEntity.setUser(user);

        BigDecimal totalPrice = BigDecimal.ZERO;
        for(CartItemEntity cartEntityItem : cartItemsEntities)
        {
            /// 6 => get product
            ProductEntity product=cartEntityItem.getProduct();
            /// 7=> get required quantity
            int quantityNeeded= cartEntityItem.getQuantity();

            /// 8=> check & decrease stockCount
            if(product.getStockCount() <quantityNeeded)
            {
                throw new BusinessLogicException(
                    "error.insufficient.stock",
                    new Object[]{quantityNeeded, product.getStockCount()}
            );
            }
            product.setStockCount(product.getStockCount() - quantityNeeded);
            /// 9=> fill cart item data
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
        /// 13=> set status
        orderEntity.setStatus(OrderStatus.PENDING);
        /// 14=> save order in db
        OrderEntity savedOrder=orderRepo.save(orderEntity);
        /// 15=> clear cart
        cartRepo.delete(cartEntity);
        /// 16=> map to response dto
        return orderMapper.mapToDto(savedOrder);
    }

    //// Cancel an Order
    @Transactional
    public void cancelOrder(Long userId , Long orderId)
    {
        /// 1=> get user order
        OrderEntity orderEntity=orderRepo.findByIdAndUserId(orderId,userId)
                .orElseThrow(
                        ()->  new ResourceNotFoundException("Order","id",orderId)
                );
        /// 2=> make sure the order not already canceled
        if(orderEntity.getStatus() != OrderStatus.PENDING)
        {
            throw new BusinessLogicException("error.order.state"+ orderEntity.getStatus());
        }
        /// 3=> update status of order
        orderEntity.setStatus(OrderStatus.CANCELLED);

        /// 3=> return products to stock
        for(OrderItemEntity orderItem : orderEntity.getOrderItems())
        {
            /// get product of order item
            ProductEntity product=orderItem.getProduct();
            /// get  order item quantity
            int quantity= orderItem.getQuantity();
            /// get product stock count after order created
            int productOldStockCount= product.getStockCount();
            /// update product stock count
            product.setStockCount(productOldStockCount+quantity);
//            /// save product
//            productRepo.save(product);  @Transactional will do this

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
            throw new BusinessLogicException("error.order.state"+ orderEntity.getStatus());
        }

        /// 3=> apply soft delete (Update Flag)
        orderRepo.delete(orderEntity);

    }

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
