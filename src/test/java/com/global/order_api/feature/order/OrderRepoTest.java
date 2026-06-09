package com.global.order_api.feature.order;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.global.order_api.BaseRepoTest;
import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

class OrderRepoTest  extends BaseRepoTest {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private TestEntityManager entityManager;

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// READING METHODS ////////////////////////////////////

    @Nested
    @DisplayName("1. Get Order Tests (GET)")
    class GetOrderTests {

        /// test our logic , mapping , relationships , softDelete
        /// prevent IDOR hack
        ///// Find By ORDER Id and User Id - Exists (Return Order)
        @Test
        void findByIdAndUserId_WhenOrderExistsAndBelongsToUser_ShouldReturnOrder() {
            // 1=> Create fake User and Order
            UserEntity user = createAndSaveUser();
            /// create order and link user to his orders and save order
            /// pass the user and status and is soft deleted or not
            OrderEntity order = createAndSaveOrder(user,  false);

            // 2=> Clear Cache
            entityManager.clear();

            // 3=> Test function
            Optional<OrderEntity> result = orderRepo.findByIdAndUserId(order.getId(), user.getId());

            // 4=> Assert
            assertThat(result).isPresent();
            //// check order status  after order created
            assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PENDING);
            /// check this order related to this user only
            assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        }

        ///// Find By Id and User Id - Belongs to different user (Return Empty)
        @Test
        void findByIdAndUserId_WhenOrderBelongsToDifferentUser_ShouldReturnEmpty() {
            UserEntity owner = createAndSaveUser();
            UserEntity hacker = createAndSaveUser(); // Another user trying to access

            OrderEntity order = createAndSaveOrder(owner,  false);

            entityManager.clear();

            // hacker tries to get owner's order
            Optional<OrderEntity> result = orderRepo.findByIdAndUserId(order.getId(), hacker.getId());

            assertThat(result).isEmpty();
        }

        ///// Find By Id and User Id - Order Id Not Found (Return Empty)
        /// user doesn't make order with this id
        /// and service will throw an exception
        @Test
        void findByIdAndUserId_WhenIdDoesNotExist_ShouldReturnEmpty() {
            UserEntity user = createAndSaveUser();
            entityManager.clear();

            Optional<OrderEntity> result = orderRepo.findByIdAndUserId(999L, user.getId());

            assertThat(result).isEmpty();
        }

        ///// Find Order Status By Id Including Deleted - Active Order
        @Test
        void findOrderStatusByIdIncludingDeleted_WhenOrderIsActive_ShouldReturnStatus() {
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user,  false);

            order.setStatus(OrderStatus.SHIPPED);
            entityManager.persistAndFlush(order);
            entityManager.clear();

            Optional<String> result = orderRepo.findOrderStatusByIdIncludingDeleted(order.getId());

