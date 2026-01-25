/**
 * OTP Verification JavaScript Module
 * Handles OTP input, validation, timer display, and resend functionality
 */

document.addEventListener('DOMContentLoaded', function() {
    // Get DOM elements
    const otpInput = document.getElementById('otp');
    const verifyBtn = document.getElementById('verify-btn');
    const resendBtn = document.getElementById('resend-btn');
    const resendTimer = document.getElementById('resend-timer');
    const countdown = document.getElementById('countdown');
    const statusMessage = document.getElementById('status-message');
    const verifyForm = document.getElementById('verify-form');

    // Timer variables
    let countdownInterval;
    let otpExpiryTimer;
    
    // Configuration
    const OTP_LENGTH = 6;
    const RESEND_COOLDOWN = 60; // seconds
    const OTP_EXPIRY_TIME = 300; // 5 minutes in seconds
    const STATUS_CHECK_INTERVAL = 30000; // 30 seconds

    // Initialize OTP verification
    init();

    function init() {
        setupOTPInput();
        setupFormSubmission();
        setupResendFunctionality();
        startOTPExpiryTimer();
        startStatusChecking();
        
        // Check initial status
        checkOTPStatus();
    }

    /**
     * Setup OTP input field with real-time validation
     */
    function setupOTPInput() {
        if (!otpInput) return;

        // Auto-format OTP input (numbers only)
        otpInput.addEventListener('input', function(e) {
            // Remove non-numeric characters
            let value = e.target.value.replace(/[^0-9]/g, '');
            
            // Limit to OTP_LENGTH digits
            if (value.length > OTP_LENGTH) {
                value = value.substring(0, OTP_LENGTH);
            }
            
            e.target.value = value;
            
            // Real-time validation
            validateOTPInput(value);
            
            // Auto-focus verify button when OTP is complete
            if (value.length === OTP_LENGTH) {
                verifyBtn.focus();
            }
        });

        // Handle paste events
        otpInput.addEventListener('paste', function(e) {
            e.preventDefault();
            const clipboardData = e.clipboardData || (window.clipboardData && window.clipboardData);
            if (clipboardData) {
                const pastedData = clipboardData.getData('text');
                const numericData = pastedData.replace(/[^0-9]/g, '').substring(0, OTP_LENGTH);
                otpInput.value = numericData;
                validateOTPInput(numericData);
            }
        });

        // Handle keydown for better UX
        otpInput.addEventListener('keydown', function(e) {
            // Allow backspace, delete, tab, escape, enter, arrow keys
            const allowedKeys = ['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'];
            
            if (allowedKeys.includes(e.code) ||
                // Allow Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
                (e.ctrlKey && ['KeyA', 'KeyC', 'KeyV', 'KeyX'].includes(e.code))) {
                return;
            }
            
            // Allow only numeric keys (0-9) from main keyboard and numpad
            const isNumericKey = (e.code >= 'Digit0' && e.code <= 'Digit9') || 
                                (e.code >= 'Numpad0' && e.code <= 'Numpad9');
            
            if (!isNumericKey || e.shiftKey) {
                e.preventDefault();
            }
        });
    }

    /**
     * Validate OTP input in real-time
     */
    function validateOTPInput(value) {
        const isValid = value.length === OTP_LENGTH && /^\d{6}$/.test(value);
        
        // Update input styling
        if (value.length === 0) {
            otpInput.classList.remove('is-valid', 'is-invalid');
        } else if (isValid) {
            otpInput.classList.remove('is-invalid');
            otpInput.classList.add('is-valid');
        } else {
            otpInput.classList.remove('is-valid');
            otpInput.classList.add('is-invalid');
        }
        
        // Enable/disable verify button
        if (verifyBtn) {
            verifyBtn.disabled = !isValid;
        }
        
        return isValid;
    }

    /**
     * Setup form submission with validation
     */
    function setupFormSubmission() {
        if (!verifyForm) return;

        verifyForm.addEventListener('submit', function(e) {
            const otpValue = otpInput.value.trim();
            
            // Validate OTP before submission
            if (!validateOTPInput(otpValue)) {
                e.preventDefault();
                showStatus('Please enter a valid 6-digit verification code', 'danger');
                return;
            }

            // Show loading state
            if (verifyBtn) {
                verifyBtn.disabled = true;
                verifyBtn.classList.add('btn-loading');
                const originalText = verifyBtn.innerHTML;
                verifyBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Verifying...';
                
                // Reset button state if form submission fails (e.g., validation error)
                setTimeout(() => {
                    if (verifyBtn.classList.contains('btn-loading')) {
                        verifyBtn.disabled = false;
                        verifyBtn.classList.remove('btn-loading');
                        verifyBtn.innerHTML = originalText;
                    }
                }, 10000); // Reset after 10 seconds as fallback
            }
        });
    }

    /**
     * Setup resend OTP functionality
     */
    function setupResendFunctionality() {
        if (!resendBtn) return;

        resendBtn.addEventListener('click', function() {
            resendOTP();
        });
    }

    /**
     * Resend OTP with proper error handling
     */
    function resendOTP() {
        resendBtn.disabled = true;
        showStatus('Sending verification code...', 'info');

        // Determine the correct endpoint based on current page
        const isPhoneVerification = window.location.pathname.includes('verify-phone');
        const endpoint = isPhoneVerification ? '/api/resend-phone-otp' : '/api/resend-otp';

        fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showStatus('Verification code sent successfully!', 'success');
                startResendTimer(data.waitTime || RESEND_COOLDOWN);
                
                // Reset OTP expiry timer
                startOTPExpiryTimer();
                
                // Clear current OTP input
                if (otpInput) {
                    otpInput.value = '';
                    otpInput.classList.remove('is-valid', 'is-invalid');
                }
            } else {
                showStatus(data.message || 'Failed to send verification code', 'danger');
                if (data.waitTime && data.waitTime > 0) {
                    startResendTimer(data.waitTime);
                } else {
                    resendBtn.disabled = false;
                }
            }
        })
        .catch(error => {
            console.error('Error resending OTP:', error);
            showStatus('Failed to send verification code. Please try again.', 'danger');
            resendBtn.disabled = false;
        });
    }

    /**
     * Start resend cooldown timer
     */
    function startResendTimer(seconds) {
        if (!resendBtn || !resendTimer || !countdown) return;

        resendBtn.style.display = 'none';
        resendTimer.style.display = 'block';
        countdown.textContent = seconds;

        // Clear any existing interval
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        countdownInterval = setInterval(function() {
            seconds--;
            countdown.textContent = seconds;

            if (seconds <= 0) {
                clearInterval(countdownInterval);
                resendTimer.style.display = 'none';
                resendBtn.style.display = 'inline-block';
                resendBtn.disabled = false;
            }
        }, 1000);
    }

    /**
     * Start OTP expiry timer
     */
    function startOTPExpiryTimer() {
        // Clear existing timer
        if (otpExpiryTimer) {
            clearTimeout(otpExpiryTimer);
        }

        otpExpiryTimer = setTimeout(function() {
            showStatus('Verification code has expired. Please request a new one.', 'warning');
            
            // Disable verify button
            if (verifyBtn) {
                verifyBtn.disabled = true;
            }
            
            // Clear OTP input
            if (otpInput) {
                otpInput.value = '';
                otpInput.classList.remove('is-valid', 'is-invalid');
            }
        }, OTP_EXPIRY_TIME * 1000);
    }

    /**
     * Show status message with auto-hide
     */
    function showStatus(message, type, autoHide = true) {
        if (!statusMessage) return;

        // Add appropriate icon based on message type
        let icon = '';
        switch(type) {
            case 'success':
                icon = '<i class="fas fa-check-circle"></i>';
                break;
            case 'danger':
                icon = '<i class="fas fa-exclamation-triangle"></i>';
                break;
            case 'warning':
                icon = '<i class="fas fa-exclamation-circle"></i>';
                break;
            case 'info':
                icon = '<i class="fas fa-info-circle"></i>';
                break;
            default:
                icon = '<i class="fas fa-info-circle"></i>';
        }

        statusMessage.className = `status-message alert-${type}`;
        statusMessage.innerHTML = `${icon}<span>${message}</span>`;
        statusMessage.style.display = 'block';

        if (autoHide && type !== 'danger') {
            setTimeout(function() {
                if (statusMessage) {
                    statusMessage.style.display = 'none';
                }
            }, 5000);
        }
    }

    /**
     * Check OTP status periodically
     */
    function checkOTPStatus() {
        fetch('/api/otp-status', {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                handleOTPStatusResponse(data);
            }
        })
        .catch(error => {
            console.error('Error checking OTP status:', error);
        });
    }

    /**
     * Handle OTP status response
     */
    function handleOTPStatusResponse(data) {
        if (data.isBlocked) {
            showStatus('Phone number is temporarily blocked due to too many failed attempts', 'danger', false);
            
            // Disable all controls
            if (verifyBtn) verifyBtn.disabled = true;
            if (resendBtn) resendBtn.disabled = true;
            if (otpInput) otpInput.disabled = true;
            
        } else if (data.remainingAttempts !== undefined) {
            updateAttemptCounter(data.remainingAttempts);
            
            if (data.remainingAttempts <= 0) {
                showStatus('No more attempts remaining. Please request a new code.', 'warning');
                if (verifyBtn) verifyBtn.disabled = true;
            } else if (data.remainingAttempts <= 2) {
                showStatus(`${data.remainingAttempts} attempts remaining`, 'warning');
            }
        }

        // Handle resend cooldown
        if (data.secondsUntilNextRequest && data.secondsUntilNextRequest > 0) {
            startResendTimer(data.secondsUntilNextRequest);
        }
    }

    /**
     * Update attempt counter display
     */
    function updateAttemptCounter(remainingAttempts) {
        let counterElement = document.getElementById('attempt-counter');
        
        if (!counterElement) {
            // Create attempt counter element if it doesn't exist
            counterElement = document.createElement('div');
            counterElement.id = 'attempt-counter';
            counterElement.className = 'attempt-counter';
            
            // Insert after the OTP input
            const otpContainer = otpInput.closest('.mb-4') || otpInput.parentElement;
            if (otpContainer && otpContainer.nextSibling) {
                otpContainer.parentElement.insertBefore(counterElement, otpContainer.nextSibling);
            }
        }
        
        if (remainingAttempts <= 0) {
            counterElement.className = 'attempt-counter danger';
            counterElement.innerHTML = '<i class="fas fa-ban"></i> No attempts remaining';
        } else if (remainingAttempts <= 2) {
            counterElement.className = 'attempt-counter danger';
            counterElement.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${remainingAttempts} attempts remaining`;
        } else if (remainingAttempts <= 5) {
            counterElement.className = 'attempt-counter';
            counterElement.innerHTML = `<i class="fas fa-info-circle"></i> ${remainingAttempts} attempts remaining`;
        } else {
            // Hide counter if plenty of attempts remain
            counterElement.style.display = 'none';
            return;
        }
        
        counterElement.style.display = 'block';
    }

    /**
     * Start periodic status checking
     */
    function startStatusChecking() {
        // Check status every 30 seconds
        setInterval(checkOTPStatus, STATUS_CHECK_INTERVAL);
    }

    /**
     * Cleanup function
     */
    function cleanup() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
        if (otpExpiryTimer) {
            clearTimeout(otpExpiryTimer);
        }
    }

    // Cleanup on page unload
    window.addEventListener('beforeunload', cleanup);
});