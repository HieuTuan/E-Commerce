// Chatbot functionality
document.addEventListener('DOMContentLoaded', function () {
    const chatbotToggle = document.getElementById('chatbot-toggle');
    const chatbotWindow = document.getElementById('chatbot-window');
    const chatbotClose = document.getElementById('chatbot-close');
    const chatbotSend = document.getElementById('chatbot-send');
    const chatbotInput = document.getElementById('chatbot-input-field');
    const chatbotMessages = document.getElementById('chatbot-messages');

    let isTyping = false;

    if (chatbotToggle) {
        chatbotToggle.addEventListener('click', function () {
            if (chatbotWindow.style.display === 'none' || !chatbotWindow.style.display) {
                // Mở chatbot - ẩn toggle button
                chatbotWindow.style.display = 'flex';
                setTimeout(() => chatbotWindow.classList.add('show'), 10);
                chatbotToggle.style.display = 'none';
                chatbotInput.focus();
            } else {
                // Đóng chatbot - hiện toggle button
                chatbotWindow.classList.remove('show');
                setTimeout(() => {
                    chatbotWindow.style.display = 'none';
                    chatbotToggle.style.display = 'flex';
                }, 300);
            }
        });
    }

    if (chatbotClose) {
        chatbotClose.addEventListener('click', function () {
            chatbotWindow.classList.remove('show');
            setTimeout(() => {
                chatbotWindow.style.display = 'none';
                chatbotToggle.style.display = 'flex';
            }, 300);
        });
    }

    if (chatbotSend) {
        chatbotSend.addEventListener('click', sendMessage);
    }

    if (chatbotInput) {
        chatbotInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter' && !isTyping) {
                sendMessage();
            }
        });

        // Auto-focus when window opens
        chatbotInput.addEventListener('focus', function () {
            this.style.borderColor = '#0d6efd';
        });

        chatbotInput.addEventListener('blur', function () {
            this.style.borderColor = '#e9ecef';
        });
    }

    function sendMessage() {
        const message = chatbotInput.value.trim();
        if (!message || isTyping) return;

        // Disable input while processing
        isTyping = true;
        chatbotSend.disabled = true;
        chatbotInput.disabled = true;

        // Add user message to chat
        addMessage(message, 'user-message');
        chatbotInput.value = '';

        // Show typing indicator
        showTypingIndicator();

        // Send to backend with timeout
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout

        fetch('/api/chatbot/message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ message: message }),
            signal: controller.signal
        })
            .then(response => {
                clearTimeout(timeoutId);
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                hideTypingIndicator();
                // Support both { response: ... } and { reply: ... }
                const botText = (data && (data.response || data.reply)) ? (data.response || data.reply) : 'Xin lỗi, tôi không thể xử lý yêu cầu này lúc này.';

                // Simulate typing delay for better UX
                setTimeout(() => {
                    addMessage(botText, 'bot-message');
                    enableInput();
                }, 500);
            })
            .catch(error => {
                hideTypingIndicator();
                console.error('Error:', error);

                let errorMessage = 'Xin lỗi, đã xảy ra lỗi. Vui lòng thử lại sau.';
                if (error.name === 'AbortError') {
                    errorMessage = 'Yêu cầu hết thời gian chờ. Vui lòng thử lại.';
                } else if (error.message.includes('HTTP')) {
                    errorMessage = 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.';
                }

                setTimeout(() => {
                    addMessage(errorMessage, 'bot-message');
                    enableInput();
                }, 500);
            });
    }

    function enableInput() {
        isTyping = false;
        chatbotSend.disabled = false;
        chatbotInput.disabled = false;
        chatbotInput.focus();
    }

    function addMessage(text, className) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message ' + className;

        const messagePara = document.createElement('p');

        // Format text with line breaks and basic formatting
        const formattedText = formatBotMessage(text);
        messagePara.innerHTML = formattedText;
        messagePara.style.margin = '0';
        messagePara.style.fontSize = '14px';
        messagePara.style.lineHeight = '1.4';

        messageDiv.appendChild(messagePara);
        chatbotMessages.appendChild(messageDiv);

        // Scroll to bottom with smooth animation
        chatbotMessages.scrollTo({
            top: chatbotMessages.scrollHeight,
            behavior: 'smooth'
        });
    }

    function formatBotMessage(text) {
        // Convert line breaks to HTML breaks
        let formatted = text.replace(/\n/g, '<br>');

        // Format bullet points
        formatted = formatted.replace(/•\s*/g, '<span style="color: #0d6efd;">•</span> ');
        formatted = formatted.replace(/\*\s*/g, '<span style="color: #0d6efd;">•</span> ');

        // Format bold text **text** or __text__
        formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        formatted = formatted.replace(/__(.*?)__/g, '<strong>$1</strong>');

        // Format prices (detect all price formats including nghìn)
        formatted = formatted.replace(/(\d+(?:[.,]\d+)*)\s*(triệu|nghìn|VND|đ|tr)/gi,
            '<span class="price">$1 $2</span>');

        // Format product detail links  
        formatted = formatted.replace(/🔍 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-primary me-1 mb-1" style="text-decoration: none; font-size: 12px;">🔍 $1</a>');

        // Format add to cart links with product parameter
        formatted = formatted.replace(/🛒 \[([^\]]+)\]\(\?action=add-to-cart&product=(\d+)\)/g,
            '<button onclick="addToCartFromChat($2)" class="btn btn-sm btn-success me-1 mb-1" style="font-size: 12px;">🛒 $1</button>');

        // Format category/search links
        formatted = formatted.replace(/📋 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-info me-1 mb-1" style="text-decoration: none; font-size: 12px;">📋 $1</a>');

        // Format price filter links
        formatted = formatted.replace(/💲 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-warning me-1 mb-1" style="text-decoration: none; font-size: 12px;">💲 $1</a>');

        // Format compare links
        formatted = formatted.replace(/🔄 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-secondary me-1 mb-1" style="text-decoration: none; font-size: 12px;">🔄 $1</a>');

        // Format shop links
        formatted = formatted.replace(/🏪 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-primary me-1 mb-1" style="text-decoration: none; font-size: 12px;">🏪 $1</a>');

        // Format old style links (backward compatibility)
        formatted = formatted.replace(/🔗 \[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-primary me-1 mb-1" style="text-decoration: none; font-size: 12px;">👁️ $1</a>');

        // Format old add to cart links
        formatted = formatted.replace(/🛒 \[([^\]]+)\]\(#add-to-cart-(\d+)\)/g,
            '<button onclick="addToCartFromChat($2)" class="btn btn-sm btn-success me-1 mb-1" style="font-size: 12px;">🛒 $1</button>');

        // Handle inline links with | separator - new format
        formatted = formatted.replace(/🔍 \[([^\]]+)\]\(([^)]+)\) \| 🛒 \[([^\]]+)\]\(#add-to-cart-(\d+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-primary me-1 mb-1" style="font-size: 12px;">🔍 $1</a> <button onclick="addToCartFromChat($4)" class="btn btn-sm btn-success me-1 mb-1" style="font-size: 12px;">🛒 $3</button>');
        
        // Handle inline links with | separator - old format (backward compatibility)
        formatted = formatted.replace(/🔍 \[([^\]]+)\]\(([^)]+)\) \| 🛒 \[([^\]]+)\]\(\?action=add-to-cart&product=(\d+)\)/g,
            '<a href="$2" target="_blank" class="btn btn-sm btn-outline-primary me-1 mb-1" style="font-size: 12px;">🔍 $1</a> <button onclick="addToCartFromChat($4)" class="btn btn-sm btn-success me-1 mb-1" style="font-size: 12px;">🛒 $3</button>');

        return formatted;
    }

    function showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message typing-indicator';
        typingDiv.id = 'typing-indicator';

        const typingDots = document.createElement('div');
        typingDots.className = 'typing-dots';
        typingDots.innerHTML = '<span></span><span></span><span></span>';

        typingDiv.appendChild(typingDots);
        chatbotMessages.appendChild(typingDiv);

        // Scroll to bottom
        chatbotMessages.scrollTo({
            top: chatbotMessages.scrollHeight,
            behavior: 'smooth'
        });
    }

    function hideTypingIndicator() {
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    // Function to add product to cart from chatbot
    window.addToCartFromChat = function (productId) {
        const button = event.target;
        const originalText = button.innerHTML;

        // Show loading state
        button.innerHTML = '⏳ Đang thêm...';
        button.disabled = true;

        // Use the new chatbot action endpoint
        fetch(`/api/chatbot/action?action=add-to-cart&product=${productId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    // Success - update button and cart count
                    button.innerHTML = '✅ Đã thêm';
                    button.style.backgroundColor = '#28a745';

                    // Update cart count in navbar
                    const cartCount = document.querySelector('.cart-count');
                    if (cartCount) {
                        const currentCount = parseInt(cartCount.textContent) || 0;
                        cartCount.textContent = currentCount + 1;
                    }

                    // Add success message to chat
                    addMessage('✅ ' + data.message, 'bot-message');

                    // Reset button after 3 seconds
                    setTimeout(() => {
                        button.innerHTML = originalText;
                        button.disabled = false;
                        button.style.backgroundColor = '';
                    }, 3000);
                } else {
                    // Handle authentication requirement
                    if (data.requireLogin) {
                        button.innerHTML = '🔐 Đăng nhập';
                        button.style.backgroundColor = '#ffc107';
                        
                        // Add login message to chat with login link
                        addMessage('🔐 ' + data.message + ' [Đăng nhập ngay](/login?redirect=' + encodeURIComponent(window.location.pathname) + ')', 'bot-message');
                        
                        // Make button clickable to redirect to login
                        button.onclick = () => {
                            window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
                        };
                    } else {
                        // Other errors
                        button.innerHTML = '❌ Lỗi';
                        button.style.backgroundColor = '#dc3545';
                        
                        // Add error message to chat
                        addMessage('❌ ' + data.message, 'bot-message');
                        
                        // Reset button after 3 seconds
                        setTimeout(() => {
                            button.innerHTML = originalText;
                            button.disabled = false;
                            button.style.backgroundColor = '';
                        }, 3000);
                    }
                }
            })
            .catch(error => {
                console.error('Error adding to cart:', error);
                button.innerHTML = '❌ Lỗi';
                button.style.backgroundColor = '#dc3545';

                // Add error message to chat
                addMessage('❌ Có lỗi xảy ra khi thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.', 'bot-message');

                // Reset button after 3 seconds
                setTimeout(() => {
                    button.innerHTML = originalText;
                    button.disabled = false;
                    button.style.backgroundColor = '';
                }, 3000);
            });
    };
});

// Auto-dismiss alerts
setTimeout(function () {
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function (alert) {
        if (alert.querySelector('.btn-close')) {
            setTimeout(function () {
                alert.classList.add('fade');
                setTimeout(function () {
                    alert.remove();
                }, 150);
            }, 3000);
        }
    });
}, 100);