            assertThat(result).isPresent();
            /// .name() => convert enum to String
            assertThat(result.get()).isEqualTo(OrderStatus.SHIPPED.name());
        }

        ///// Find Order Status By Id Including Deleted - Soft Deleted Order
        @Test
        void findOrderStatusByIdIncludingDeleted_WhenOrderIsSoftDeleted_ShouldReturnStatus() {
            UserEntity user = createAndSaveUser();
            // Create a SOFT DELETED order =true
            OrderEntity order = createAndSaveOrder(user, true);
            order.setStatus(OrderStatus.DELIVERED);
            entityManager.persistAndFlush(order);
            entityManager.clear();

            Optional<String> result = orderRepo.findOrderStatusByIdIncludingDeleted(order.getId());

            // Even though it's deleted, native query should find it
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(OrderStatus.DELIVERED.name());
        }

        ///// Find Order Status By Id Including Deleted - Id Not Found
        @Test
        void findOrderStatusByIdIncludingDeleted_WhenIdDoesNotExist_ShouldReturnEmpty() {
            entityManager.clear();
            Optional<String> result = orderRepo.findOrderStatusByIdIncludingDeleted(999L);
            assertThat(result).isEmpty();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// WRITING & UPDATE METHODS /////////////////////////

    @Nested
    @DisplayName("2. Update & Restore Order Tests (PUT)")
    class UpdateOrderTests {

        ///// Restore Order - Should set is_deleted to false
        @Test
        void restoreOrder_ShouldSetIsDeletedFalse() {
            UserEntity user = createAndSaveUser();
            // Create soft-deleted order
            OrderEntity order = createAndSaveOrder(user,  true);
            order.setStatus(OrderStatus.CANCELLED);

            entityManager.clear();

            // Act
            orderRepo.restoreOrder(order.getId());

            // Clear cache to force hibernate to fetch fresh data from DB after @Modifying
            entityManager.clear();

            // Verify using Native Query to bypass any Hibernate filters just to be sure
            Object isDeleted = entityManager.getEntityManager()
                    .createNativeQuery("SELECT is_deleted FROM orders WHERE id = :id")
                    .setParameter("id", order.getId())
                    .getSingleResult();

            assertThat(((Boolean) isDeleted)).isFalse();
        }

        ///// Restore Order - When Id Does Not Exist (Should Not Throw)
        /// service will throw
        @Test
        void restoreOrder_WhenIdDoesNotExist_ShouldNotThrowException() {
            assertDoesNotThrow(() -> orderRepo.restoreOrder(999L));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// DELETE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("3. Delete Order Tests (DELETE)")
    class DeleteOrderTests {

        ///// Hard Delete Order Items
        @Test
        void hardDeleteOrderItems_ShouldDeleteItemsLinkedToOrder() {
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user,  false);
            CategoryEntity category = new CategoryEntity();
            category.setName("Test Category");
            entityManager.persistAndFlush(category);

            ProductEntity product = new ProductEntity();
            product.setName("Test Product");
            product.setPrice(BigDecimal.valueOf(100));
            product.setCategory(category);
            entityManager.persistAndFlush(product);
            OrderItemEntity orderItem=new OrderItemEntity();
            orderItem.setOrder(order);
            orderItem.setPrice(BigDecimal.valueOf(10));
            orderItem.setProduct(product);
            orderItem.setQuantity(5);
            entityManager.persistAndFlush(orderItem);


            entityManager.clear();

            // Act
            orderRepo.hardDeleteOrderItems(order.getId());
            entityManager.clear();

            // Verify item is deleted
            //// using COUNT insteadof findById => because we remove all order items
            //// and here we don't have order item id
            ////
            Number count = (Number) entityManager.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM order_item WHERE order_id = :orderId")
                    .setParameter("orderId", order.getId())
                    .getSingleResult();

            assertThat(count.intValue()).isEqualTo(0);
        }

        ///// Hard Delete Order
        @Test
        void hardDelete_ShouldDeleteOrderCompletely() {
            UserEntity user = createAndSaveUser();
            OrderEntity order = createAndSaveOrder(user,  false);

            entityManager.clear();

            // Act
            orderRepo.hardDelete(order.getId());
            entityManager.clear();

            // Verify order is completely removed from DB باستخدام findById
            Optional<OrderEntity> deletedOrder = orderRepo.findById(order.getId());

            assertThat(deletedOrder).isEmpty();
        }

        ///// Hard Delete Order - When Id Does Not Exist (Should Not Throw)
        @Test
        void hardDelete_WhenIdDoesNotExist_ShouldNotThrowException() {
            assertDoesNotThrow(() -> orderRepo.hardDelete(999L));
        }
    }

    ///////////////////////////////////////////////////////////////////
    /////////////////////// HELPER METHODS ////////////////////////////
    ///////////////////////////////////////////////////////////////////

    private UserEntity createAndSaveUser() {
        UserEntity user = new UserEntity();
        user.setName("Test User");
        /// for testing hacker or another user because email is unique
        String uniqueEmail = "testUser_" + UUID.randomUUID().toString() + "@gmail.com";
        user.setEmail(uniqueEmail);
        user.setPassword("testPassword");
        user.setRole(UserRole.USER);
        return entityManager.persistAndFlush(user);
    }

    private OrderEntity createAndSaveOrder(UserEntity user, boolean isDeleted) {
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setDeleted(isDeleted);
         order.setTotalPrice(BigDecimal.valueOf(100.0));
        return entityManager.persistAndFlush(order);
    }
}