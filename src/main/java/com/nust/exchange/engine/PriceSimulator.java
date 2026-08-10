package com.nust.exchange.engine;

import com.nust.exchange.model.Stock;

import java.util.List;
import java.util.Random;

/**
 * Background thread that keeps the market "alive" by nudging every stock's
 * price with a small random walk at a fixed interval.
 *
 * <p>Demonstrates a long-running worker thread that shares data (the
 * {@link Stock} prices) with the matching-engine thread. Because
 * {@link Stock#setLastPrice(double)} is {@code synchronized}, the simulator and
 * the engine can never corrupt the price history even when they update the same
 * stock at the same moment.</p>
 *
 * <p>Implements {@link Runnable} rather than extending {@code Thread} so the
 * simulation logic is decoupled from thread management (composition over
 * inheritance).</p>
 */
public class PriceSimulator implements Runnable {

    private final Exchange exchange;
    private final long intervalMillis;
    private final double volatility;   // max fractional move per tick, e.g. 0.02 = 2%
    private final Random random = new Random();

    private volatile boolean running = false;
    private Thread thread;

    public PriceSimulator(Exchange exchange, long intervalMillis, double volatility) {
        this.exchange = exchange;
        this.intervalMillis = intervalMillis;
        this.volatility = volatility;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "price-simulator");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                tick();
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Apply one random-walk step to every listed stock. */
    private void tick() {
        List<Stock> stocks = exchange.getStocks();
        for (Stock stock : stocks) {
            double move = (random.nextDouble() * 2 - 1) * volatility; // in [-vol, +vol]
            double newPrice = stock.getLastPrice() * (1 + move);
            newPrice = Math.max(0.01, newPrice);
            stock.setLastPrice(newPrice);
            exchange.notifyPriceUpdate(stock);
        }
    }
}
