package com.global.order_api.feature.order;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.feature.payment.PaymentRepo;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private OrderService orderService; // For processOrderCancellation dependency

    @InjectMocks
    private AdminOrderService adminOrderService;


    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////READING METHODS////////////////////////////////////

    @Nested
    @DisplayName("1. Get Orders Tests (GET)")
    class GetOrdersTests {

        /// // Get All Orders (Admin) - RETURN PAGE RESPONSE
        @Test
        void getAllOrders_ShouldReturnPagedOrders() {
            /// 1. create fake filter and page
            OrderFilterRequest filter = new OrderFilterRequest();

            OrderEntity fakeEntity = new OrderEntity();
            fakeEntity.setId(1L);
            /// to be returned from repo and go into mapper
            Page<OrderEntity> mockEntityPage = new PageImpl<>(List.of(fakeEntity));

            OrderResponseDto fakeDto = new OrderResponseDto();
            fakeDto.setId(1L);

            when(orderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockEntityPage);
            when(orderMapper.mapToDtoList(mockEntityPage.getContent())).thenReturn(List.of(fakeDto));

            // 2. Act
            PageResponse<OrderResponseDto> result = adminOrderService.getAllOrders(filter);

            // 3. Assert
            assertNotNull(result);
            assertFalse(result.getData().isEmpty());
            assertEquals(1L, result.getData().get(0).getId());

            verify(orderRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(orderMapper, times(1)).mapToDtoList(anyList());
        }

        /// // Get All Orders (Admin) - No Orders in DB - RETURN EMPTY PAGE
        @Test
        void getAllOrders_WhenNoOrdersExist_ShouldReturnEmptyPage() {
            // 1. Arrange
            OrderFilterRequest filter = new OrderFilterRequest();

            // Empty Page
            Page<OrderEntity> emptyMockPage = new PageImpl<>(List.of());

            when(orderRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyMockPage);
            when(orderMapper.mapToDtoList(emptyMockPage.getContent())).thenReturn(List.of());

            // 2. Act
            PageResponse<OrderResponseDto> result = adminOrderService.getAllOrders(filter);

            // 3. Assert
            assertNotNull(result);
            assertTrue(result.getData().isEmpty());
            assertEquals(0, result.getTotalElements());

            verify(orderRepo, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(orderMapper, times(1)).mapToDtoList(emptyMockPage.getContent());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// /////////////////////////////////DELETE METHODS////////////////////////////////////

    @Nested
    @DisplayName("2. Delete Order Tests (DELETE)")
    class DeleteOrderTests {

        /// // Hard Delete - Cancelled Status - Should Hard Delete
        @Test
        void hardDeleteOrder_WhenStatusIsCancelled_ShouldHardDeleteOrderAndItems() {
            Long orderId = 10L;

            when(orderRepo.findOrderStatusByIdIncludingDeleted(orderId)).thenReturn(Optional.of("CANCELLED"));
            doNothing().when(orderRepo).hardDeleteOrderItems(orderId);
            doNothing().when(orderRepo).hardDelete(orderId);

            adminOrderService.hardDeleteOrder(orderId);

            verify(orderRepo, times(1)).hardDeleteOrderItems(orderId);
            verify(orderRepo, times(1)).hardDelete(orderId);
        }

        /// // Hard Delete - Not Cancelled Status - Should Throw Exception
        @Test
        void hardDeleteOrder_WhenStatusIsNotCancelled_ShouldThrowException() {
            Long orderId = 10L;

            when(orderRepo.findOrderStatusByIdIncludingDeleted(orderId)).thenReturn(Optional.of("DELIVERED"));

            assertThrows(BusinessLogicException.class, () -> adminOrderService.hardDeleteOrder(orderId));

            verify(orderRepo, never()).hardDeleteOrderItems(any());
            verify(orderRepo, never()).hardDelete(any());
        }
    }
}