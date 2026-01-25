package com.mypkga.commerceplatformfull.service;

import com.mypkga.commerceplatformfull.entity.Cart;
import com.mypkga.commerceplatformfull.entity.CartItem;
import com.mypkga.commerceplatformfull.entity.Product;
import com.mypkga.commerceplatformfull.entity.User;
import com.mypkga.commerceplatformfull.exception.CartUpdateException;
import com.mypkga.commerceplatformfull.exception.OutOfStockException;
import com.mypkga.commerceplatformfull.repository.CartItemRepository;
import com.mypkga.commerceplatformfull.repository.CartRepository;
import com.mypkga.commerceplatformfull.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public Cart getCartByUser(User user) {
        Optional<Cart> cart = cartRepository.findByUser(user);
        if (cart.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        }
        return cart.get();
    }

    @Override
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
    }

    @Override
    @Transactional
    public CartItem addToCart(User user, Long productId, Integer quantity) {
        Cart cart = getCartByUser(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Validate quantity
        if (quantity <= 0) {
            throw new CartUpdateException("Quantity must be greater than 0");
        }

        // Check if product already in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        int totalQuantity = quantity;
        if (existingItem.isPresent()) {
            totalQuantity = existingItem.get().getQuantity() + quantity;
        }

        // Check stock availability
        if (product.getStockQuantity() < totalQuantity) {
            log.warn("Out of stock - Product: {}, Available: {}, Requested: {}",
                product.getName(), product.getStockQuantity(), totalQuantity);
            throw new OutOfStockException(
                String.format("Không đủ hàng trong kho. Chỉ còn %d sản phẩm", product.getStockQuantity()),
                product.getStockQuantity(),
                totalQuantity
            );
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(totalQuantity);
            log.info("Updated cart item quantity - Product: {}, Quantity: {}", product.getName(), totalQuantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPrice(product.getPrice());
            log.info("Added new item to cart - Product: {}, Quantity: {}", product.getName(), quantity);
            return cartItemRepository.save(newItem);
        }
    }

    @Override
    @Transactional
    public CartItem updateCartItemQuantity(Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found: " + cartItemId));

        // If quantity is 0 or negative, remove item
        if (quantity <= 0) {
            log.info("Removing cart item - Product: {}", item.getProduct().getName());
            cartItemRepository.delete(item);
            return null;
        }

        Product product = item.getProduct();

        // Check stock availability
        if (product.getStockQuantity() < quantity) {
            log.warn("Out of stock during update - Product: {}, Available: {}, Requested: {}",
                product.getName(), product.getStockQuantity(), quantity);
            throw new OutOfStockException(
                String.format("Không đủ hàng trong kho. Chỉ còn %d sản phẩm", product.getStockQuantity()),
                product.getStockQuantity(),
                quantity
            );
        }

        item.setQuantity(quantity);
        log.info("Updated cart item quantity - Product: {}, New quantity: {}", product.getName(), quantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Override
    public BigDecimal getCartTotal(Long userId) {
        Cart cart = getCartByUserId(userId);
        return cart.getTotalAmount();
    }
}
