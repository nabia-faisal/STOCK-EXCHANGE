package com.nust.exchange.exceptions;

/**
 * Thrown when a trader tries to sell more shares than they hold.
 */
public class InsufficientSharesException extends TradingException {

    private static final long serialVersionUID = 1L;

    private final String symbol;
    private final int requested;
    private final int owned;

    public InsufficientSharesException(String symbol, int requested, int owned) {
        super(String.format("Insufficient shares of %s: tried to sell %d but only own %d",
                symbol, requested, owned));
        this.symbol = symbol;
        this.requested = requested;
        this.owned = owned;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getRequested() {
        return requested;
    }

    public int getOwned() {
        return owned;
    }
}
