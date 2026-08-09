package com.nust.exchange.exceptions;

/**
 * Thrown when a trader tries to buy but does not have enough cash.
 *
 * <p>Demonstrates a custom, domain-specific checked exception that carries
 * structured data (required vs. available) alongside the message.</p>
 */
public class InsufficientFundsException extends TradingException {

    private static final long serialVersionUID = 1L;

    private final double required;
    private final double available;

    public InsufficientFundsException(double required, double available) {
        super(String.format("Insufficient funds: need %.2f but only %.2f available",
                required, available));
        this.required = required;
        this.available = available;
    }

    public double getRequired() {
        return required;
    }

    public double getAvailable() {
        return available;
    }
}
