package com.nust.exchange.model;

/**
 * An individual retail investor: higher commission, smaller order limits.
 * Demonstrates <b>inheritance</b> + <b>constructor chaining</b> to {@link Trader}.
 */
public class RetailTrader extends Trader {

    private static final long serialVersionUID = 1L;

    private static final double COMMISSION = 0.005; // 0.5%
    private static final int MAX_ORDER = 1_000;
    private static final double STARTING_CASH = 100_000.0;

    public RetailTrader(String id, String name, String password) {
        super(id, name, password, STARTING_CASH); // chain to base constructor
    }

    @Override
    public double commissionRate() {
        return COMMISSION;
    }

    @Override
    public int maxOrderSize() {
        return MAX_ORDER;
    }

    @Override
    public String role() {
        return "Retail";
    }
}
