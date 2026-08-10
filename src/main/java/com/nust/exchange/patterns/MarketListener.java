package com.nust.exchange.patterns;

import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;

/**
 * Observer interface for market events (the <b>Observer design pattern</b>).
 *
 * <p>Any component that wants to react to the market - the JavaFX dashboard,
 * a logging service, a bot trader - implements this interface and registers
 * with the {@code Exchange}. The exchange (the subject/observable) then pushes
 * updates to every registered listener.</p>
 *
 * <p>All methods are {@code default} so an implementer only overrides the
 * events it cares about (demonstrates interface default methods).</p>
 */
public interface MarketListener {

    /** Fired when a stock's traded price changes. */
    default void onPriceUpdate(Stock stock) {
    }

    /** Fired when two orders are matched into a trade. */
    default void onTrade(Trade trade) {
    }

    /** Fired when a symbol's order book changes (order added/removed/filled). */
    default void onOrderBookChanged(String symbol) {
    }
}
