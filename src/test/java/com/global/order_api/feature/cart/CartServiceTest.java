package com.global.order_api.feature.cart;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.cart.dto.CartItemRequestDto;
import com.global.order_api.feature.cart.dto.CartResponseDto;
import com.global.order_api.feature.cart.dto.RawCartDto;
import com.global.order_api.feature.cart.dto.RawCartItemDto;
import com.global.order_api.feature.cart.entity.CartEntity;
import com.global.order_api.feature.cart.entity.CartItemEntity;
import com.global.order_api.feature.cart.mapper.CartItemMapper;
import com.global.order_api.feature.cart.mapper.CartMapper;
import com.global.order_api.feature.cart.repo.CartRepo;
import com.global.order_api.feature.cart.service.CartService;
import com.global.order_api.feature.product.entity.ProductEntity;
import com.global.order_api.feature.product.repo.ProductRepo;
import com.global.order_api.feature.product.service.ProductService;
import com.global.order_api.feature.product.dto.UserProductResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepo cartRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;


    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// READING METHODS ////////////////////////////////////

    @Nested
    @DisplayName("1. Get User Cart Tests (GET)")
    class GetUserCartTests {

        /// // Find by RAW Car ID Exists - RETURN Cart
        @Test
        void getRawCart_WhenCartExists_ShouldReturnRawCartDto() {
            // 1. Setup Data
            Long userId = 1L;
            CartEntity mockCart = new CartEntity();
            mockCart.setId(10L);

            RawCartDto expectedDto = new RawCartDto();
            expectedDto.setCartId(10L);
            expectedDto.setUserId(userId);

            // 2. Mocking
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(mockCart));
            when(cartMapper.mapToRawDto(mockCart)).thenReturn(expectedDto);

            // 3. Act
            RawCartDto result = cartService.getRawCart(userId);

            // 4. Assert
            assertNotNull(result);
            assertEquals(10L, result.getCartId());

            verify(cartRepo, times(1)).findByUserId(userId);
            verify(cartMapper, times(1)).mapToRawDto(mockCart);
        }

        @Test
        void getRawCart_WhenCartDoesNotExist_ShouldReturnNull() {
            // 1. Setup Data
            Long userId = 1L;

            // 2. Mocking
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());

            // 3. Act
            RawCartDto result = cartService.getRawCart(userId);

            // 4. Assert
            assertNull(result);

            verify(cartRepo, times(1)).findByUserId(userId);
            verify(cartMapper, never()).mapToRawDto(any());
        }

        /// // Find by ID Exists - RETURN Hydrated Cart
        @Test
        void getUserCart_WhenCartExists_ShouldReturnHydratedCartDto() {
            // 1. Arrange
            Long userId = 1L;
            Long productId = 100L;

            /// 1. Mock Database Entity
            CartEntity cartEntity = new CartEntity();
            cartEntity.setId(10L);

            /// 2. Mock Raw Item & Cart
            RawCartItemDto rawItem = new RawCartItemDto();
            rawItem.setProductId(productId);
            rawItem.setQuantity(2);
            RawCartDto rawCartDto = new RawCartDto();
            rawCartDto.setCartId(10L);
            rawCartDto.setUserId(userId);
            rawCartDto.setItems(List.of(rawItem));

            /// 3. Mock Product Service Response
            UserProductResponseDto mockProduct = new UserProductResponseDto();
            mockProduct.setId(productId);
            mockProduct.setName("Laptop");
            mockProduct.setPrice(new BigDecimal("250.0"));

            // Mocking the behavior
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cartEntity));
            when(cartMapper.mapToRawDto(cartEntity)).thenReturn(rawCartDto);
            when(productService.getProductByIdWithCategory(productId)).thenReturn(mockProduct);

            // 2. Act
            CartResponseDto result = cartService.getUserCart(userId);

            // 3. Assert
            assertNotNull(result);
            assertEquals(1, result.getCartItems().size());
            assertEquals(500.0, result.getTotalCartPrice()); // 250 * 2 = 500
            assertEquals("Laptop", result.getCartItems().get(0).getProductName());

            verify(cartRepo, times(1)).findByUserId(userId);
            verify(cartMapper, times(1)).mapToRawDto(cartEntity);
            verify(productService, times(1)).getProductByIdWithCategory(productId);
        }

        /// // Find by ID Doesn't exist (Or Empty Items) - RETURN empty cart
        @Test
        void getUserCart_WhenCartDoesNotExist_ShouldReturnEmptyCartDto() {
            // 1. Arrange
            Long userId = 1L;

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());

            // 2. Act
            CartResponseDto result = cartService.getUserCart(userId);

            // 3. Assert
            assertNotNull(result);
            assertTrue(result.getCartItems().isEmpty());
            assertEquals(0.0, result.getTotalCartPrice());

            verify(cartRepo, times(1)).findByUserId(userId);
            verify(cartMapper, never()).mapToRawDto(any());
            verify(productService, never()).getProductByIdWithCategory(any());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// WRITING METHODS ////////////////////////////////////

    @Nested
    @DisplayName("2. Add Cart Item Tests (POST)")
    class AddCartItemTests {

        @Test
        void addCartItem_WhenCartExistsAndItemIsNew_ShouldAddNewItemToCart() {
            // 1. Arrange
            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(2);

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>()); // Empty items list

            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setStockCount(10); // Sufficient stock

            CartItemEntity newItem = new CartItemEntity();

            CartResponseDto responseDto = new CartResponseDto();

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productRepo.findByIdOrThrow(100L)).thenReturn(product);
            when(cartItemMapper.mapToEntity(requestDto)).thenReturn(newItem);
            when(cartRepo.save(any(CartEntity.class))).thenReturn(cart);
            when(cartMapper.mapToDto(cart)).thenReturn(responseDto);

            // 2. Act
            CartResponseDto result = cartService.addCartItem(userId, requestDto);

            // 3. Assert
            assertNotNull(result);
            assertTrue(cart.getItems().contains(newItem));
            assertEquals(product, newItem.getProduct());
            verify(cartRepo, times(1)).save(cart);
        }

        @Test
        void addCartItem_WhenCartExistsAndItemExists_ShouldIncreaseQuantity() {
            // 1. Arrange
            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(2); // Wants to add 2 more

            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setStockCount(10);

            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setProduct(product);
            existingItem.setQuantity(3); // Already has 3 in cart

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem); // Item exists in cart

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productRepo.findByIdOrThrow(100L)).thenReturn(product);
            when(cartRepo.save(cart)).thenReturn(cart);
            when(cartMapper.mapToDto(cart)).thenReturn(new CartResponseDto());

            // 2. Act
            cartService.addCartItem(userId, requestDto);

            // 3. Assert
            assertEquals(5, existingItem.getQuantity()); // 3 (old) + 2 (new) = 5
            verify(cartItemMapper, never()).mapToEntity(any()); // Because item existed
        }

        @Test
        void addCartItem_WhenCartDoesNotExist_ShouldCreateNewCart() {
            // 1. Arrange
            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(1);

            UserEntity user = new UserEntity();
            user.setId(userId);

            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setStockCount(10);

            // Simulate cart not existing
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(productRepo.findByIdOrThrow(100L)).thenReturn(product);
            when(cartItemMapper.mapToEntity(requestDto)).thenReturn(new CartItemEntity());
            when(cartRepo.save(any(CartEntity.class))).thenReturn(new CartEntity());
            when(cartMapper.mapToDto(any())).thenReturn(new CartResponseDto());

            // 2. Act
            cartService.addCartItem(userId, requestDto);

            // 3. Assert & Verify
            verify(userRepo, times(1)).findById(userId); // Confirms user was fetched to create cart
            verify(cartRepo, times(1)).save(any(CartEntity.class));
        }

        /// // FAILURE CASES FOR ADD ITEM /////
        /// / add cart item but product id not found - Throw exception
        @Test
        void addCartItem_WhenProductNotFound_ShouldThrowException() {
            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(999L);

            CartEntity cart = new CartEntity();

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productRepo.findByIdOrThrow(999L))
                    .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.addCartItem(userId, requestDto));

            verify(cartRepo, never()).save(any());
        }

        /// / add cart item but quantity required exceeds Stock - Throw exception
        @Test
        void addCartItem_WhenExistingItemQuantityExceedsStock_ShouldThrowException() {
            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(5); // Wants to add 5

            /// 6 products in DB only
            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setStockCount(6); // Only 6 in stock total

            ///existing item already in user cart and user want to add more quantity
            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setProduct(product);
            existingItem.setQuantity(3); // Already has 3. Total will be 8 > 6

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productRepo.findByIdOrThrow(100L)).thenReturn(product);

            assertThrows(BusinessLogicException.class, () ->
                    cartService.addCartItem(userId, requestDto));

            verify(cartRepo, never()).save(any());
        }

        @Test
        void addCartItem_WhenNewItemQuantityExceedsStock_ShouldThrowException() {

            Long userId = 1L;
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(10); // Wants to add 10

            ProductEntity product = new ProductEntity();
            product.setId(100L);
            product.setStockCount(5); // Only 5 in stock

            //// no cart items before
            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>()); // Item is new

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productRepo.findByIdOrThrow(100L)).thenReturn(product);

            assertThrows(BusinessLogicException.class, () ->
                    cartService.addCartItem(userId, requestDto));

            verify(cartRepo, never()).save(any());
        }

        /// user not found
        @Test
        void addCartItem_WhenUserNotFoundWhileCreatingCart_ShouldThrowException() {
            Long userId = 999L;
            CartItemRequestDto requestDto = new CartItemRequestDto();

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(userRepo.findById(userId)).thenReturn(Optional.empty()); // User not found

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.addCartItem(userId, requestDto));

            verify(productRepo, never()).findByIdOrThrow(any());
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// REMOVE & UPDATE METHODS ////////////////////////////

    @Nested
    @DisplayName("3. Remove & Update Cart Item Tests")
    class RemoveAndUpdateTests {

        /// // REMOVE SUCCESS /////
        /// Remove Cart Item - Removed
        @Test
        void removeCartItem_WhenItemExists_ShouldRemoveItem() {
            Long userId = 1L;
            Long cartItemId = 50L;

            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setId(cartItemId);

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));

            // Act
            cartService.removeCartItem(userId, cartItemId);

            // Assert
            assertTrue(cart.getItems().isEmpty());
        }

        /// // REMOVE FAILURES /////
        /// Remove cart item but cart not found - Throw Exception
        @Test
        void removeCartItem_WhenCartNotFound_ShouldThrowException() {
            Long userId = 1L;
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.removeCartItem(userId, 10L));
        }

        /// Remove cart item but cart item not found - Throw Exception
        @Test
        void removeCartItem_WhenItemNotFoundInCart_ShouldThrowException() {
            Long userId = 1L;
            Long invalidCartItemId = 99L;

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>()); // Cart is empty

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.removeCartItem(userId, invalidCartItemId));
        }

        /// // UPDATE SUCCESS /////
        /// Update product with valid quantity - Update Quantity
        @Test
        void updateItemQuantity_WhenQuantityIsValid_ShouldUpdateQuantity() {
            Long userId = 1L;
            Long cartItemId = 50L;
            Integer newQuantity = 4;

            ProductEntity product = new ProductEntity();
            product.setStockCount(10); // Sufficient stock

            //// item already in user cart will be updated
            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setId(cartItemId);
            existingItem.setQuantity(2);
            existingItem.setProduct(product);

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartMapper.mapToDto(cart)).thenReturn(new CartResponseDto());

            // Act
            cartService.updateItemQuantity(userId, cartItemId, newQuantity);

            // Assert check the right value after update
            assertEquals(4, existingItem.getQuantity());
            verify(cartMapper, times(1)).mapToDto(cart);
        }

        /// Update product with 0 quantity - Update Quantity
        @Test
        void updateItemQuantity_WhenQuantityIsZeroOrLess_ShouldRemoveItem() {
            Long userId = 1L;
            Long cartItemId = 50L;

            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setId(cartItemId);

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(cartMapper.mapToDto(cart)).thenReturn(new CartResponseDto());

            // Act (Send quantity 0)
            cartService.updateItemQuantity(userId, cartItemId, 0);

            // Assert
            /// remove the cart item
            assertTrue(cart.getItems().isEmpty()); // Confirms removeCartItem logic ran
        }

        /// // UPDATE FAILURES /////
        /// Update product with invalid quantity - Throw Exception
        @Test
        void updateItemQuantity_WhenExceedsStock_ShouldThrowException() {
            Long userId = 1L;
            Long cartItemId = 50L;
            Integer newQuantity = 20; // Stock is only 10

            ProductEntity product = new ProductEntity();
            product.setStockCount(10);

            CartItemEntity existingItem = new CartItemEntity();
            existingItem.setId(cartItemId);
            existingItem.setProduct(product);

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(existingItem);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));

            assertThrows(BusinessLogicException.class, () ->
                    cartService.updateItemQuantity(userId, cartItemId, newQuantity));
        }

        /// Update product but item not found - Update Quantity
        @Test
        void updateItemQuantity_WhenItemNotFound_ShouldThrowException() {
            Long userId = 1L;
            Long invalidCartItemId = 99L;

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>()); // Empty cart

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.updateItemQuantity(userId, invalidCartItemId, 5));
        }
    }

    /// /////////////////////////////////////////////////////////////////////////////////////
    /// ///////////////////////////////// CLEAR CART METHODS ////////////////////////////////

    @Nested
    @DisplayName("4. Clear Cart Tests")
    class ClearCartTests {

        /// Remove All cart  - Throw Exception
        @Test
        void clearCart_WhenCartExists_ShouldClearAllItems() {
            Long userId = 1L;

            CartItemEntity item1 = new CartItemEntity();
            CartItemEntity item2 = new CartItemEntity();

            CartEntity cart = new CartEntity();
            cart.setItems(new ArrayList<>());
            cart.getItems().add(item1);
            cart.getItems().add(item2);

            when(cartRepo.findByUserId(userId)).thenReturn(Optional.of(cart));

            // Act
            cartService.clearCart(userId);

            // Assert
            assertTrue(cart.getItems().isEmpty());
            verify(cartRepo, times(1)).save(cart);
        }

        /// Remove cart  but cart not found - Throw Exception
        @Test
        void clearCart_WhenCartDoesNotExist_ShouldThrowException() {
            Long userId = 1L;
            when(cartRepo.findByUserId(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    cartService.clearCart(userId));

            verify(cartRepo, never()).save(any());
        }
    }
}