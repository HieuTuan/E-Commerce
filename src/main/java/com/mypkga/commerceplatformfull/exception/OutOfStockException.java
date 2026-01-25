package com.mypkga.commerceplatformfull.exception;

public class OutOfStockException extends RuntimeException {
    private final int availableStock;
    private final int requestedQuantity;

    public OutOfStockException(String message, int availableStock, int requestedQuantity) {
        super(message);
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
