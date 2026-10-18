package com.ghunsai.dairy.entity;

public enum MovementType {
    PURCHASE(true),
    SALE(false),
    WASTAGE(false),
    ADJUSTMENT_IN(true),
    ADJUSTMENT_OUT(false);

    private final boolean inbound;

    MovementType(boolean inbound) {
        this.inbound = inbound;
    }

    /** true = increases stock, false = decreases stock. */
    public boolean isInbound() {
        return inbound;
    }
}
