package com.nust.exchange.model;

import com.nust.exchange.exceptions.InsufficientFundsException;
import com.nust.exchange.exceptions.InsufficientSharesException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for cash/holding bookkeeping and error handling. */
class PortfolioTest {

    @Test
    void buyDebitsCashAndCreatesHolding() throws Exception {
        Portfolio p = new Portfolio("T1", 10_000.0);
        p.buy("AAPL", 50, 100.0);
        assertEquals(5_000.0, p.getCashBalance(), 1e-9);
        assertEquals(50, p.getHolding("AAPL").getQuantity());
    }

    @Test
    void sellCreditsCashAndReducesHolding() throws Exception {
        Portfolio p = new Portfolio("T1", 10_000.0);
        p.buy("AAPL", 50, 100.0);
        p.sell("AAPL", 20, 120.0);
        assertEquals(7_400.0, p.getCashBalance(), 1e-9);
        assertEquals(30, p.getHolding("AAPL").getQuantity());
    }

    @Test
    void weightedAverageCostIsCorrect() throws Exception {
        Portfolio p = new Portfolio("X", 1_000_000.0);
        p.buy("XYZ", 10, 100.0);
        p.buy("XYZ", 10, 200.0);
        assertEquals(150.0, p.getHolding("XYZ").getAverageCost(), 1e-9);
    }

    @Test
    void buyingBeyondCashThrows() {
        Portfolio p = new Portfolio("T", 100.0);
        assertThrows(InsufficientFundsException.class, () -> p.buy("AAPL", 10, 50.0));
    }

    @Test
    void sellingUnownedSharesThrows() {
        Portfolio p = new Portfolio("T", 100_000.0);
        assertThrows(InsufficientSharesException.class, () -> p.sell("AAPL", 5, 10.0));
    }
}
