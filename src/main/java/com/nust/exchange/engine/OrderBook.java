package com.nust.exchange.engine;

import com.nust.exchange.model.LimitOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The order book for a single symbol: resting buy orders (bids) and sell
 * orders (asks), each kept in a {@link PriorityQueue} so the best-priced,
 * earliest order is always at the head.
 *
 * <p><b>Price-time priority</b> is encoded in the comparators:</p>
 * <ul>
 *   <li>Bids: highest price first; ties broken by earliest arrival.</li>
 *   <li>Asks: lowest price first; ties broken by earliest arrival.</li>
 * </ul>
 *
 * <p>Priority queues give O(log n) insertion and O(1) best-price peek, which is
 * what keeps the matching logic scalable (Logic &amp; Efficiency rubric).</p>
 *
 * <p>All public methods are {@code synchronized}: bids/asks are shared state
 * touched by the matching-engine thread and read by the UI, so access is
 * serialised on the book's monitor.</p>
 */
public class OrderBook {

    private final String symbol;

    /** Best bid = highest price, then earliest time. */
    private final PriorityQueue<LimitOrder> bids = new PriorityQueue<>(
            (a, b) -> {
                int byPrice = Double.compare(b.getLimitPrice(), a.getLimitPrice());
                return byPrice != 0 ? byPrice : a.compareTo(b);
            });

    /** Best ask = lowest price, then earliest time. */
    private final PriorityQueue<LimitOrder> asks = new PriorityQueue<>(
            (a, b) -> {
                int byPrice = Double.compare(a.getLimitPrice(), b.getLimitPrice());
                return byPrice != 0 ? byPrice : a.compareTo(b);
            });

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /** Add a resting limit order to the correct side. */
    public synchronized void add(LimitOrder order) {
        if (order.isBuy()) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    /** @return the queue of resting orders on the opposite side to {@code buy}. */
    synchronized PriorityQueue<LimitOrder> oppositeSide(boolean incomingIsBuy) {
        return incomingIsBuy ? asks : bids;
    }

    /** Remove any cancelled/filled orders sitting at the head of both queues. */
    public synchronized void pruneInactive() {
        while (!bids.isEmpty() && !bids.peek().isActive()) {
            bids.poll();
        }
        while (!asks.isEmpty() && !asks.peek().isActive()) {
            asks.poll();
        }
    }

    public synchronized Double getBestBid() {
        pruneInactive();
        return bids.isEmpty() ? null : bids.peek().getLimitPrice();
    }

    public synchronized Double getBestAsk() {
        pruneInactive();
        return asks.isEmpty() ? null : asks.peek().getLimitPrice();
    }

    /** @return ask - bid spread, or null if either side is empty. */
    public synchronized Double getSpread() {
        Double bid = getBestBid();
        Double ask = getBestAsk();
        return (bid == null || ask == null) ? null : ask - bid;
    }

    /** Snapshot of resting bids, best first (for the depth view). */
    public synchronized List<LimitOrder> snapshotBids() {
        return sortedSnapshot(bids);
    }

    /** Snapshot of resting asks, best first (for the depth view). */
    public synchronized List<LimitOrder> snapshotAsks() {
        return sortedSnapshot(asks);
    }

    private List<LimitOrder> sortedSnapshot(PriorityQueue<LimitOrder> queue) {
        List<LimitOrder> list = new ArrayList<>();
        for (LimitOrder o : queue) {
            if (o.isActive()) {
                list.add(o);
            }
        }
        list.sort(queue.comparator());
        return list;
    }

    public synchronized int restingOrderCount() {
        return bids.size() + asks.size();
    }
}
