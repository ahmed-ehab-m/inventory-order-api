package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseRepo;
import com.global.order_api.core.base.BaseService;
import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.product.ProductRepo;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService extends BaseService<CartEntity,Long> {

    private final CartRepo cartRepo;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;

    public CartService(
                       CartRepo cartRepo, CartMapper cartMapper, CartItemMapper cartItemMapper, UserRepo userRepo, ProductRepo productRepo) {
        super(cartRepo);
        this.cartRepo=cartRepo;
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
    }

    ////////////////////////////////////////////////
    /// READING METHODS
    /// Get User Cart
    public CartResponseDto getUserCart(Long userId)
    {

        Optional<CartEntity> optionalCart= cartRepo.findByUserId(userId);
        /// if user doesn't  have cart then create new cart don't throw exception
        if (optionalCart.isEmpty()) {
            CartResponseDto emptyCart = new CartResponseDto();
            emptyCart.setCartItems(new ArrayList<>());
            emptyCart.setTotalCartPrice(0.0);
            return emptyCart;
        }
        CartEntity cartEntity = optionalCart.get();
        return cartMapper.mapToDto(cartEntity);
    }

    /// WRITING METHODS
    /// add cart item
    @Transactional
    public CartResponseDto addCartItem(Long userId ,CartItemRequestDto cartItemRequestDto) {
        /// 1=> Get current user cart OR
        CartEntity cart=cartRepo.findByUserId(userId)
                /// create cart to add items
                .orElseGet(()->{
                    CartEntity newCart = new CartEntity();
                    /// link user to Cart
                    UserEntity user=userRepo.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    newCart.setUser(user);
                    return newCart;
                });
        /// 2=> check if product already existed in DB
        /// to pass it in new cart
        ProductEntity product=productRepo.findByIdOrThrow(cartItemRequestDto.getProductId());

        /// 3=> check if product already existed in cart
        /// using stream instead of call DB again ,because we already get the Cart already
        /// Stream => take list of data
        /// return Optional , clean code ,easily filter data than for-loop
        Optional<CartItemEntity> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();
        /// ++ quantity of product in cart
        if (existingItem.isPresent()) {

            CartItemEntity item=existingItem.get();
            Integer newQuantity=item.getQuantity()+ cartItemRequestDto.getQuantity();
            item.setQuantity(newQuantity);
            if (newQuantity > product.getStockCount()) {
                throw new BusinessLogicException(
                        "error.insufficient.stock",
                        new Object[]{newQuantity, product.getStockCount()}
                );
            }
        }
        else
        {
            /// check stock count
            if (cartItemRequestDto.getQuantity() > product.getStockCount()) {
                throw new BusinessLogicException(
                        "error.insufficient.stock",
                        new Object[]{cartItemRequestDto.getQuantity(), product.getStockCount()}
                );
            }
            CartItemEntity newItem=cartItemMapper.mapToEntity(cartItemRequestDto);
            /// add the product
            newItem.setProduct(product);
            /// link the new item with cart to add cart_id
            cart.addCartItem(newItem);
        }
        /// 4=> save the cart in db
        CartEntity savedCart=cartRepo.save(cart);
        return cartMapper.mapToDto(savedCart);
    }

    /// remove cartItem
    @Transactional
    public void removeCartItem(Long userId ,Long cartItemId) {
        /// 1=> Get current user cart OR
        CartEntity cart=cartRepo.findByUserId(userId)
                .orElseThrow(()->new ResourceNotFoundException("Cart", "user id", userId));
        /// 2=> remove cart item from cart items list
        boolean isRemoved=cart.getItems().removeIf(item->item.getId().equals(cartItemId));
        if(!isRemoved)
        {
          throw new ResourceNotFoundException("CartItem", "id", cartItemId);
        }

    }

    /// update quantity
    /// pass to direct value to update item quantity
    @Transactional
    public  CartResponseDto updateItemQuantity(Long userId,long cartItemId,Integer newQuantity)
    {
        /// 1=> Get current user cart OR throw exception
        CartEntity cart=cartRepo.findByUserId(userId)
                .orElseThrow(()->new ResourceNotFoundException("Cart", "user id", userId));
        /// 2=> if quantity == 0
        if(newQuantity<=0)
        {
            removeCartItem(userId,cartItemId);
            return cartMapper.mapToDto(cart); /// return the cart after remove items
        }

        /// 3=> check product stock count
            /// get the product from list
        CartItemEntity itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        ProductEntity product = itemToUpdate.getProduct();

        if (newQuantity > product.getStockCount()) {
            throw new BusinessLogicException(
                    "error.insufficient.stock",
                    new Object[]{newQuantity, product.getStockCount()}
            );
        }

        /// 4=> pass the newQuantity
        itemToUpdate.setQuantity(newQuantity);

        /// 5 => return the cart
        return cartMapper.mapToDto(cart);
    }

    /// clear cart after creating order
    @Transactional
    public void clearCart(long userId)
    {
        CartEntity cart=cartRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "user id", userId));
        cart.getItems().clear();
        cartRepo.save(cart);
    }
}
