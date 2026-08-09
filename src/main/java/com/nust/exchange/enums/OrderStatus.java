package com.nust.exchange.enums;

/**
 * Lifecycle state of an order as it moves through the exchange.
 */
public enum OrderStatus {
    /** Live in the order book, nothing filled yet. */
    OPEN("Open"),
    /** Some quantity filled, remainder still resting in the book. */
    PARTIAL("Partially Filled"),
    /** Fully filled - no quantity remaining. */
    FILLED("Filled"),
    /** Cancelled by the trader before being fully filled. */
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** @return true when no further matching is possible for this order. */
    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED;
    }
}
