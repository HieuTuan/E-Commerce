/**
 * TikTok-style Add to Cart Animation
 */

class CartAnimation {
    constructor() {
        this.init();
    }

    init() {
        // Add event listeners to all add to cart buttons
        document.addEventListener('DOMContentLoaded', () => {
            this.attachEventListeners();
        });
    }

    attachEventListeners() {
        // Find all add to cart forms
        const addToCartForms = document.querySelectorAll('form[action*="/cart/add"]');
        
        addToCartForms.forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleAddToCart(form, e);
            });
        });

        // Also handle direct button clicks
        const addToCartButtons = document.querySelectorAll('button[type="submit"]:has(i.fa-cart-plus)');
        addToCartButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const form = button.closest('form');
                if (form && form.action.includes('/cart/add')) {
                    e.preventDefault();
                    this.handleAddToCart(form, e);
                }
            });
        });
    }

    async handleAddToCart(form, event) {
        const button = form.querySelector('button[type="submit"]');
        const productCard = form.closest('.card') || form.closest('.product-card');
        const productImage = productCard?.querySelector('img');

        // Show loading state
        this.showLoadingState(button);

        try {
            // Submit form via AJAX
            const formData = new FormData(form);
            const response = await fetch(form.action, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.ok) {
                // Show success animation
                await this.showSuccessAnimation(productImage, button);
                
                // Update cart count if exists
                this.updateCartCount();
                
                // Show success message
                this.showSuccessMessage('Đã thêm vào giỏ hàng!');
                
                // Redirect if needed (for non-AJAX requests)
                const returnUrl = formData.get('returnUrl');
                if (returnUrl && returnUrl !== window.location.pathname) {
                    setTimeout(() => {
                        window.location.href = returnUrl;
                    }, 1500);
                }
            } else {
                throw new Error('Failed to add to cart');
            }
        } catch (error) {
            console.error('Error adding to cart:', error);
            this.showErrorMessage('Không thể thêm vào giỏ hàng');
        } finally {
            this.resetButtonState(button);
        }
    }

    showLoadingState(button) {
        const originalContent = button.innerHTML;
        button.dataset.originalContent = originalContent;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang thêm...';
        button.disabled = true;
    }

    resetButtonState(button) {
        setTimeout(() => {
            button.innerHTML = button.dataset.originalContent || '<i class="fas fa-cart-plus"></i> Thêm';
            button.disabled = false;
        }, 2000);
    }

    async showSuccessAnimation(productImage, button) {
        if (!productImage) return;

        // Create flying image
        const flyingImage = this.createFlyingImage(productImage);
        document.body.appendChild(flyingImage);

        // Get cart icon position (or create one if doesn't exist)
        const cartIcon = this.getOrCreateCartIcon();
        
        // Animate flying image
        await this.animateFlyingImage(flyingImage, cartIcon);
        
        // Show button success state
        this.showButtonSuccess(button);
        
        // Animate cart icon
        this.animateCartIcon(cartIcon);
        
        // Clean up
        setTimeout(() => {
            if (flyingImage.parentNode) {
                flyingImage.parentNode.removeChild(flyingImage);
            }
        }, 1000);
    }

    createFlyingImage(sourceImage) {
        const flyingImage = document.createElement('img');
        flyingImage.src = sourceImage.src;
        flyingImage.className = 'flying-cart-image';
        
        // Get source position
        const rect = sourceImage.getBoundingClientRect();
        
        // Style the flying image
        Object.assign(flyingImage.style, {
            position: 'fixed',
            top: rect.top + 'px',
            left: rect.left + 'px',
            width: rect.width + 'px',
            height: rect.height + 'px',
            zIndex: '9999',
            borderRadius: '8px',
            transition: 'all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
            pointerEvents: 'none',
            boxShadow: '0 4px 20px rgba(0,0,0,0.3)'
        });

        return flyingImage;
    }

    getOrCreateCartIcon() {
        // Try to find existing cart icon
        let cartIcon = document.querySelector('.navbar .fa-shopping-cart');
        
        if (!cartIcon) {
            // Create floating cart icon if none exists
            cartIcon = document.createElement('div');
            cartIcon.innerHTML = '<i class="fas fa-shopping-cart"></i>';
            cartIcon.className = 'floating-cart-icon';
            Object.assign(cartIcon.style, {
                position: 'fixed',
                top: '20px',
                right: '20px',
                width: '50px',
                height: '50px',
                backgroundColor: '#007bff',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '20px',
                zIndex: '1000',
                boxShadow: '0 4px 12px rgba(0,123,255,0.3)'
            });
            document.body.appendChild(cartIcon);
        }

        return cartIcon;
    }

    async animateFlyingImage(flyingImage, cartIcon) {
        return new Promise((resolve) => {
            const cartRect = cartIcon.getBoundingClientRect();
            
            // Animate to cart position
            setTimeout(() => {
                Object.assign(flyingImage.style, {
                    top: cartRect.top + 'px',
                    left: cartRect.left + 'px',
                    width: '30px',
                    height: '30px',
                    opacity: '0.8',
                    transform: 'scale(0.5) rotate(360deg)'
                });
            }, 50);

            setTimeout(resolve, 800);
        });
    }

    showButtonSuccess(button) {
        const originalContent = button.innerHTML;
        button.innerHTML = '<i class="fas fa-check"></i> Đã thêm!';
        button.style.backgroundColor = '#28a745';
        button.style.borderColor = '#28a745';
        
        setTimeout(() => {
            button.style.backgroundColor = '';
            button.style.borderColor = '';
        }, 2000);
    }

    animateCartIcon(cartIcon) {
        // Bounce animation
        cartIcon.style.transform = 'scale(1.3)';
        cartIcon.style.transition = 'transform 0.3s ease';
        
        setTimeout(() => {
            cartIcon.style.transform = 'scale(1)';
        }, 300);

        // Add pulse effect
        cartIcon.classList.add('cart-pulse');
        setTimeout(() => {
            cartIcon.classList.remove('cart-pulse');
        }, 1000);
    }

    updateCartCount() {
        // Update cart count badge if exists
        const cartBadge = document.querySelector('.cart-count, .badge');
        if (cartBadge) {
            const currentCount = parseInt(cartBadge.textContent) || 0;
            cartBadge.textContent = currentCount + 1;
            
            // Animate badge
            cartBadge.style.transform = 'scale(1.5)';
            setTimeout(() => {
                cartBadge.style.transform = 'scale(1)';
            }, 200);
        }
    }

    showSuccessMessage(message) {
        // Create toast notification
        const toast = document.createElement('div');
        toast.className = 'cart-success-toast';
        toast.innerHTML = `
            <i class="fas fa-check-circle"></i>
            <span>${message}</span>
        `;
        
        Object.assign(toast.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            backgroundColor: '#28a745',
            color: 'white',
            padding: '12px 20px',
            borderRadius: '8px',
            zIndex: '10000',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(40, 167, 69, 0.3)',
            transform: 'translateX(100%)',
            transition: 'transform 0.3s ease'
        });

        document.body.appendChild(toast);

        // Animate in
        setTimeout(() => {
            toast.style.transform = 'translateX(0)';
        }, 100);

        // Animate out and remove
        setTimeout(() => {
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3000);
    }

    showErrorMessage(message) {
        // Similar to success message but red
        const toast = document.createElement('div');
        toast.className = 'cart-error-toast';
        toast.innerHTML = `
            <i class="fas fa-exclamation-circle"></i>
            <span>${message}</span>
        `;
        
        Object.assign(toast.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            backgroundColor: '#dc3545',
            color: 'white',
            padding: '12px 20px',
            borderRadius: '8px',
            zIndex: '10000',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 4px 12px rgba(220, 53, 69, 0.3)',
            transform: 'translateX(100%)',
            transition: 'transform 0.3s ease'
        });

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.transform = 'translateX(0)';
        }, 100);

        setTimeout(() => {
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3000);
    }
}

// CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes cart-pulse {
        0% { transform: scale(1); }
        50% { transform: scale(1.2); }
        100% { transform: scale(1); }
    }
    
    .cart-pulse {
        animation: cart-pulse 0.6s ease-in-out;
    }
    
    .flying-cart-image {
        object-fit: cover;
    }
    
    .cart-success-toast,
    .cart-error-toast {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        font-size: 14px;
        font-weight: 500;
    }
`;
document.head.appendChild(style);

// Initialize animation system
new CartAnimation();