# Design Document

## Overview

Thiết kế này mô tả hệ thống hiệu ứng động cho giỏ hàng nhằm cải thiện trải nghiệm người dùng khi thêm sản phẩm. Hệ thống bao gồm:

1. **Product Animation**: Hiệu ứng di chuyển sản phẩm từ vị trí click đến icon giỏ hàng
2. **Cart Badge Animation**: Hiệu ứng cập nhật số lượng với màu đỏ nổi bật
3. **Mini Cart Preview**: Hiển thị tạm thời giỏ hàng thu nhỏ
4. **Performance Optimization**: Đảm bảo hiệu suất và accessibility

## Architecture

Kiến trúc hệ thống animation được tích hợp vào frontend hiện tại:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Product Page  │───▶│  Cart Animation  │───▶│   Cart Icon     │
│   (Any Page)    │    │    Controller    │    │   & Badge       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Add to Cart    │    │   Animation      │    │   Mini Cart     │
│    Button       │    │   Engine (JS)    │    │   Component     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   CSS Animations │
                       │   & Transitions  │
                       └──────────────────┘
```

### Animation Flow Diagram
```
User Click → Product Animation → Cart Badge Update → Mini Cart Show → Auto Hide
     │              │                    │                │            │
     ▼              ▼                    ▼                ▼            ▼
[Add Button]  [Flying Effect]    [Number Bounce]   [Cart Preview]  [Fade Out]
```

## Components and Interfaces

### 1. Cart Animation Controller (New)
- Quản lý tất cả animation sequences
- Coordinate giữa các animation components
- Handle animation queuing và timing
- Provide API cho các page components

### 2. Product Flying Animation (New)
- Tạo hiệu ứng sản phẩm bay từ button đến cart icon
- Sử dụng CSS transforms và JavaScript để tính toán trajectory
- Clone product image để tạo animation element
- Remove animation element sau khi hoàn thành

### 3. Cart Badge Component (Updated)
- Cập nhật để support animation effects
- Thêm bounce/pulse animation khi số lượng thay đổi
- Implement red notification styling
- Handle số lượng = 0 (hide badge)

### 4. Mini Cart Component (New)
- Hiển thị cart preview với animation
- Show/hide với smooth transitions
- Display product info và quick actions
- Position dynamically based on cart icon location

### 5. Animation Engine (New)
- Core JavaScript module để handle animations
- Provide reusable animation functions
- Handle performance optimization
- Respect user motion preferences

### 6. Accessibility Handler (New)
- Implement ARIA announcements
- Handle reduced motion preferences
- Ensure keyboard navigation compatibility
- Provide alternative feedback for screen readers

## Data Models

### Animation Configuration
```javascript
const AnimationConfig = {
    // Product flying animation
    productFly: {
        duration: 800, // milliseconds
        easing: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)',
        trajectory: 'curved', // straight, curved, arc
        scale: {
            start: 1.0,
            end: 0.3
        }
    },
    
    // Cart badge animation
    badge: {
        bounce: {
            duration: 600,
            scale: 1.2,
            iterations: 2
        },
        color: {
            normal: '#6c757d',
            notification: '#dc3545', // Bootstrap danger red
            duration: 2000 // how long to show red
        }
    },
    
    // Mini cart animation
    miniCart: {
        show: {
            duration: 300,
            easing: 'ease-out',
            transform: 'translateY(-10px) scale(0.95) to translateY(0) scale(1)'
        },
        hide: {
            duration: 200,
            delay: 2500, // auto-hide after 2.5s
            easing: 'ease-in'
        }
    }
};
```

### Cart State Management
```javascript
class CartAnimationState {
    constructor() {
        this.isAnimating = false;
        this.animationQueue = [];
        this.currentCartCount = 0;
        this.miniCartVisible = false;
    }
    
    addToQueue(animation) {
        this.animationQueue.push(animation);
        if (!this.isAnimating) {
            this.processQueue();
        }
    }
    
    processQueue() {
        // Process animation queue sequentially
    }
}
```

## Technical Implementation

### CSS Classes for Animations
```css
/* Product flying animation */
.product-fly-animation {
    position: fixed;
    pointer-events: none;
    z-index: 9999;
    transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

/* Cart badge animations */
.cart-badge {
    transition: all 0.3s ease;
}

.cart-badge.bounce {
    animation: badgeBounce 0.6s ease-in-out;
}

.cart-badge.notification {
    background-color: #dc3545 !important;
    animation: badgePulse 0.5s ease-in-out;
}

/* Mini cart animations */
.mini-cart {
    transform: translateY(-10px) scale(0.95);
    opacity: 0;
    transition: all 0.3s ease-out;
}

.mini-cart.show {
    transform: translateY(0) scale(1);
    opacity: 1;
}

/* Keyframe animations */
@keyframes badgeBounce {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.2); }
}

