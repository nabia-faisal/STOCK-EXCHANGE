package com.nust.exchange.model;

import com.nust.exchange.enums.AssetType;
import com.nust.exchange.util.Money;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A tradable instrument (e.g. a company's stock).
 *
 * <p>Demonstrates several course topics:</p>
 * <ul>
 *   <li><b>Encapsulation / data hiding</b>: all fields are {@code private} and
 *       reached only through validated getters/setters.</li>
 *   <li><b>Constructors</b>: a full constructor, a convenience overload, and a
 *       <b>copy constructor</b> {@link #Stock(Stock)}.</li>
 *   <li><b>{@code this} reference</b>: used to disambiguate fields from
 *       parameters.</li>
 *   <li><b>Serialization</b>: implements {@link Serializable} so full state can
 *       be written with {@code ObjectOutputStream}.</li>
 * </ul>
 */
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String symbol;      // immutable ticker, e.g. "AAPL"
    private String name;              // company name
    private AssetType type;
    private final double openPrice;   // reference price at session open
    private volatile double lastPrice; // updated by the price-simulator thread
    private final List<Double> priceHistory;

    /** Full constructor. */
    public Stock(String symbol, String name, AssetType type, double openPrice) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol cannot be empty");
        }
        if (openPrice <= 0) {
            throw new IllegalArgumentException("Opening price must be positive");
        }
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.type = type;
        this.openPrice = openPrice;
        this.lastPrice = openPrice;
        this.priceHistory = new ArrayList<>();
        this.priceHistory.add(openPrice);
    }

    /** Convenience overload: defaults the asset type to {@link AssetType#STOCK}. */
    public Stock(String symbol, String name, double openPrice) {
        this(symbol, name, AssetType.STOCK, openPrice);
    }

    /**
     * Copy constructor - produces an independent deep copy (course topic).
     * Useful for handing the UI a price snapshot without exposing internal state.
     */
    public Stock(Stock other) {
        this.symbol = other.symbol;
        this.name = other.name;
        this.type = other.type;
        this.openPrice = other.openPrice;
        this.lastPrice = other.lastPrice;
        this.priceHistory = new ArrayList<>(other.priceHistory);
    }

    // --- Getters / setters (encapsulation) ---

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    /**
     * Update the traded price and record it in the history.
     *
     * <p>{@code synchronized} because both the matching-engine thread (on a
     * trade) and the price-simulator thread write the price - this serialises
     * the writes and keeps {@code priceHistory} from being corrupted.</p>
     */
    public synchronized void setLastPrice(double newPrice) {
        if (newPrice <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.lastPrice = Money.round(newPrice);
        this.priceHistory.add(this.lastPrice);
    }

    /** @return the percentage change since the session open. */
    public double getChangePercent() {
        return ((lastPrice - openPrice) / openPrice) * 100.0;
    }

    /** @return a defensive copy of the price history (safe to iterate). */
    public synchronized List<Double> getPriceHistory() {
        return new ArrayList<>(priceHistory);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %s  %+.2f%%",
                symbol, name, Money.format(lastPrice), getChangePercent());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Stock)) {
            return false;
        }
        return symbol.equals(((Stock) o).symbol);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }
}
