package com.global.order_api.feature.order;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.feature.cart.CartEntity;
import com.global.order_api.feature.cart.CartItemEntity;
import com.global.order_api.feature.cart.CartRepo;
import com.global.order_api.feature.payment.PaymentService;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private CartRepo cartRepo;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Orders Tests (GET)")
    class GetOrdersTests {

        /// // Get User Orders - RETURN PAGE RESPONSE
        @Test
        void getUserOrders_ShouldReturnPagedOrdersForSpecificUser() {
            Long userId = 1L;
            OrderFilterRequest filter = new OrderFilterRequest();

            OrderEntity fakeEntity = new OrderEntity();
            Page<OrderEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            when(orderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(orderMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(new OrderResponseDto()));

            PageResponse<OrderResponseDto> result = orderService.getUserOrders(userId, filter);

            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            verify(orderRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }

        /// // Get User Orders - User Does Not Exist OR Has No Orders - RETURN EMPTY PAGE
        @Test
        void getUserOrders_WhenUserHasNoOrders_ShouldReturnEmptyPage() {
            // 1. Arrange
            Long userId = 999L;
            OrderFilterRequest filter = new OrderFilterRequest();

            // Page
            Page<OrderEntity> emptyMockPage = new PageImpl<>(List.of());

            when(orderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);
            when(orderMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            // 2. Act
            PageResponse<OrderResponseDto> result = orderService.getUserOrders(userId, filter);

            // 3. Assert
            assertNotNull(result);
            assertTrue(result.getData().isEmpty());

            verify(orderRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////WRITING METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Create & Cancel Order Tests (POST / PUT)")
    class CreateAndCancelOrderTests {

        /// // Create Order - Valid Cart & Stock - Should Create Order & Clear Cart
        @Test
        void createOrder_WithValidCartAndStock_ShouldCreateOrder() {
            // 1. Arrange
            Long userId = 1L;
            OrderRequestDto requestDto = new OrderRequestDto();
            requestDto.setPaymentType(PaymentType.CASH);

            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setPrice(BigDecimal.valueOf(500.0));
            product.setStockCount(10); // Available stock

            CartItemEntity cartItem = new CartItemEntity();
            cartItem.setProduct(product);
            cartItem.setQuantity(2); // Needs 2

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>(List.of(cartItem)));

            UserEntity user = new UserEntity();
            user.setId(userId);

            OrderEntity mappedOrder = new OrderEntity();
            mappedOrder.setOrderItems(new ArrayList<>()); // initialize list to avoid NullPointer in addOrderItem

            OrderEntity savedOrder = new OrderEntity();
            savedOrder.setId(10L);

            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(10L);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(orderMapper.mapToEntity(requestDto)).thenReturn(mappedOrder);
            when(userRepo.findByIdOrThrow(userId)).thenReturn(user);
            when(orderRepo.save(any(OrderEntity.class))).thenReturn(savedOrder);
            when(orderMapper.mapToDto(savedOrder)).thenReturn(responseDto);

            // 2. Act
            OrderResponseDto result = orderService.createOrder(userId, requestDto);

            // 3. Assert
            assertNotNull(result);
            assertEquals(10L, result.getId());

            // Check Business Logic Applications
            assertEquals(8, product.getStockCount()); // Stock should be reduced by 2
            assertEquals(OrderStatus.PENDING, mappedOrder.getStatus());
            assertEquals(BigDecimal.valueOf(1000.0), mappedOrder.getTotalPrice()); // 500 * 2

            // Verify interactions
            verify(cartRepo, times(1)).delete(cart); // Cart cleared
            verify(orderRepo, times(1)).save(mappedOrder);
        }

        /// // Create Order - Empty Cart - Should Throw Exception
        @Test
        void createOrder_WhenCartIsEmpty_ShouldThrowException() {
            Long userId = 1L;
            OrderRequestDto requestDto = new OrderRequestDto();

            CartEntity emptyCart = new CartEntity();
            emptyCart.setItems(new ArrayList<>()); // Empty items

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(emptyCart));

            assertThrows(BusinessLogicException.class, () -> orderService.createOrder(userId, requestDto));

            verify(orderRepo, never()).save(any());
            verify(cartRepo, never()).delete(any());
        }

        /// // Create Order - Insufficient Stock - Should Throw Exception
        @Test
        void createOrder_WhenInsufficientStock_ShouldThrowException() {
            Long userId = 1L;
            OrderRequestDto requestDto = new OrderRequestDto();

            ProductEntity product = new ProductEntity();
            product.setStockCount(1); // Only 1 in stock

            CartItemEntity cartItem = new CartItemEntity();
            cartItem.setProduct(product);
            cartItem.setQuantity(5); // User wants 5!

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>(List.of(cartItem)));

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(orderMapper.mapToEntity(requestDto)).thenReturn(new OrderEntity());
            when(userRepo.findByIdOrThrow(userId)).thenReturn(new UserEntity());

            assertThrows(BusinessLogicException.class, () -> orderService.createOrder(userId, requestDto));

            verify(orderRepo, never()).save(any());
        }

        /// // Cancel Order - PENDING Status - Should Cancel and Restore Stock
        @Test
        void cancelOrder_WhenStatusIsPending_ShouldCancelAndRestoreStock() {
            Long userId = 1L;
            Long orderId = 10L;

            ProductEntity product = new ProductEntity();
            product.setStockCount(5); // Current stock

            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setProduct(product);
            orderItem.setQuantity(3); // Quantity to be restored

            OrderEntity order = new OrderEntity();
            order.setStatus(OrderStatus.PENDING); // Valid status for cancellation
            order.setOrderItems(List.of(orderItem));

            when(orderRepo.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            // Act
            orderService.cancelOrder(userId, orderId);

            // Assert
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            assertEquals(8, product.getStockCount()); // Stock should increase: 5 + 3
        }

        /// // Cancel Order - NOT PENDING Status - Should Throw Exception
        @Test
        void cancelOrder_WhenStatusIsNotPending_ShouldThrowException() {
            Long userId = 1L;
            Long orderId = 10L;

            OrderEntity order = new OrderEntity();
            order.setStatus(OrderStatus.SHIPPED); // Invalid status for cancellation

            when(orderRepo.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            assertThrows(BusinessLogicException.class, () -> orderService.cancelOrder(userId, orderId));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("3. Delete Order Tests (DELETE)")
    class DeleteOrderTests {

        /// // Soft Delete - Valid Status - Should Delete Order
        @Test
        void softDeleteOrder_WhenStatusIsDelivered_ShouldDeleteOrder() {
            Long userId = 1L;
            Long orderId = 10L;

            OrderEntity order = new OrderEntity();
            order.setStatus(OrderStatus.DELIVERED); // Allowed to be soft deleted

            when(orderRepo.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
            doNothing().when(orderRepo).delete(order);

            orderService.softDeleteOrder(userId, orderId);

            verify(orderRepo, times(1)).delete(order);
        }

        /// // Soft Delete - In Progress Status (PENDING/SHIPPED) - Should Throw Exception
        @Test
        void softDeleteOrder_WhenStatusIsInProgress_ShouldThrowException() {
            Long userId = 1L;
            Long orderId = 10L;

            OrderEntity order = new OrderEntity();
            order.setStatus(OrderStatus.SHIPPED); // Cannot delete while shipping!

            when(orderRepo.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            assertThrows(BusinessLogicException.class, () -> orderService.softDeleteOrder(userId, orderId));
//            verify(orderRepo, never()).delete(any());
        }
    }
}