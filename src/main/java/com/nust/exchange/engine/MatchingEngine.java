package com.nust.exchange.engine;

import com.nust.exchange.model.Holding;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;
import com.nust.exchange.model.Trader;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The core matching algorithm.
 *
 * <p>An incoming order is matched against the opposite side of the book while
 * the prices cross. Each match executes at the <b>resting order's price</b>
 * (standard price-time-priority behaviour: the passive order sets the price),
 * supports <b>partial fills</b>, settles both portfolios, updates the stock's
 * last traded price, and produces a {@link Trade} record.</p>
 *
 * <p>Any unfilled remainder of a limit order rests in the book; an unfilled
 * remainder of a market order is cancelled (no resting price).</p>
 *
 * <p>The whole match runs inside {@code synchronized (book)} so no other thread
 * can mutate the same book concurrently - this is what guarantees no race
 * conditions or corrupted books under concurrent bot trading.</p>
 */
public class MatchingEngine {

    /**
     * Match {@code incoming} against {@code book}.
     *
     * @return the list of trades generated (possibly empty)
     */
    public List<Trade> match(Order incoming, OrderBook book) {
        List<Trade> trades = new ArrayList<>();
        Stock stock = incoming.getStock();

        synchronized (book) {
            PriorityQueue<LimitOrder> opposite = book.oppositeSide(incoming.isBuy());

            while (incoming.isActive() && !opposite.isEmpty()) {
                LimitOrder resting = opposite.peek();

                // Skip orders that were cancelled/filled while resting.
                if (!resting.isActive()) {
                    opposite.poll();
                    continue;
                }

                double tradePrice = resting.getLimitPrice();

                // Stop when the prices no longer cross.
                if (!incoming.acceptsPrice(tradePrice)) {
                    break;
                }

                int qty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());

                Trader buyer = incoming.isBuy() ? incoming.getOwner() : resting.getOwner();
                Trader seller = incoming.isBuy() ? resting.getOwner() : incoming.getOwner();

                // Pre-check funds/shares so settlement never fails mid-way.
                if (!canAfford(buyer, qty, tradePrice)) {
                    cancelBuySide(incoming, resting, opposite);
                    continue;
                }
                if (!ownsShares(seller, stock.getSymbol(), qty)) {
                    cancelSellSide(incoming, resting, opposite);
                    continue;
                }

                settle(buyer, seller, stock.getSymbol(), qty, tradePrice);
                incoming.fill(qty);
                resting.fill(qty);
                stock.setLastPrice(tradePrice);
                trades.add(new Trade(stock.getSymbol(), buyer.getId(), seller.getId(), qty, tradePrice));

                if (!resting.isActive()) {
                    opposite.poll();
                }
            }

            // Rest any unfilled remainder of a limit order; cancel a market remainder.
            if (incoming.isActive()) {
                if (incoming instanceof LimitOrder) {
                    book.add((LimitOrder) incoming);
                } else {
                    incoming.cancel();
                }
            }
        }
        return trades;
    }

    private boolean canAfford(Trader buyer, int qty, double price) {
        return buyer.getPortfolio().getCashBalance() >= qty * price;
    }

    private boolean ownsShares(Trader seller, String symbol, int qty) {
        Holding h = seller.getPortfolio().getHolding(symbol);
        return h != null && h.getQuantity() >= qty;
    }

    /** Move cash and shares between the two counterparties (pre-validated). */
    private void settle(Trader buyer, Trader seller, String symbol, int qty, double price) {
        try {
            buyer.getPortfolio().buy(symbol, qty, price);
            seller.getPortfolio().sell(symbol, qty, price);
        } catch (Exception e) {
            // Pre-checks guarantee this cannot happen; fail loudly if it ever does.
            throw new IllegalStateException("Settlement failed after validation", e);
        }
    }

    private void cancelBuySide(Order incoming, LimitOrder resting, PriorityQueue<LimitOrder> opposite) {
        if (incoming.isBuy()) {
            incoming.cancel();
        } else {
            resting.cancel();
            opposite.poll();
        }
    }

    private void cancelSellSide(Order incoming, LimitOrder resting, PriorityQueue<LimitOrder> opposite) {
        if (!incoming.isBuy()) {
            incoming.cancel();
        } else {
            resting.cancel();
            opposite.poll();
        }
    }
}
