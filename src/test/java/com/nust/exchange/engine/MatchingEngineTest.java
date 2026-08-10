package com.nust.exchange.engine;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.model.InstitutionalTrader;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.MarketOrder;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;
import com.nust.exchange.model.Trader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the matching engine via the Exchange facade. */
class MatchingEngineTest {

    private Exchange ex;

    @BeforeEach
    void setUp() {
        ex = Exchange.getInstance();
        ex.reset();
    }

    private Trader trader(String id, double cash, String sym, int shares, double px) throws Exception {
        Trader t = new InstitutionalTrader(id, id, "pw");
        double cur = t.getPortfolio().getCashBalance();
        if (cash > cur) {
            t.getPortfolio().deposit(cash - cur);
        } else if (cash < cur) {
            t.getPortfolio().withdraw(cur - cash);
        }
        if (shares > 0) {
            t.getPortfolio().addHolding(sym, shares, px);
        }
        ex.registerTrader(t);
        return t;
    }

    @Test
    void crossingLimitOrdersExecuteAtRestingPrice() throws Exception {
        Stock s = new Stock("AAPL", "Apple", 100.0);
        ex.listStock(s);
        Trader seller = trader("S1", 0, "AAPL", 100, 90.0);
        Trader buyer = trader("B1", 100_000, "AAPL", 0, 0);

        ex.placeOrder(new LimitOrder(seller, s, OrderSide.SELL, 100, 100.0));
        List<Trade> trades = ex.placeOrder(new LimitOrder(buyer, s, OrderSide.BUY, 100, 101.0));

        assertEquals(1, trades.size());
        assertEquals(100.0, trades.get(0).getPrice(), 1e-9);
        assertEquals(100, buyer.getPortfolio().getHolding("AAPL").getQuantity());
        assertNull(seller.getPortfolio().getHolding("AAPL"));
    }

    @Test
    void largeOrderPartiallyFillsAndRests() throws Exception {
        Stock s = new Stock("TSLA", "Tesla", 200.0);
        ex.listStock(s);
        Trader seller = trader("S2", 0, "TSLA", 30, 150.0);
        Trader buyer = trader("B2", 1_000_000, "TSLA", 0, 0);

        ex.placeOrder(new LimitOrder(seller, s, OrderSide.SELL, 30, 200.0));
        List<Trade> trades = ex.placeOrder(new LimitOrder(buyer, s, OrderSide.BUY, 100, 200.0));

        assertEquals(30, trades.get(0).getQuantity());
        assertEquals(200.0, ex.getOrderBook("TSLA").getBestBid(), 1e-9);
        assertNull(ex.getOrderBook("TSLA").getBestAsk());
    }

    @Test
    void lowestAskIsMatchedFirst() throws Exception {
        Stock s = new Stock("NVDA", "Nvidia", 500.0);
        ex.listStock(s);
        Trader high = trader("HIGH", 0, "NVDA", 10, 400.0);
        Trader low = trader("LOW", 0, "NVDA", 10, 400.0);
        Trader buyer = trader("BB", 1_000_000, "NVDA", 0, 0);

        ex.placeOrder(new LimitOrder(high, s, OrderSide.SELL, 10, 505.0));
        ex.placeOrder(new LimitOrder(low, s, OrderSide.SELL, 10, 500.0));
        List<Trade> trades = ex.placeOrder(new LimitOrder(buyer, s, OrderSide.BUY, 10, 510.0));

        assertEquals(500.0, trades.get(0).getPrice(), 1e-9);
        assertEquals("LOW", trades.get(0).getSellerId());
    }

    @Test
    void marketOrderFillsAgainstBook() throws Exception {
        Stock s = new Stock("AMD", "AMD", 100.0);
        ex.listStock(s);
        Trader seller = trader("MS", 0, "AMD", 50, 90.0);
        Trader buyer = trader("MB", 1_000_000, "AMD", 0, 0);

        ex.placeOrder(new LimitOrder(seller, s, OrderSide.SELL, 50, 105.0));
        List<Trade> trades = ex.placeOrder(new MarketOrder(buyer, s, OrderSide.BUY, 50));

        assertEquals(105.0, trades.get(0).getPrice(), 1e-9);
        assertEquals(50, buyer.getPortfolio().getHolding("AMD").getQuantity());
    }

    @Test
    void cashAndSharesAreConservedAcrossManyTrades() throws Exception {
        Stock s = new Stock("CONS", "Conserve", 100.0);
        ex.listStock(s);
        Trader a = trader("CA", 1_000_000, "CONS", 1000, 100.0);
        Trader b = trader("CB", 1_000_000, "CONS", 1000, 100.0);

        double cash0 = a.getPortfolio().getCashBalance() + b.getPortfolio().getCashBalance();
        int shares0 = a.getPortfolio().getHolding("CONS").getQuantity()
                + b.getPortfolio().getHolding("CONS").getQuantity();

        Random r = new Random(42);
        for (int i = 0; i < 200; i++) {
            Trader x = r.nextBoolean() ? a : b;
            Trader y = (x == a) ? b : a;
            int q = 1 + r.nextInt(20);
            double p = 90 + r.nextInt(20);
            try {
                ex.placeOrder(new LimitOrder(x, s, OrderSide.SELL, q, p));
                ex.placeOrder(new LimitOrder(y, s, OrderSide.BUY, q, p));
            } catch (Exception ignored) {
            }
        }

        double cash1 = a.getPortfolio().getCashBalance() + b.getPortfolio().getCashBalance();
        int shares1 = a.getPortfolio().getHolding("CONS").getQuantity()
                + b.getPortfolio().getHolding("CONS").getQuantity();

        assertEquals(cash0, cash1, 1e-6, "total cash must be conserved");
        assertEquals(shares0, shares1, "total shares must be conserved");
    }
}
