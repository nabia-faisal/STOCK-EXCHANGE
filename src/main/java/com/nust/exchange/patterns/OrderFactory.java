package com.nust.exchange.patterns;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderType;
import com.nust.exchange.exceptions.InvalidOrderException;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.MarketOrder;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;

/**
 * Factory for creating validated {@link Order} objects (the <b>Factory design
 * pattern</b>).
 *
 * <p>Centralises order construction and validation so the rest of the system
 * never builds orders by hand. Adding a new order type later means changing
 * only this factory - callers are unaffected (Open-Closed Principle).</p>
 */
public final class OrderFactory {

    private OrderFactory() {
        // Static factory - not instantiable.
    }

    /**
     * Build an order after validating quantity, price, and the trader's own
     * order-size limit.
     *
     * @param limitPrice used only for LIMIT orders; ignored for MARKET orders
     * @throws InvalidOrderException if the request is malformed
     */
    public static Order create(Trader trader, Stock stock, OrderSide side,
                               OrderType type, int quantity, double limitPrice)
            throws InvalidOrderException {

        if (trader == null || stock == null || side == null || type == null) {
            throw new InvalidOrderException("Order fields cannot be null");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }
        if (quantity > trader.maxOrderSize()) {
            throw new InvalidOrderException(String.format(
                    "Quantity %d exceeds %s limit of %d",
                    quantity, trader.role(), trader.maxOrderSize()));
        }

        if (type == OrderType.MARKET) {
            return new MarketOrder(trader, stock, side, quantity);
        }
        if (limitPrice <= 0) {
            throw new InvalidOrderException("Limit orders require a positive price");
        }
        return new LimitOrder(trader, stock, side, quantity, limitPrice);
    }
}
