package com.nust.exchange.engine;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.model.Holding;
import com.nust.exchange.model.InstitutionalTrader;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency invariant test: many producer threads submit orders at once while
 * the price simulator runs; cash and shares must be perfectly conserved and the
 * queue must fully drain - i.e. no race conditions or lost updates.
 */
class ConcurrencyTest {

    private static final String SYM = "STRESS";

    private Trader newTrader(Exchange ex, String id) throws Exception {
        Trader t = new InstitutionalTrader(id, id, "pw");
        double currentCash = t.getPortfolio().getCashBalance();
        if (currentCash < 1_000_000) {
            t.getPortfolio().deposit(1_000_000 - currentCash);
        } else if (currentCash > 1_000_000) {
            try { t.getPortfolio().withdraw(currentCash - 1_000_000); } catch (Exception ignored) {}
        }
        t.getPortfolio().addHolding(SYM, 2_000, 100.0);
        ex.registerTrader(t);
        return t;
    }

    private int totalShares(List<Trader> traders) {
        int s = 0;
        for (Trader t : traders) {
            Holding h = t.getPortfolio().getHolding(SYM);
            if (h != null) {
                s += h.getQuantity();
            }
        }
        return s;
    }

    private double totalCash(List<Trader> traders) {
        double c = 0;
        for (Trader t : traders) {
            c += t.getPortfolio().getCashBalance();
        }
        return c;
    }

    @Test
    void invariantsHoldUnderConcurrentLoad() throws Exception {
        Exchange ex = Exchange.getInstance();
        ex.reset();
        Stock stock = new Stock(SYM, "StressCo", 100.0);
        ex.listStock(stock);

        List<Trader> traders = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            traders.add(newTrader(ex, "T" + i));
        }
        double cash0 = totalCash(traders);
        int shares0 = totalShares(traders);

        ex.startEngine();
        PriceSimulator sim = new PriceSimulator(ex, 5, 0.01);
        sim.start();

        int producers = 6;
        int ordersEach = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(producers);
        CountDownLatch done = new CountDownLatch(producers);

        for (int p = 0; p < producers; p++) {
            final Trader trader = traders.get(p);
            pool.submit(() -> {
                Random r = new Random();
                for (int i = 0; i < ordersEach; i++) {
                    try {
                        OrderSide side = r.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
                        int qty = 1 + r.nextInt(20);
                        double px = Math.max(1, stock.getLastPrice() * (1 + (r.nextDouble() * 0.04 - 0.02)));
                        Order o = new LimitOrder(trader, stock, side, qty, px);
                        ex.submitOrder(o);
                    } catch (Exception ignored) {
                    }
                }
                done.countDown();
            });
        }

        assertTrue(done.await(60, TimeUnit.SECONDS), "producers finished");
        pool.shutdown();

        long deadline = System.currentTimeMillis() + 30_000;
        while (!ex.isQueueDrained() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        sim.stop();
        ex.stopEngine();

        assertTrue(ex.isQueueDrained(), "engine drained all orders");
        assertEquals(ex.getSubmittedCount(), ex.getProcessedCount(), "processed == submitted");
        assertEquals(cash0, totalCash(traders), 1e-3, "total cash conserved");
        assertEquals(shares0, totalShares(traders), "total shares conserved");
        assertTrue(ex.getTradeHistory().size() > 0, "trades occurred");
    }
}
