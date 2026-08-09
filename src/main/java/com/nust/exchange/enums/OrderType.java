package com.nust.exchange.enums;

/**
 * The execution style of an order.
 *
 * <ul>
 *   <li>{@link #MARKET} - execute immediately at the best available price.</li>
 *   <li>{@link #LIMIT} - execute only at the limit price or better.</li>
 * </ul>
 */
public enum OrderType {
    MARKET("Market"),
    LIMIT("Limit");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
