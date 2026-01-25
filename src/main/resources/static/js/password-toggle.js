/**
 * Password Toggle Module
 * Provides functionality to show/hide password fields with eye icon toggle
 */
class PasswordToggle {
    constructor() {
        this.init();
    }

    init() {
        document.addEventListener('DOMContentLoaded', () => {
            this.setupPasswordToggles();
        });
    }

    /**
     * Set up password toggle functionality for all password fields
     */
    setupPasswordToggles() {
        const passwordToggles = document.querySelectorAll('.password-toggle');
        
        passwordToggles.forEach(toggle => {
            toggle.addEventListener('click', (e) => {
                e.preventDefault();
                this.togglePasswordVisibility(toggle);
            });
        });

        // Also setup for dynamically created password fields
        this.observePasswordFields();
    }

    /**
     * Toggle password visibility for a specific toggle button
     * @param {HTMLElement} toggle - The toggle button element
     */
    togglePasswordVisibility(toggle) {
        const targetId = toggle.getAttribute('data-target');
        const passwordInput = targetId ? 
            document.getElementById(targetId) : 
            toggle.parentElement.querySelector('input[type="password"], input[type="text"]');
        
        if (!passwordInput) {
            console.warn('Password input not found for toggle');
            return;
        }

        const passwordIcon = toggle.querySelector('.password-icon, .fas, .fa');
        
        // Store current cursor position
        const cursorPosition = passwordInput.selectionStart;
        
        // Toggle input type
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        
        // Update icon
        this.updateToggleIcon(passwordIcon, !isPassword);
        
        // Restore cursor position
        setTimeout(() => {
            passwordInput.setSelectionRange(cursorPosition, cursorPosition);
            passwordInput.focus();
        }, 0);
        
        // Update ARIA attributes for accessibility
        this.updateAriaAttributes(toggle, passwordInput, !isPassword);
    }

    /**
     * Update the toggle icon based on password visibility state
     * @param {HTMLElement} icon - The icon element
     * @param {boolean} isVisible - Whether password is visible
     */
    updateToggleIcon(icon, isVisible) {
        if (!icon) return;

        if (isVisible) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
            icon.title = 'Hide password';
        } else {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
            icon.title = 'Show password';
        }
    }

    /**
     * Update ARIA attributes for accessibility
     * @param {HTMLElement} toggle - The toggle button
     * @param {HTMLElement} input - The password input
     * @param {boolean} isVisible - Whether password is visible
     */
    updateAriaAttributes(toggle, input, isVisible) {
        toggle.setAttribute('aria-label', isVisible ? 'Hide password' : 'Show password');
        input.setAttribute('aria-describedby', 
            isVisible ? 'password-visible' : 'password-hidden');
    }

    /**
     * Create a password toggle button for a password input
     * @param {HTMLElement} passwordInput - The password input element
     * @returns {HTMLElement} The created toggle button
     */
    createToggleButton(passwordInput) {
        const toggleButton = document.createElement('button');
        toggleButton.type = 'button';
        toggleButton.className = 'btn btn-outline-secondary position-absolute end-0 top-0 h-100 px-3 password-toggle';
        toggleButton.style.cssText = 'border-left: none; z-index: 10;';
        toggleButton.setAttribute('data-target', passwordInput.id);
        toggleButton.setAttribute('aria-label', 'Show password');
        
        const icon = document.createElement('i');
        icon.className = 'fas fa-eye password-icon';
        icon.title = 'Show password';
        
        toggleButton.appendChild(icon);
        
        return toggleButton;
    }

    /**
     * Add password toggle to an existing password input
     * @param {HTMLElement} passwordInput - The password input element
     */
    addToggleToPasswordInput(passwordInput) {
        // Check if toggle already exists
        const existingToggle = passwordInput.parentElement.querySelector('.password-toggle');
        if (existingToggle) return;

        // Ensure the input has an ID
        if (!passwordInput.id) {
            passwordInput.id = 'password-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
        }

        // Wrap input in relative positioned container if not already
        const parent = passwordInput.parentElement;
        if (!parent.classList.contains('position-relative')) {
            const wrapper = document.createElement('div');
            wrapper.className = 'position-relative';
            parent.insertBefore(wrapper, passwordInput);
            wrapper.appendChild(passwordInput);
        }

        // Create and add toggle button
        const toggleButton = this.createToggleButton(passwordInput);
        passwordInput.parentElement.appendChild(toggleButton);

        // Set up event listener
        toggleButton.addEventListener('click', (e) => {
            e.preventDefault();
            this.togglePasswordVisibility(toggleButton);
        });
    }

    /**
     * Observe for dynamically added password fields
     */
    observePasswordFields() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === Node.ELEMENT_NODE) {
                        // Check if the added node is a password input
                        if (node.type === 'password') {
                            this.addToggleToPasswordInput(node);
                        }
                        
                        // Check for password inputs within the added node
                        const passwordInputs = node.querySelectorAll && 
                            node.querySelectorAll('input[type="password"]');
                        if (passwordInputs) {
                            passwordInputs.forEach(input => {
                                this.addToggleToPasswordInput(input);
                            });
                        }
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    /**
     * Initialize password toggle for specific container
     * @param {HTMLElement} container - Container element to search for password inputs
     */
    initializeContainer(container) {
        const passwordInputs = container.querySelectorAll('input[type="password"]');
        passwordInputs.forEach(input => {
            this.addToggleToPasswordInput(input);
        });
    }

    /**
     * Destroy password toggle functionality
     */
    destroy() {
        const toggles = document.querySelectorAll('.password-toggle');
        toggles.forEach(toggle => {
            toggle.removeEventListener('click', this.togglePasswordVisibility);
            toggle.remove();
        });
    }
}

// Auto-initialize when script loads
const passwordToggle = new PasswordToggle();

// Export for manual initialization if needed
window.PasswordToggle = PasswordToggle;