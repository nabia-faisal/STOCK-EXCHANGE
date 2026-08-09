package com.nust.exchange.exceptions;

/**
 * Thrown when an order is malformed - e.g. non-positive quantity, a limit
 * order with no price, or a price/quantity above a trader's allowed maximum.
 */
public class InvalidOrderException extends TradingException {

    private static final long serialVersionUID = 1L;

    public InvalidOrderException(String message) {
        super(message);
    }
}
