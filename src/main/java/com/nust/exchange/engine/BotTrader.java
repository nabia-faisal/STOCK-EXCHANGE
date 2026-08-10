package com.nust.exchange.engine;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderType;
import com.nust.exchange.exceptions.TradingException;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;
import com.nust.exchange.patterns.OrderFactory;

import java.util.List;
import java.util.Random;

/**
 * An automated trader that periodically submits random but valid orders,
 * creating genuine concurrent load on the order books.
 *
 * <p>Several {@code BotTrader} threads run at once, all producing into the same
 * {@link Exchange#submitOrder(Order) order queue}. This is the multi-producer /
 * single-consumer pattern: many bot threads enqueue while the single
 * matching-engine thread dequeues and matches - a clean, race-free division of
 * work.</p>
 */
public class BotTrader implements Runnable {

    private final Exchange exchange;
    private final Trader trader;
    private final long intervalMillis;
    private final Random random = new Random();

    private volatile boolean running = false;
    private Thread thread;

    public BotTrader(Exchange exchange, Trader trader, long intervalMillis) {
        this.exchange = exchange;
        this.trader = trader;
        this.intervalMillis = intervalMillis;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "bot-" + trader.getId());
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
                placeRandomOrder();
                Thread.sleep(intervalMillis + random.nextInt((int) intervalMillis + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void placeRandomOrder() {
        List<Stock> stocks = exchange.getStocks();
        if (stocks.isEmpty()) {
            return;
        }
        Stock stock = stocks.get(random.nextInt(stocks.size()));
        OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
        OrderType type = random.nextInt(4) == 0 ? OrderType.MARKET : OrderType.LIMIT;

        int quantity = 1 + random.nextInt(Math.min(50, trader.maxOrderSize()));
        // Quote a limit price within +/-3% of the current price.
        double drift = 1 + (random.nextDouble() * 0.06 - 0.03);
        double limitPrice = stock.getLastPrice() * drift;

        try {
            Order order = OrderFactory.create(trader, stock, side, type, quantity, limitPrice);
            exchange.submitOrder(order);
        } catch (TradingException e) {
            // Expected sometimes (e.g. not enough cash/shares) - just skip this tick.
        }
    }
}
