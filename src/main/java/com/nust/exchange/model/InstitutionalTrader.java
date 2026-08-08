package com.nust.exchange.model;

/**
 * A large institutional trader: lower commission, much larger order limits.
 * A second concrete subclass of {@link Trader}, showing how new trader types
 * slot in without changing existing code (Open-Closed Principle).
 */
public class InstitutionalTrader extends Trader {

    private static final long serialVersionUID = 1L;

    private static final double COMMISSION = 0.001; // 0.1%
    private static final int MAX_ORDER = 100_000;
    private static final double STARTING_CASH = 5_000_000.0;

    public InstitutionalTrader(String id, String name, String password) {
        super(id, name, password, STARTING_CASH);
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
        return "Institutional";
    }
}
