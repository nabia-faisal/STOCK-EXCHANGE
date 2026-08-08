package com.nust.exchange.model;

import com.nust.exchange.util.IdGenerator;
import com.nust.exchange.util.Money;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of one matched execution between a buyer and a seller.
 *
 * <p>All fields are {@code final} - once a trade happens it never changes.
 * Provides {@link #toCsv()} for the human-readable transaction log and is
 * {@link Serializable} for the binary snapshot.</p>
 */
public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String id;
    private final String symbol;
    private final String buyerId;
    private final String sellerId;
    private final int quantity;
    private final double price;
    private final LocalDateTime time;

    public Trade(String symbol, String buyerId, String sellerId, int quantity, double price) {
        this.id = IdGenerator.nextTradeId();
        this.symbol = symbol;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.price = Money.round(price);
        this.time = LocalDateTime.now();
    }

    /** Reconstruction constructor - rebuilds a persisted trade from stored fields. */
    public Trade(String id, String symbol, String buyerId, String sellerId,
                 int quantity, double price, LocalDateTime time) {
        this.id = id;
        this.symbol = symbol;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.quantity = quantity;
        this.price = Money.round(price);
        this.time = time;
    }

    /** Parse a CSV row produced by {@link #toCsv()} back into a Trade. */
    public static Trade fromCsv(String[] t) {
        return new Trade(t[0], t[2], t[3], t[4],
                Integer.parseInt(t[5].trim()), Double.parseDouble(t[6].trim()),
                LocalDateTime.parse(t[1].trim(), FMT));
    }

    public double getValue() {
        return Money.round(quantity * price);
    }

    /** @return one CSV row: id,time,symbol,buyer,seller,qty,price,value */
    public String toCsv() {
        return String.join(",",
                id, time.format(FMT), symbol, buyerId, sellerId,
                String.valueOf(quantity), String.valueOf(price), String.valueOf(getValue()));
    }

    public static String csvHeader() {
        return "TradeId,Time,Symbol,BuyerId,SellerId,Quantity,Price,Value";
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return String.format("%s: %d %s @ %s (buyer %s / seller %s)",
                id, quantity, symbol, Money.format(price), buyerId, sellerId);
    }
}
