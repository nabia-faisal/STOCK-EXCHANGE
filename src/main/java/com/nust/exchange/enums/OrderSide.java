package com.nust.exchange.enums;

/**
 * Which direction an order is placed in: buying or selling.
 *
 * <p>Enums demonstrate a type-safe fixed set of constants (a course topic
 * under data types / classes). Each constant carries a human-readable label.</p>
 */
public enum OrderSide {
    BUY("Buy"),
    SELL("Sell");

    private final String label;

    OrderSide(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** @return the opposite side (BUY &lt;-&gt; SELL); used by the matching engine. */
    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}
