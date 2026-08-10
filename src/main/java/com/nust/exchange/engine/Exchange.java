package com.nust.exchange.engine;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.exceptions.InsufficientFundsException;
import com.nust.exchange.exceptions.InsufficientSharesException;
import com.nust.exchange.exceptions.InvalidOrderException;
import com.nust.exchange.model.Holding;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;
import com.nust.exchange.model.Trader;
import com.nust.exchange.patterns.MarketListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The central controller of the whole system - implemented as a <b>Singleton</b>
 * so there is exactly one source of truth for stocks, order books, traders, and
 * the trade history.
 *
 * <p>Also acts as the <b>Observable</b> subject in the Observer pattern:
 * components register as {@link MarketListener}s and are notified of price
 * updates and trades.</p>
 *
 * <p>Uses thread-safe collections ({@link ConcurrentHashMap},
 * {@link CopyOnWriteArrayList}) because the engine thread, bot threads, and the
 * UI thread all touch this state.</p>
 */
public final class Exchange {

    // --- Singleton (thread-safe, lazy via holder idiom) ---
    private Exchange() {
    }

    private static final class Holder {
        private static final Exchange INSTANCE = new Exchange();
    }

    public static Exchange getInstance() {
        return Holder.INSTANCE;
    }

    // --- State ---
    private final Map<String, Stock> stocks = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<String, Trader> traders = new ConcurrentHashMap<>();
    private final List<Trade> tradeHistory = new CopyOnWriteArrayList<>();
    private final List<MarketListener> listeners = new CopyOnWriteArrayList<>();
    private final MatchingEngine engine = new MatchingEngine();

    // --- Async order pipeline (concurrency) ---
    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
    private final AtomicLong submittedCount = new AtomicLong();
    private final AtomicLong processedCount = new AtomicLong();
    private volatile boolean engineRunning = false;
    private Thread engineThread;

    // --- Market administration ---

