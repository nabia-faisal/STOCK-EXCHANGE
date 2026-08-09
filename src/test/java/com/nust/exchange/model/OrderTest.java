package com.nust.exchange.model;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for order polymorphism, price acceptance, and fill lifecycle. */
class OrderTest {

    private Stock stock() {
        return new Stock("AAPL", "Apple", 100.0);
    }

    private Trader trader() {
        return new RetailTrader("T1", "Ali", "pw");
    }

    @Test
    void marketOrderAcceptsAnyPrice() {
        Order o = new MarketOrder(trader(), stock(), OrderSide.BUY, 10);
        assertTrue(o.acceptsPrice(1.0));
        assertTrue(o.acceptsPrice(9_999.0));
    }

    @Test
    void buyLimitAcceptsAtOrBelowLimit() {
        Order o = new LimitOrder(trader(), stock(), OrderSide.BUY, 10, 105.0);
        assertTrue(o.acceptsPrice(105.0));
        assertTrue(o.acceptsPrice(100.0));
        assertFalse(o.acceptsPrice(110.0));
    }

    @Test
    void sellLimitAcceptsAtOrAboveLimit() {
        Order o = new LimitOrder(trader(), stock(), OrderSide.SELL, 10, 105.0);
        assertTrue(o.acceptsPrice(110.0));
        assertFalse(o.acceptsPrice(100.0));
    }

    @Test
    void earlierOrderHasTimePriority() {
        Stock s = stock();
        Trader t = trader();
        Order first = new LimitOrder(t, s, OrderSide.BUY, 10, 100.0);
        Order second = new LimitOrder(t, s, OrderSide.BUY, 10, 100.0);
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    void fillLifecycleTransitionsStatus() {
        Order o = new LimitOrder(trader(), stock(), OrderSide.BUY, 100, 105.0);
        assertEquals(OrderStatus.OPEN, o.getStatus());
        o.fill(40);
        assertEquals(OrderStatus.PARTIAL, o.getStatus());
        assertEquals(60, o.getRemainingQuantity());
        o.fill(60);
        assertEquals(OrderStatus.FILLED, o.getStatus());
        assertFalse(o.isActive());
    }
}
