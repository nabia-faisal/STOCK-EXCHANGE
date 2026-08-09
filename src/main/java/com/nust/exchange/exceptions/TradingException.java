package com.nust.exchange.exceptions;

/**
 * Base class for all domain-specific trading errors.
 *
 * <p>Extends {@link Exception} (not {@link RuntimeException}) so these are
 * <b>checked</b> exceptions - the compiler forces callers to handle them,
 * which is exactly what we want for predictable, recoverable trading errors
 * (course topic: checked vs. unchecked exceptions).</p>
 */
public class TradingException extends Exception {

    private static final long serialVersionUID = 1L;

    public TradingException(String message) {
        super(message);
    }

    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}
