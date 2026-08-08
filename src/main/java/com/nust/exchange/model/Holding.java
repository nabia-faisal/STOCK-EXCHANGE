package com.nust.exchange.model;

import com.nust.exchange.util.Money;

import java.io.Serializable;

/**
 * A single position: how many shares of one symbol a trader owns and at what
 * average cost. {@code Holding} objects are <b>composed</b> inside a
 * {@link Portfolio} (a HAS-A relationship - a course topic).
 */
public class Holding implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String symbol;
    private int quantity;
    private double averageCost;

    public Holding(String symbol, int quantity, double averageCost) {
        this.symbol = symbol.toUpperCase();
        this.quantity = quantity;
        this.averageCost = averageCost;
    }

    /** Add shares, recomputing the weighted average cost. */
    public void addShares(int qty, double price) {
        double totalCost = (averageCost * quantity) + (price * qty);
        this.quantity += qty;
        this.averageCost = quantity == 0 ? 0 : Money.round(totalCost / quantity);
    }

    /** Remove shares (average cost is unchanged when selling). */
    public void removeShares(int qty) {
        if (qty > quantity) {
            throw new IllegalArgumentException("Cannot remove more shares than held");
        }
        this.quantity -= qty;
    }

    public double getMarketValue(double currentPrice) {
        return Money.round(quantity * currentPrice);
    }

    public double getUnrealizedPnL(double currentPrice) {
        return Money.round((currentPrice - averageCost) * quantity);
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAverageCost() {
        return averageCost;
    }

    @Override
    public String toString() {
        return String.format("%s x%d @ avg %s", symbol, quantity, Money.format(averageCost));
    }
}
