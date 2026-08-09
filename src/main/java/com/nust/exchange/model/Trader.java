package com.nust.exchange.model;

import java.io.Serializable;

/**
 * Abstract base class for all account holders on the exchange.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li><b>Abstraction</b>: abstract methods {@link #commissionRate()},
 *       {@link #maxOrderSize()} and {@link #role()} leave the "kind" of trader
 *       to subclasses.</li>
 *   <li><b>Composition</b>: every Trader HAS-A {@link Portfolio}.</li>
 *   <li><b>Encapsulation</b>: the password is stored only as a hash and is
 *       never exposed.</li>
 *   <li><b>Template behaviour</b>: {@link #computeCommission(double)} is shared
 *       logic that relies on the subclass-provided {@code commissionRate()}.</li>
 * </ul>
 */
public abstract class Trader implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;
    protected String name;
    private final int passwordHash;      // never store raw passwords
    protected final Portfolio portfolio; // composition (HAS-A)

    protected Trader(String id, String name, String password, double startingCash) {
        this.id = id;
        this.name = name;
        this.passwordHash = hash(password);
        this.portfolio = new Portfolio(id, startingCash);
    }

    // --- Abstract, subclass-specific behaviour (polymorphism) ---

    /** Fraction of trade value charged as commission (e.g. 0.005 = 0.5%). */
    public abstract double commissionRate();

    /** Largest number of shares this trader may put in a single order. */
    public abstract int maxOrderSize();

    /** Human-readable role name shown in the UI. */
    public abstract String role();

    // --- Shared, concrete behaviour ---

    /** Commission for a given trade value, using the subclass rate. */
    public double computeCommission(double tradeValue) {
        return Math.round(tradeValue * commissionRate() * 100.0) / 100.0;
    }

    /** Whether this trader may add/remove market instruments. Admins override. */
    public boolean canManageMarket() {
        return false;
    }

    public boolean verifyPassword(String attempt) {
        return hash(attempt) == passwordHash;
    }

    private static int hash(String password) {
        return password == null ? 0 : password.hashCode();
    }

    // --- Getters (encapsulation) ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, id, role());
    }
}
