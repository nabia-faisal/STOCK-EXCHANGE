package com.nust.exchange.model;

import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderStatus;
import com.nust.exchange.enums.OrderType;
import com.nust.exchange.util.IdGenerator;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for every order placed on the exchange.
 *
 * <p>This class is the centrepiece of the project's OOP demonstration:</p>
 * <ul>
 *   <li><b>Abstraction</b>: declared {@code abstract} with abstract methods
 *       {@link #getType()} and {@link #acceptsPrice(double)} that subclasses
 *       must implement.</li>
 *   <li><b>Inheritance</b>: {@link MarketOrder} and {@link LimitOrder} extend
 *       it and reuse the shared state below.</li>
 *   <li><b>Polymorphism</b>: the matching engine handles every order as an
 *       {@code Order} and calls the overridden {@code acceptsPrice} at runtime
 *       (dynamic method dispatch).</li>
 *   <li><b>{@code protected} members</b>: shared fields are visible to
 *       subclasses but hidden from the outside world (encapsulation).</li>
 *   <li><b>{@link Comparable}</b>: natural ordering is by arrival sequence,
 *       giving <i>time priority</i> for equally-priced orders.</li>
 * </ul>
 */
public abstract class Order implements Comparable<Order>, Serializable {

    private static final long serialVersionUID = 1L;

    /** Global arrival counter - static member giving strict time priority. */
    private static final AtomicLong ARRIVAL_SEQUENCE = new AtomicLong();

    private final String id;
    protected final Trader owner;   // association: an order is owned by a trader
    protected final Stock stock;    // association: an order targets a stock
    protected final OrderSide side;
    protected final int quantity;
    protected int filledQuantity;
    protected OrderStatus status;
    private final long sequence;    // arrival order (time priority tie-breaker)
    private final long timestamp;   // wall-clock creation time

    protected Order(Trader owner, Stock stock, OrderSide side, int quantity) {
        this.id = IdGenerator.nextOrderId();
        this.owner = owner;
        this.stock = stock;
        this.side = side;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.status = OrderStatus.OPEN;
        this.sequence = ARRIVAL_SEQUENCE.getAndIncrement();
        this.timestamp = System.currentTimeMillis();
    }

    // --- Abstract behaviour each subclass must define (polymorphism) ---

    /** @return whether this is a MARKET or LIMIT order. */
    public abstract OrderType getType();

    /**
     * Polymorphic matching condition: would this order agree to trade at the
     * given price? A market order accepts any price; a limit order only
     * accepts prices at or better than its limit.
     */
    public abstract boolean acceptsPrice(double price);

    // --- Shared, concrete behaviour ---

    /** Record a (possibly partial) fill and update the status accordingly. */
    public void fill(int qty) {
        if (qty <= 0 || qty > getRemainingQuantity()) {
            throw new IllegalArgumentException("Invalid fill quantity: " + qty);
        }
        this.filledQuantity += qty;
        this.status = (filledQuantity == quantity) ? OrderStatus.FILLED : OrderStatus.PARTIAL;
    }

    public void cancel() {
        if (!status.isTerminal()) {
            this.status = OrderStatus.CANCELLED;
        }
    }

    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public boolean isActive() {
        return !status.isTerminal() && getRemainingQuantity() > 0;
    }

    // --- Getters (encapsulation) ---

    public String getId() {
        return id;
    }

    public Trader getOwner() {
        return owner;
    }

    public Stock getStock() {
        return stock;
    }

    public OrderSide getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getFilledQuantity() {
        return filledQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isBuy() {
        return side == OrderSide.BUY;
    }

    /** Natural ordering: earliest arrival first (time priority). */
    @Override
    public int compareTo(Order other) {
        return Long.compare(this.sequence, other.sequence);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s x%d (%s) [%s]",
                id, side.getLabel(), stock.getSymbol(), quantity,
                getType().getLabel(), status.getLabel());
    }
}
