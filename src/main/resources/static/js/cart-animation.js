// Cart Animation and Enhancement Script

document.addEventListener('DOMContentLoaded', function() {
    // Cart count animation
    function animateCartCount() {
        const cartCount = document.querySelector('.cart-count');
        if (cartCount) {
            cartCount.classList.add('pulse');
            setTimeout(() => {
                cartCount.classList.remove('pulse');
            }, 1000);
        }
    }

    // Add to cart button enhancement
    function enhanceAddToCartButtons() {
        document.querySelectorAll('.add-to-cart-form').forEach(form => {
            form.addEventListener('submit', function(e) {
                const button = this.querySelector('.add-to-cart');
                if (button) {
                    // Add loading state
                    const originalText = button.innerHTML;
                    button.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Đang thêm...';
                    button.disabled = true;
                    
                    // Create floating animation
                    const rect = button.getBoundingClientRect();
                    const floatingIcon = document.createElement('div');
                    floatingIcon.innerHTML = '<i class="fas fa-shopping-cart"></i>';
                    floatingIcon.style.cssText = `
                        position: fixed;
                        left: ${rect.left + rect.width / 2}px;
                        top: ${rect.top + rect.height / 2}px;
                        z-index: 9999;
                        color: #667eea;
                        font-size: 1.5rem;
                        pointer-events: none;
                        transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
                    `;
                    
                    document.body.appendChild(floatingIcon);
                    
                    // Animate to cart
                    const cartIcon = document.querySelector('.cart-link');
                    if (cartIcon) {
                        const cartRect = cartIcon.getBoundingClientRect();
                        setTimeout(() => {
                            floatingIcon.style.left = cartRect.left + 'px';
                            floatingIcon.style.top = cartRect.top + 'px';
                            floatingIcon.style.transform = 'scale(0.5)';
                            floatingIcon.style.opacity = '0';
                        }, 100);
                        
                        setTimeout(() => {
                            document.body.removeChild(floatingIcon);
                            animateCartCount();
                        }, 900);
                    }
                    
                    // Reset button after delay (for demo purposes)
                    setTimeout(() => {
                        button.innerHTML = originalText;
                        button.disabled = false;
                    }, 2000);
                }
            });
        });
    }

    // Product card hover effects
    function enhanceProductCards() {
        document.querySelectorAll('.product-card').forEach(card => {
            card.addEventListener('mouseenter', function() {
                this.style.transform = 'translateY(-15px) scale(1.02)';
                
                // Show product actions
                const actions = this.querySelector('.product-actions');
                if (actions) {
                    actions.style.opacity = '1';
                    actions.style.transform = 'translateX(0)';
                }
            });
            
            card.addEventListener('mouseleave', function() {
                this.style.transform = 'translateY(0) scale(1)';
                
                // Hide product actions
                const actions = this.querySelector('.product-actions');
                if (actions) {
                    actions.style.opacity = '0';
                    actions.style.transform = 'translateX(10px)';
                }
            });
        });
    }

    // Quantity controls animation
    function enhanceQuantityControls() {
        document.querySelectorAll('.quantity-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                this.style.transform = 'scale(0.9)';
                setTimeout(() => {
                    this.style.transform = 'scale(1)';
                }, 150);
            });
        });
    }

    // Search input enhancement
    function enhanceSearchInput() {
        const searchInput = document.querySelector('input[name="search"]');
        if (searchInput) {
            searchInput.addEventListener('focus', function() {
                this.parentElement.style.transform = 'scale(1.02)';
                this.parentElement.style.boxShadow = '0 8px 30px rgba(102, 126, 234, 0.2)';
            });
            
            searchInput.addEventListener('blur', function() {
                this.parentElement.style.transform = 'scale(1)';
                this.parentElement.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.1)';
            });
        }
    }

    // Toast notification system
    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type} border-0`;
        toast.setAttribute('role', 'alert');
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-check-circle me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        // Create toast container if it doesn't exist
        let toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }
        
        toastContainer.appendChild(toast);
        
        // Initialize and show toast
        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();
        
        // Remove toast element after it's hidden
        toast.addEventListener('hidden.bs.toast', function() {
            this.remove();
        });
    }

    // Initialize all enhancements
    enhanceAddToCartButtons();
    enhanceProductCards();
    enhanceQuantityControls();
    enhanceSearchInput();

    // Global cart animation trigger (can be called from other scripts)
    window.triggerCartAnimation = animateCartCount;
    window.showToast = showToast;
});

// Smooth scroll utility
function smoothScrollTo(element) {
    element.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });
}

// Debounce utility for search
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Export utilities for use in other scripts
window.cartUtils = {
    smoothScrollTo,
    debounce
};