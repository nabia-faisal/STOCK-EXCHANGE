package com.nust.exchange.engine;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;
import com.nust.exchange.persistence.DataStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SellersTableTest {

    @Test
    void verifySellersTablePopulatedWithHoldingsAndTraderNames() throws Exception {
        Exchange ex = Exchange.getInstance();
        ex.reset();

        // 1. Load default market & traders via DataStore
        DataStore ds = new DataStore("target/test-data-" + System.currentTimeMillis());
        ds.loadAll(ex);

        // 2. Start the engine
        ex.startEngine();

        // Verify default traders exist and have shares
        Trader alice = ex.getTrader("alice");
        assertNotNull(alice, "Alice trader should exist");
        assertTrue(alice.getPortfolio().getHoldings().containsKey("AAPL"), "Alice should own AAPL shares");
        assertTrue(alice.getPortfolio().getHolding("AAPL").getQuantity() > 0, "Alice should have >0 AAPL shares");

        Trader bilal = ex.getTrader("bilal");
        assertNotNull(bilal, "Bilal trader should exist");

        Stock aapl = ex.getStock("AAPL");
        assertNotNull(aapl, "AAPL stock should exist");

        // 3. Alice places a SELL order for AAPL at $300 (above market price so it stays as an Ask in the book)
        Order sellOrder = new LimitOrder(alice, aapl, OrderSide.SELL, 10, 300.0);
        ex.submitOrder(sellOrder);

        // 4. Wait for matching engine to process the order
        long deadline = System.currentTimeMillis() + 5000;
        while (!ex.isQueueDrained() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        ex.stopEngine();

        // 5. Check the order book for AAPL
        OrderBook book = ex.getOrderBook("AAPL");
        assertNotNull(book, "Order book for AAPL should exist");

        List<LimitOrder> asks = book.snapshotAsks();
        assertFalse(asks.isEmpty(), "Asks (sellers table) MUST NOT be empty when a seller places a sell order");

        LimitOrder topAsk = asks.get(0);
        assertEquals("alice", topAsk.getOwner().getId(), "The seller trader ID in the asks table MUST be 'alice'");
        assertEquals(300.0, topAsk.getLimitPrice(), "Limit price in asks table should match");
        assertEquals(10, topAsk.getRemainingQuantity(), "Remaining quantity should match");

        System.out.println("TEST PASSED: Sellers table successfully contains seller '" + topAsk.getOwner().getId() + "' with limit price $" + topAsk.getLimitPrice());
    }
}
