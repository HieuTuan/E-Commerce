package com.mypkga.commerceplatformfull.entity;

/**
 * Enum representing the reasons for return requests.
 * These are predefined reasons that customers can select when requesting a return.
 */
public enum ReturnReason {
    DEFECTIVE_ITEM("Sản phẩm bị lỗi"),
    NOT_AS_DESCRIBED("Không đúng như mô tả"),
    WRONG_DELIVERY("Giao sai hàng"),
    DAMAGED_PACKAGING("Bao bì bị hỏng"),
    OTHER("Lý do khác");

    private final String displayName;

    ReturnReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}