@keyframes badgePulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.1); }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
    .product-fly-animation,
    .cart-badge,
    .mini-cart {
        transition: none !important;
        animation: none !important;
    }
}
```

### JavaScript Animation Controller
```javascript
class CartAnimationController {
    constructor() {
        this.state = new CartAnimationState();
        this.config = AnimationConfig;
        this.respectsReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }
    
    async animateAddToCart(productElement, cartIcon, productData) {
        if (this.respectsReducedMotion) {
            return this.handleReducedMotion(productData);
        }
        
        // 1. Create flying animation
        await this.createProductFlyAnimation(productElement, cartIcon);
        
        // 2. Update cart badge with animation
        this.animateCartBadge();
        
        // 3. Show mini cart
        this.showMiniCart(productData);
        
        // 4. Announce to screen readers
        this.announceToScreenReader(productData);
    }
    
    createProductFlyAnimation(source, target) {
        // Implementation for flying animation
    }
    
    animateCartBadge() {
        // Implementation for badge animation
    }
    
    showMiniCart(productData) {
        // Implementation for mini cart display
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*
### Property Reflection

After reviewing all properties identified in the prework, I've identified several areas where properties can be consolidated:

**Redundancy Analysis:**
- Properties about animation sequence (1.1, 1.2, 1.3, 1.4) can be combined into animation flow properties
- Properties about badge behavior (2.1, 2.2, 2.4) can be consolidated into badge animation properties
- Properties about consistency across pages (5.1, 5.2, 5.3, 5.5) can be combined into cross-page consistency properties
- Properties about mini cart content (3.1, 3.2) can be combined into mini cart display properties

**Consolidated Properties:**

Property 1: Animation sequence flow
*For any* add to cart action, the system should execute animations in the correct sequence: product fly → badge update → mini cart show → auto hide
**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

Property 2: Badge animation behavior
*For any* cart quantity change, the badge should animate appropriately (bounce/pulse), show red notification color, and hide when cart is empty
**Validates: Requirements 2.1, 2.2, 2.4**

Property 3: Animation queuing integrity
*For any* rapid sequence of add to cart actions, animations should be queued properly without overlap or interference
**Validates: Requirements 2.3**

Property 4: Mini cart content accuracy
*For any* product added to cart, the mini cart should display correct product information including image, name, and quantity
**Validates: Requirements 3.1, 3.2**

Property 5: Accessibility compliance
*For any* animation or interaction, the system should respect reduced motion preferences and provide appropriate ARIA announcements
**Validates: Requirements 4.2, 4.4**

Property 6: Cross-page consistency
*For any* page where products can be added to cart, the animation system should work consistently regardless of page context
**Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

Property 7: Hover interaction responsiveness
*For any* hover event on the cart icon, the mini cart should display on demand
**Validates: Requirements 3.4**

## Error Handling

### Animation Failures
- Missing cart icon: Provide alternative visual feedback (toast notification)
- Animation interruption: Clean up animation elements and update state correctly
- Performance issues: Fallback to simple state updates without animations
- JavaScript errors: Ensure cart functionality works even if animations fail

### Accessibility Errors
- Screen reader compatibility: Ensure ARIA announcements work even if visual animations fail
- Reduced motion: Gracefully disable animations while maintaining functionality
- Keyboard navigation: Ensure all interactive elements remain accessible

### Cross-browser Issues
- CSS animation support: Provide fallbacks for older browsers
- JavaScript compatibility: Use polyfills where necessary
- Performance variations: Adjust animation complexity based on device capabilities

## Testing Strategy

**Dual testing approach requirements**:

The system MUST use both unit testing and property-based testing approaches:
- Unit tests verify specific animation behaviors, timing, and UI interactions
- Property tests verify universal animation properties across different scenarios
- Integration tests verify end-to-end animation flows and cross-page consistency

**Property-based testing requirements**:
- Use Jest with jsdom for JavaScript animation testing
- Configure each property-based test to run a minimum of 100 iterations
- Tag each property-based test with comments referencing the design document property
- Use format: '**Feature: cart-animation-effects, Property {number}: {property_text}**'
- Each correctness property MUST be implemented by a SINGLE property-based test

**Unit testing requirements**:
- Unit tests cover specific animation timing and sequencing
- Test individual animation components (badge, mini cart, flying animation)
- Test accessibility features and reduced motion handling
- Test cross-browser compatibility scenarios

**Integration testing requirements**:
- Test complete add-to-cart flows from different pages
- Test animation performance under various conditions
- Test user interaction scenarios (hover, rapid clicking, etc.)
- Test responsive behavior across different screen sizes

**Test Data Generation**:
- Generate random product data for animation testing
- Generate various cart states (empty, single item, multiple items)
- Generate different page contexts (product list, detail, search results)
- Generate different user interaction patterns (single click, rapid clicks, hover)