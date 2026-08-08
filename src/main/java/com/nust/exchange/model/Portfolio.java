package com.nust.exchange.model;

import com.nust.exchange.exceptions.InsufficientFundsException;
import com.nust.exchange.exceptions.InsufficientSharesException;
import com.nust.exchange.util.Money;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A trader's account: cash balance plus a collection of {@link Holding}s.
 *
 * <p>Illustrates <b>composition</b> (a Portfolio is built from Holdings),
 * <b>method overloading</b> ({@code addHolding} has two forms), a
 * <b>no-argument constructor</b> alongside a parameterised one, and the use of
 * <b>custom checked exceptions</b> to reject invalid transactions.</p>
 *
 * <p>The buy/sell methods are {@code synchronized} because a trader's balance is
 * shared state that bot threads and the matching-engine thread may update
 * concurrently (protects against lost updates - a course-adjacent concurrency
 * requirement of the project).</p>
 */
public class Portfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double DEFAULT_STARTING_CASH = 100_000.0;

    private String ownerId;
    private double cashBalance;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();

    /** No-argument constructor (course topic) - starts with default cash. */
    public Portfolio() {
        this("", DEFAULT_STARTING_CASH);
    }

    /** Convenience overload: default starting cash for a named owner. */
    public Portfolio(String ownerId) {
        this(ownerId, DEFAULT_STARTING_CASH);
    }

    /** Full constructor. */
    public Portfolio(String ownerId, double startingCash) {
        this.ownerId = ownerId;
        this.cashBalance = startingCash;
    }

    /** Execute a buy: validate funds, debit cash, and add to the position. */
    public synchronized void buy(String symbol, int quantity, double price)
            throws InsufficientFundsException {
        double cost = quantity * price;
        if (cost > cashBalance) {
            throw new InsufficientFundsException(cost, cashBalance);
        }
        cashBalance = Money.round(cashBalance - cost);
        addHolding(symbol, quantity, price);
    }

    /** Execute a sell: validate holdings, credit cash, and reduce the position. */
    public synchronized void sell(String symbol, int quantity, double price)
            throws InsufficientSharesException {
        Holding holding = holdings.get(symbol.toUpperCase());
        int owned = holding == null ? 0 : holding.getQuantity();
        if (quantity > owned) {
            throw new InsufficientSharesException(symbol, quantity, owned);
        }
        holding.removeShares(quantity);
        if (holding.getQuantity() == 0) {
            holdings.remove(symbol.toUpperCase());
        }
        cashBalance = Money.round(cashBalance + (quantity * price));
    }

    /** Overload #1: add shares by symbol/quantity/price. */
    public synchronized void addHolding(String symbol, int quantity, double price) {
        holdings.computeIfAbsent(symbol.toUpperCase(), s -> new Holding(s, 0, 0))
                .addShares(quantity, price);
    }

    /** Overload #2: merge an existing {@link Holding} object. */
    public synchronized void addHolding(Holding holding) {
        addHolding(holding.getSymbol(), holding.getQuantity(), holding.getAverageCost());
    }

    public synchronized void deposit(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deposit cannot be negative");
        }
        cashBalance = Money.round(cashBalance + amount);
    }

    /** Debit cash (e.g. commission/fees); rejects overdrafts. */
    public synchronized void withdraw(double amount) throws InsufficientFundsException {
        if (amount < 0) {
            throw new IllegalArgumentException("Withdrawal cannot be negative");
        }
        if (amount > cashBalance) {
            throw new InsufficientFundsException(amount, cashBalance);
        }
        cashBalance = Money.round(cashBalance - amount);
    }

    public synchronized double getCashBalance() {
        return cashBalance;
    }

    public Holding getHolding(String symbol) {
        return holdings.get(symbol.toUpperCase());
    }

    public Map<String, Holding> getHoldings() {
        return Collections.unmodifiableMap(holdings);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Total account value = cash + market value of every holding, priced with
     * the supplied symbol-&gt;price lookup.
     */
    public synchronized double getTotalValue(Map<String, Double> priceLookup) {
        double total = cashBalance;
        for (Holding h : holdings.values()) {
            Double price = priceLookup.get(h.getSymbol());
            if (price != null) {
                total += h.getMarketValue(price);
            }
        }
        return Money.round(total);
    }
}
