/**
 * Client-side order synchronization utility
 * Handles real-time status updates and conflict resolution
 */
class OrderSyncManager {
    constructor(orderId) {
        this.orderId = orderId;
        this.lastKnownStatus = null;
        this.lastKnownTimestamp = null;
        this.syncInterval = null;
        this.conflictResolver = new ConflictResolver();
    }

    /**
     * Start periodic synchronization
     */
    startSync(intervalMs = 30000) { // Default 30 seconds
        this.syncInterval = setInterval(() => {
            this.syncOrderStatus();
        }, intervalMs);
        
        // Initial sync
        this.syncOrderStatus();
    }

    /**
     * Stop synchronization
     */
    stopSync() {
        if (this.syncInterval) {
            clearInterval(this.syncInterval);
            this.syncInterval = null;
        }
    }

    /**
     * Sync order status with server
     */
    async syncOrderStatus() {
        try {
            const response = await fetch(`/api/sync/order/${this.orderId}/status`);
            const data = await response.json();
            
            if (response.ok) {
                this.handleStatusUpdate(data);
            } else {
                console.error('Sync failed:', data.error);
            }
        } catch (error) {
            console.error('Sync error:', error);
        }
    }

    /**
     * Handle status update from server
     */
    handleStatusUpdate(data) {
        const serverStatus = data.status;
        const serverTimestamp = new Date(data.lastUpdated);
        const isConsistent = data.isConsistent;

        // Check if status changed
        if (this.lastKnownStatus && this.lastKnownStatus !== serverStatus) {
            this.onStatusChanged(this.lastKnownStatus, serverStatus, serverTimestamp);
        }

        // Check for inconsistency
        if (!isConsistent) {
            this.onInconsistencyDetected(data);
        }

        // Update local state
        this.lastKnownStatus = serverStatus;
        this.lastKnownTimestamp = serverTimestamp;

        // Update UI
        this.updateUI(data);
    }

    /**
     * Handle status change event
     */
    onStatusChanged(oldStatus, newStatus, timestamp) {
        console.log(`Order ${this.orderId} status changed: ${oldStatus} -> ${newStatus}`);
        
        // Trigger custom event
        const event = new CustomEvent('orderStatusChanged', {
            detail: {
                orderId: this.orderId,
                oldStatus: oldStatus,
                newStatus: newStatus,
                timestamp: timestamp
            }
        });
        document.dispatchEvent(event);
    }

    /**
     * Handle data inconsistency
     */
    onInconsistencyDetected(data) {
        console.warn(`Data inconsistency detected for order ${this.orderId}`, data);
        
        // Show warning to user
        this.showInconsistencyWarning();
    }

    /**
     * Update UI with latest data
     */
    updateUI(data) {
        // Update status display
        const statusElements = document.querySelectorAll(`[data-order-status="${this.orderId}"]`);
        statusElements.forEach(element => {
            element.textContent = data.displayName;
            element.setAttribute('data-status', data.status);
        });

        // Update timestamp display
        const timestampElements = document.querySelectorAll(`[data-order-timestamp="${this.orderId}"]`);
        timestampElements.forEach(element => {
            element.textContent = new Date(data.lastUpdated).toLocaleString();
        });

        // Update consistency indicator
        const consistencyElements = document.querySelectorAll(`[data-order-consistency="${this.orderId}"]`);
        consistencyElements.forEach(element => {
            element.classList.toggle('consistent', data.isConsistent);
            element.classList.toggle('inconsistent', !data.isConsistent);
        });
    }

    /**
     * Show inconsistency warning
     */
    showInconsistencyWarning() {
        const warning = document.createElement('div');
        warning.className = 'alert alert-warning alert-dismissible fade show';
        warning.innerHTML = `
            <strong>Cảnh báo:</strong> Dữ liệu đơn hàng có thể không đồng bộ. 
            <button type="button" class="btn btn-sm btn-outline-warning ms-2" onclick="orderSync.forceSync()">
                Đồng bộ ngay
            </button>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        const container = document.querySelector('.order-details') || document.body;
        container.insertBefore(warning, container.firstChild);
    }

    /**
     * Force immediate synchronization
     */
    async forceSync() {
        await this.syncOrderStatus();
    }

    /**
     * Resolve conflict with server
     */
    async resolveConflict(clientStatus, clientTimestamp) {
        try {
            const response = await fetch(`/api/sync/order/${this.orderId}/resolve-conflict`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    clientStatus: clientStatus,
                    clientTimestamp: clientTimestamp.toISOString()
                })
            });

            const data = await response.json();
            
            if (response.ok) {
                this.handleConflictResolution(data);
                return data.resolvedStatus;
            } else {
                console.error('Conflict resolution failed:', data.error);
                return null;
            }
        } catch (error) {
            console.error('Conflict resolution error:', error);
            return null;
        }
    }

    /**
     * Handle conflict resolution result
     */
    handleConflictResolution(data) {
        if (data.wasConflict) {
            console.log(`Conflict resolved for order ${this.orderId}: ${data.clientStatus} -> ${data.resolvedStatus}`);
            
            // Show notification to user
            this.showConflictResolutionNotification(data);
        }

        // Update local state
        this.lastKnownStatus = data.resolvedStatus;
        this.lastKnownTimestamp = new Date(data.timestamp);
    }

    /**
     * Show conflict resolution notification
     */
    showConflictResolutionNotification(data) {
        const notification = document.createElement('div');
        notification.className = 'alert alert-info alert-dismissible fade show';
        notification.innerHTML = `
            <strong>Thông báo:</strong> Trạng thái đơn hàng đã được đồng bộ: ${data.displayName}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        const container = document.querySelector('.order-details') || document.body;
        container.insertBefore(notification, container.firstChild);
        
        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            notification.remove();
        }, 5000);
    }
}

/**
 * Conflict resolution utility
 */
class ConflictResolver {
    /**
     * Determine if client should accept server status
     */
    shouldAcceptServerStatus(clientTimestamp, serverTimestamp) {
        return new Date(serverTimestamp) > new Date(clientTimestamp);
    }

    /**
     * Get conflict resolution strategy
     */
    getResolutionStrategy(clientStatus, serverStatus, clientTimestamp, serverTimestamp) {
        if (this.shouldAcceptServerStatus(clientTimestamp, serverTimestamp)) {
            return {
                action: 'accept_server',
                resolvedStatus: serverStatus,
                reason: 'Server timestamp is newer'
            };
        } else {
            return {
                action: 'keep_client',
                resolvedStatus: clientStatus,
                reason: 'Client timestamp is newer or equal'
            };
        }
    }
}

// Global instance for easy access
let orderSync = null;

/**
 * Initialize order synchronization
 */
function initOrderSync(orderId) {
    if (orderSync) {
        orderSync.stopSync();
    }
    
    orderSync = new OrderSyncManager(orderId);
    orderSync.startSync();
    
    // Listen for page visibility changes to pause/resume sync
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            orderSync.stopSync();
        } else {
            orderSync.startSync();
        }
    });
    
    return orderSync;
}

/**
 * Cleanup on page unload
 */
window.addEventListener('beforeunload', () => {
    if (orderSync) {
        orderSync.stopSync();
    }
});