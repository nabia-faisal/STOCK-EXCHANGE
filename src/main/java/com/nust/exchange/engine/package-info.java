/**
 * Exchange engine and concurrency.
 *
 * <p>Holds the {@code Exchange} singleton, the {@code OrderBook} (dual
 * priority queues for bids/asks), the {@code MatchingEngine} (price-time
 * priority matching with partial fills), and the background threads:
 * {@code PriceSimulator} and {@code BotTrader}. Shared state here is guarded
 * with locks and concurrent collections.</p>
 */
package com.nust.exchange.engine;
