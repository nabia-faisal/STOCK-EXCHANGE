package com.nust.exchange.model;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderType;

/**
 * An order to trade immediately at the best available price.
 *
 * <p>Demonstrates <b>inheritance</b> and <b>constructor chaining</b> (the
 * constructor delegates to {@code super(...)}), plus <b>method overriding</b>
 * of the two abstract methods declared in {@link Order}.</p>
 */
public class MarketOrder extends Order {

    private static final long serialVersionUID = 1L;

    public MarketOrder(Trader owner, Stock stock, OrderSide side, int quantity) {
        super(owner, stock, side, quantity); // constructor chaining to the base class
    }

    @Override
    public OrderType getType() {
        return OrderType.MARKET;
    }

    /** A market order accepts any execution price. */
    @Override
    public boolean acceptsPrice(double price) {
        return true;
    }
}
