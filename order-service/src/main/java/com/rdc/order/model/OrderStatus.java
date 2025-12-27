package com.rdc.order.model;

/**
 * Order Status Enum
 *
 * Status transitions:
 * CREATED -> PAID (after payment)
 * CREATED -> CANCELLED (user cancels)
 * PAID -> REFUNDED (admin refunds)
 */
public enum OrderStatus {
    /**
     * Order created, awaiting payment.
     * User can cancel at this stage.
     */
    CREATED,

    /**
     * Payment completed.
     * User cannot cancel at this stage.
     */
    PAID,

    /**
     * Order cancelled by user.
     * Only possible when status is CREATED.
     */
    CANCELLED,

    /**
     * Order refunded by admin.
     * Only possible when status is PAID.
     */
    REFUNDED
}
