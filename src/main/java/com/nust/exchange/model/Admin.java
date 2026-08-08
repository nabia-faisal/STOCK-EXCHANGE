package com.nust.exchange.model;

/**
 * An administrator who can also trade but additionally manages the market
 * (listing new stocks, viewing all traders).
 *
 * <p>Overrides {@link #canManageMarket()} to grant elevated permissions - a
 * clean example of <b>method overriding</b> changing inherited behaviour.
 * Pays no commission and has effectively no order-size cap.</p>
 */
public class Admin extends Trader {

    private static final long serialVersionUID = 1L;

    private static final double STARTING_CASH = 1_000_000.0;

    public Admin(String id, String name, String password) {
        super(id, name, password, STARTING_CASH);
    }

    @Override
    public double commissionRate() {
        return 0.0; // admins trade commission-free
    }

    @Override
    public int maxOrderSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String role() {
        return "Admin";
    }

    /** Elevated permission - overrides the base default of {@code false}. */
    @Override
    public boolean canManageMarket() {
        return true;
    }
}
