package com.nust.exchange.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique, thread-safe identifiers for orders and trades.
 *
 * <p>Demonstrates <b>static class members</b> (a course topic): the counters
 * belong to the class, not to any instance. Declared {@code final} so the
 * class cannot be extended - it is a pure utility holder.</p>
 *
 * <p>{@link AtomicLong} keeps ID generation correct even when several threads
 * (bot traders, the UI, the engine) request IDs at the same time.</p>
 */
public final class IdGenerator {

    private static final AtomicLong ORDER_SEQ = new AtomicLong(1000);
    private static final AtomicLong TRADE_SEQ = new AtomicLong(5000);

    private IdGenerator() {
        // Utility class - prevent instantiation.
    }

    public static String nextOrderId() {
        return "ORD-" + ORDER_SEQ.getAndIncrement();
    }

    public static String nextTradeId() {
        return "TRD-" + TRADE_SEQ.getAndIncrement();
    }
}
