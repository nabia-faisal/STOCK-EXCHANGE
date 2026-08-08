package com.nust.exchange.model;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderType;
import com.nust.exchange.util.Money;

/**
 * An order to trade only at a specified price or better.
 *
 * <p>Adds its own {@code limitPrice} state on top of the inherited fields and
 * overrides {@link #acceptsPrice(double)} with price-aware logic - a clear
 * example of <b>polymorphism</b>: the engine calls the same method name on a
 * {@code MarketOrder} and a {@code LimitOrder} and gets different behaviour.</p>
 */
public class LimitOrder extends Order {

    private static final long serialVersionUID = 1L;

    private final double limitPrice;

    public LimitOrder(Trader owner, Stock stock, OrderSide side, int quantity, double limitPrice) {
        super(owner, stock, side, quantity); // chain to base constructor
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Limit price must be positive");
        }
        this.limitPrice = Money.round(limitPrice);
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    @Override
    public OrderType getType() {
        return OrderType.LIMIT;
    }

    /**
     * A buy limit accepts prices at or below its limit; a sell limit accepts
     * prices at or above its limit.
     */
    @Override
    public boolean acceptsPrice(double price) {
        return isBuy() ? price <= limitPrice : price >= limitPrice;
    }

    @Override
    public String toString() {
        return super.toString() + " @ " + Money.format(limitPrice);
    }
}
