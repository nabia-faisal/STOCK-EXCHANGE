package com.nust.exchange.util;

/**
 * Small helper for money/price formatting and rounding.
 *
 * <p>Demonstrates <b>static helper methods</b> and <b>method overloading</b>
 * (two {@code format} methods with different parameter lists - a course
 * topic). Kept {@code final}; not instantiable.</p>
 */
public final class Money {

    private static final String CURRENCY = "$";

    private Money() {
        // Utility class.
    }

    /** Round a raw amount to 2 decimal places (cents). */
    public static double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    /** Overload #1: format using the default currency symbol. */
    public static String format(double amount) {
        return format(amount, CURRENCY);
    }

    /** Overload #2: format using a caller-supplied currency symbol. */
    public static String format(double amount, String symbol) {
        return String.format("%s%,.2f", symbol, amount);
    }
}
