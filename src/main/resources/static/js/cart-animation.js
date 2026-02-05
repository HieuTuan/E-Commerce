// Cart Animation Script
// Implements "fly-to-cart" effect and AJAX form submission

document.addEventListener('DOMContentLoaded', function () {

    // Attach listeners to all add-to-cart forms
    const forms = document.querySelectorAll('form[action*="/cart/add"]');
    forms.forEach(form => {
        form.addEventListener('submit', handleAddToCart);
    });

    // Cart count pulse animation
    function animateCartCount() {
        const cartCount = document.querySelector('.cart-count');
        if (cartCount) {
            cartCount.classList.remove('pulse'); // Reset
            void cartCount.offsetWidth; // Trigger reflow
            cartCount.classList.add('pulse');
        }
    }

    // Handle form submission
    function handleAddToCart(e) {
        e.preventDefault();
        const form = this;
        const button = form.querySelector('button[type="submit"]');

        // Prevent double clicks
        if (button.disabled) return;

        // Visual feedback
        const originalText = button.innerHTML;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Adding...';
        button.disabled = true;

        // Perform Animation
        performFlyToCartAnimation(button, () => {
            // Submit Data via AJAX
            const formData = new FormData(form);

            fetch(form.action, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(response => {
                    if (response.ok) {
                        // Update Cart Count (Optimistic +1)
                        updateCartCount(1);
                        animateCartCount();

                        // Show success toast (optional)
                        // showToast("Item added to cart!");
                    } else {
                        // Start fallback generic error
                        console.error("Failed to add to cart");
                        window.location.reload(); // Fallback to reload
                    }
                })
                .catch(error => {
                    console.error("Error:", error);
                    window.location.reload(); // Fallback
                })
                .finally(() => {
                    // Reset button
                    setTimeout(() => {
                        button.innerHTML = originalText;
                        button.disabled = false;
                    }, 500);
                });
        });
    }

    function performFlyToCartAnimation(button, callback) {
        // Find product image
        const productCard = button.closest('.card');
        let productImg = productCard ? productCard.querySelector('img') : null;

        // Fallback image if not found
        if (!productImg) {
            callback(); // Skip animation
            return;
        }

        // Find Cart Icon
        const cartIcon = document.querySelector('.nav-link .fas.fa-shopping-cart');
        if (!cartIcon) {
            callback();
            return;
        }

        // Clone Image
        const imgClone = productImg.cloneNode(true);
        const rect = productImg.getBoundingClientRect();

        imgClone.style.position = 'fixed';
        imgClone.style.left = rect.left + 'px';
        imgClone.style.top = rect.top + 'px';
        imgClone.style.width = rect.width + 'px';
        imgClone.style.height = rect.height + 'px';
        imgClone.style.opacity = '0.8';
        imgClone.style.zIndex = '9999';
        imgClone.style.borderRadius = '50%';
        imgClone.style.transition = 'all 0.8s ease-in-out';
        imgClone.style.pointerEvents = 'none'; // Prevent interfering with clicks

        document.body.appendChild(imgClone);

        // Trigger Animation (next frame)
        requestAnimationFrame(() => {
            const cartRect = cartIcon.getBoundingClientRect();

            imgClone.style.left = (cartRect.left + cartRect.width / 2 - 15) + 'px'; // Center roughly
            imgClone.style.top = (cartRect.top + cartRect.height / 2 - 15) + 'px';
            imgClone.style.width = '30px';
            imgClone.style.height = '30px';
            imgClone.style.opacity = '0';
        });

        // Cleanup and Callback
        setTimeout(() => {
            if (document.body.contains(imgClone)) {
                document.body.removeChild(imgClone);
            }
            callback();
        }, 800); // Match transition duration
    }

    function updateCartCount(change) {
        const cartCountEl = document.querySelector('.cart-count');
        if (cartCountEl) {
            let current = parseInt(cartCountEl.innerText) || 0;
            cartCountEl.innerText = current + change;
        }
    }
});