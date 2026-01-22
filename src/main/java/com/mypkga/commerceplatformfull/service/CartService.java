package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.CartItem;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    Cart getCartByUser(User user);

    Cart getCartByUserId(Long userId);

    CartItem addToCart(User user, Long productId, Integer quantity);

    CartItem updateCartItemQuantity(Long cartItemId, Integer quantity);

    void removeFromCart(Long cartItemId);

    void clearCart(Long userId);

    BigDecimal getCartTotal(Long userId);
}