    /** List a new instrument for trading (creates its order book). */
    public void listStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
        books.putIfAbsent(stock.getSymbol(), new OrderBook(stock.getSymbol()));
        notifyPriceUpdate(stock);
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol.toUpperCase());
    }

    public List<Stock> getStocks() {
        return new ArrayList<>(stocks.values());
    }

    public OrderBook getOrderBook(String symbol) {
        return books.get(symbol.toUpperCase());
    }

    // --- Traders / authentication ---

    public void registerTrader(Trader trader) {
        traders.put(trader.getId(), trader);
    }

    public Trader getTrader(String id) {
        return traders.get(id);
    }

    public List<Trader> getTraders() {
        return new ArrayList<>(traders.values());
    }

    /** @return the trader if id exists and the password matches, else null. */
    public Trader authenticate(String id, String password) {
        Trader t = traders.get(id);
        return (t != null && t.verifyPassword(password)) ? t : null;
    }

    // --- Order placement ---

    /**
     * Validate an order against the trader's account, then run it through the
     * matching engine. Returns the trades it generated.
     *
     * @throws InvalidOrderException       book missing / order malformed
     * @throws InsufficientFundsException  buyer cannot cover the order
     * @throws InsufficientSharesException seller does not own the shares
     */
    public List<Trade> placeOrder(Order order)
            throws InvalidOrderException, InsufficientFundsException, InsufficientSharesException {

        OrderBook book = books.get(order.getStock().getSymbol());
        if (book == null) {
            throw new InvalidOrderException("No market for " + order.getStock().getSymbol());
        }
        validateAffordability(order);
        return process(order, book);
    }

    /**
     * Core processing shared by the synchronous and asynchronous paths: run the
     * order through the matching engine, record trades, and notify listeners.
     */
    private List<Trade> process(Order order, OrderBook book) {
        List<Trade> trades = engine.match(order, book);
        for (Trade trade : trades) {
            tradeHistory.add(trade);
            notifyTrade(trade);
        }
        if (!trades.isEmpty()) {
            notifyPriceUpdate(order.getStock());
        }
        notifyOrderBookChanged(order.getStock().getSymbol());
        processedCount.incrementAndGet();
        return trades;
    }

    // --- Asynchronous submission (used by bots and the live UI) ---

    /**
     * Validate an order and hand it to the matching-engine thread via the
     * {@link BlockingQueue}. Returns immediately; results arrive through
     * {@link MarketListener} callbacks.
     */
    public void submitOrder(Order order)
            throws InvalidOrderException, InsufficientFundsException, InsufficientSharesException {

        if (books.get(order.getStock().getSymbol()) == null) {
            throw new InvalidOrderException("No market for " + order.getStock().getSymbol());
        }
        validateAffordability(order);
        submittedCount.incrementAndGet();
        try {
            orderQueue.put(order); // blocks only if the queue is full (unbounded here)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidOrderException("Order submission interrupted");
        }
    }

    /**
     * Start the single matching-engine consumer thread. Using exactly one
     * consumer means all matching and settlement is serialised - the key design
     * decision that makes the engine race-free under concurrent submissions.
     */
    public synchronized void startEngine() {
        if (engineRunning) {
            return;
        }
        engineRunning = true;
        engineThread = new Thread(this::engineLoop, "matching-engine");
        engineThread.setDaemon(true);
        engineThread.start();
    }

    public synchronized void stopEngine() {
        engineRunning = false;
        if (engineThread != null) {
            engineThread.interrupt();
        }
    }

    private void engineLoop() {
        while (engineRunning) {
            try {
                Order order = orderQueue.take(); // blocks until an order arrives
                OrderBook book = books.get(order.getStock().getSymbol());
                if (book != null) {
                    process(order, book);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                // A single bad order must never kill the engine thread.
                System.err.println("Engine skipped an order: " + e.getMessage());
                processedCount.incrementAndGet();
            }
        }
    }

    /** @return true once every submitted order has been processed. */
    public boolean isQueueDrained() {
        return processedCount.get() >= submittedCount.get() && orderQueue.isEmpty();
    }

    public long getSubmittedCount() {
        return submittedCount.get();
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    /** Reject orders the trader clearly cannot honour before matching. */
    private void validateAffordability(Order order)
            throws InsufficientFundsException, InsufficientSharesException {

        Trader trader = order.getOwner();
        String symbol = order.getStock().getSymbol();

        if (order.getSide() == OrderSide.BUY) {
            double reference = order.getStock().getLastPrice();
            double estimatedCost = order.getQuantity() * reference;
            double cash = trader.getPortfolio().getCashBalance();
            if (estimatedCost > cash) {
                throw new InsufficientFundsException(estimatedCost, cash);
            }
        } else {
            Holding h = trader.getPortfolio().getHolding(symbol);
            int owned = h == null ? 0 : h.getQuantity();
            if (order.getQuantity() > owned) {
                throw new InsufficientSharesException(symbol, order.getQuantity(), owned);
            }
        }
    }

    // --- Trade history ---

    public List<Trade> getTradeHistory() {
        return new ArrayList<>(tradeHistory);
    }

    /** Replace the trade history (used when loading persisted state). */
    public void restoreTradeHistory(List<Trade> trades) {
        tradeHistory.clear();
        tradeHistory.addAll(trades);
    }

    /** @return a symbol -> last price map for portfolio valuation. */
    public Map<String, Double> priceLookup() {
        Map<String, Double> map = new HashMap<>();
        for (Stock s : stocks.values()) {
            map.put(s.getSymbol(), s.getLastPrice());
        }
        return map;
    }

    // --- Observer (Observable) support ---

    public void addListener(MarketListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MarketListener listener) {
        listeners.remove(listener);
    }

    public void notifyPriceUpdate(Stock stock) {
        for (MarketListener l : listeners) {
            l.onPriceUpdate(stock);
        }
    }

    public void notifyTrade(Trade trade) {
        for (MarketListener l : listeners) {
            l.onTrade(trade);
        }
    }

    public void notifyOrderBookChanged(String symbol) {
        for (MarketListener l : listeners) {
            l.onOrderBookChanged(symbol);
        }
    }

    /** Test/utility hook: stop the engine and clear all state. */
    public void reset() {
        stopEngine();
        orderQueue.clear();
        submittedCount.set(0);
        processedCount.set(0);
        stocks.clear();
        books.clear();
        traders.clear();
        tradeHistory.clear();
        listeners.clear();
    }
}
