package com.nust.exchange.enums;

/**
 * The category of a tradable instrument. Allows the exchange to be extended
 * beyond equities without changing existing code (Open-Closed Principle).
 */
public enum AssetType {
    STOCK("Stock"),
    ETF("ETF"),
    BOND("Bond");

    private final String label;

    AssetType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
