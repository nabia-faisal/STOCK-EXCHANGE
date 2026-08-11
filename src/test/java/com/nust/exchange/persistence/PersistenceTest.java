package com.nust.exchange.persistence;

import com.nust.exchange.engine.Exchange;
import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.model.InstitutionalTrader;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.RetailTrader;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Round-trip save/load tests for the persistence layer. */
class PersistenceTest {

    @Test
    void savesAndReloadsFullState(@TempDir Path dir) throws Exception {
        DataStore store = new DataStore(dir.toString());
        Exchange ex = Exchange.getInstance();
        ex.reset();

        Stock aapl = new Stock("AAPL", "Apple Inc.", 180.0);
        ex.listStock(aapl);
        Stock tsla = new Stock("TSLA", "Tesla Inc.", 250.0);
        ex.listStock(tsla);
        aapl.setLastPrice(190.5);

        Trader alice = new RetailTrader("alice", "Alice Khan", "pass");
        alice.getPortfolio().addHolding("AAPL", 100, 175.0);
        Trader seller = new InstitutionalTrader("seller", "Seller", "pass");
        seller.getPortfolio().addHolding("TSLA", 50, 200.0);
        Trader mega = new InstitutionalTrader("megafund", "Mega Fund", "pass");
        ex.registerTrader(alice);
        ex.registerTrader(seller);
        ex.registerTrader(mega);

        ex.addListener(store.newTransactionLogger());
        ex.placeOrder(new LimitOrder(seller, tsla, OrderSide.SELL, 50, 240.0));
        ex.placeOrder(new LimitOrder(mega, tsla, OrderSide.BUY, 50, 240.0));

        int stocks = ex.getStocks().size();
        int traders = ex.getTraders().size();
        int trades = ex.getTradeHistory().size();

        store.saveAll(ex);

        // Reload into a fresh exchange
        ex.reset();
        assertTrue(ex.getStocks().isEmpty());
        store.loadAll(ex);

        assertEquals(stocks, ex.getStocks().size());
        assertEquals(traders, ex.getTraders().size());
        assertEquals(trades, ex.getTradeHistory().size());
        assertEquals(190.5, ex.getStock("AAPL").getLastPrice(), 1e-9);

        Trader reAlice = ex.getTrader("alice");
        assertNotNull(reAlice);
        assertTrue(reAlice instanceof RetailTrader, "subclass type preserved");
        assertEquals(100, reAlice.getPortfolio().getHolding("AAPL").getQuantity());
        assertTrue(reAlice.verifyPassword("pass"), "password survives serialization");
        assertEquals(50, ex.getTrader("megafund").getPortfolio().getHolding("TSLA").getQuantity());
    }

    @Test
    void missingFilesSeedDefaultsInsteadOfCrashing(@TempDir Path dir) {
        DataStore store = new DataStore(dir.toString());
        Exchange ex = Exchange.getInstance();
        ex.reset();

        assertDoesNotThrow(() -> store.loadAll(ex));
        assertFalse(ex.getStocks().isEmpty(), "default market seeded");
        assertNotNull(ex.getTrader("admin"), "default admin seeded");
    }
}
