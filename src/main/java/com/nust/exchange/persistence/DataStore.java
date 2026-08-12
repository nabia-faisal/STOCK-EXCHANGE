package com.nust.exchange.persistence;

import com.nust.exchange.engine.Exchange;
import com.nust.exchange.model.Admin;
import com.nust.exchange.model.InstitutionalTrader;
import com.nust.exchange.model.RetailTrader;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;
import com.nust.exchange.model.Trader;

import java.io.IOException;
import java.util.List;

/**
 * High-level persistence facade: one call to save the whole exchange, one call
 * to load it back. Hides the individual repositories behind a simple API
 * (Single Responsibility + Facade).
 *
 * <p>Storage layout inside the data directory:</p>
 * <ul>
 *   <li>{@code stocks.csv}   - the listed instruments (CSV text I/O)</li>
 *   <li>{@code trades.csv}   - full trade history (CSV text I/O)</li>
 *   <li>{@code traders.dat}  - trader accounts + portfolios (binary serialization)</li>
 *   <li>{@code transactions.log} - live append-only audit trail</li>
 * </ul>
 *
 * <p>Loading is <b>defensive</b>: a missing or corrupt file never crashes the
 * app - it falls back to a freshly seeded default market.</p>
 */
public class DataStore {

    private final Repository<Stock> stockRepo;
    private final Repository<Trade> tradeRepo;
    private final Repository<Trader> traderRepo;
    private final String logPath;

    public DataStore(String directory) {
        this.stockRepo = new CsvRepository<>(directory + "/stocks.csv", new StockCsvMapper());
        this.tradeRepo = new CsvRepository<>(directory + "/trades.csv", new TradeCsvMapper());
        this.traderRepo = new BinaryRepository<>(directory + "/traders.dat");
        this.logPath = directory + "/transactions.log";
    }

    /** @return a live transaction logger to register with the exchange. */
    public TransactionLogger newTransactionLogger() {
        return new TransactionLogger(logPath);
    }

    /** Persist the entire exchange state. */
    public void saveAll(Exchange exchange) {
        try {
            stockRepo.save(exchange.getStocks());
            traderRepo.save(exchange.getTraders());
            tradeRepo.save(exchange.getTradeHistory());
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    /**
     * Load the exchange state, seeding sensible defaults when there is nothing
     * saved yet or a file is unreadable.
     */
    public void loadAll(Exchange exchange) {
        List<Stock> stocks = safeLoadStocks();
        if (stocks.isEmpty()) {
            seedDefaultMarket(exchange);
        } else {
            stocks.forEach(exchange::listStock);
        }

        List<Trader> traders = safeLoadTraders();
        if (traders.isEmpty()) {
            seedDefaultTraders(exchange);
        } else {
            traders.forEach(exchange::registerTrader);
        }

        exchange.restoreTradeHistory(safeLoadTrades());
    }

    private List<Stock> safeLoadStocks() {
        try {
            return stockRepo.load();
        } catch (IOException e) {
            System.err.println("Could not load stocks, seeding defaults: " + e.getMessage());
            return List.of();
        }
    }

    private List<Trader> safeLoadTraders() {
        try {
            return traderRepo.load();
        } catch (IOException e) {
            System.err.println("Could not load traders, seeding defaults: " + e.getMessage());
            return List.of();
        }
    }

    private List<Trade> safeLoadTrades() {
        try {
            return tradeRepo.load();
        } catch (IOException e) {
            System.err.println("Could not load trades: " + e.getMessage());
            return List.of();
        }
    }

    // --- Default seed data (first run / recovery) ---

    private void seedDefaultMarket(Exchange exchange) {
        exchange.listStock(new Stock("AAPL", "Apple Inc.", 180.0));
        exchange.listStock(new Stock("GOOG", "Alphabet Inc.", 140.0));
        exchange.listStock(new Stock("MSFT", "Microsoft Corp.", 420.0));
        exchange.listStock(new Stock("TSLA", "Tesla Inc.", 250.0));
        exchange.listStock(new Stock("AMZN", "Amazon.com Inc.", 185.0));
        exchange.listStock(new Stock("NVDA", "NVIDIA Corp.", 120.0));
    }

    private void seedDefaultTraders(Exchange exchange) {
        Admin admin = new Admin("admin", "Administrator", "admin123");
        seedHoldings(admin, 50);
        exchange.registerTrader(admin);

        RetailTrader alice = new RetailTrader("alice", "Alice Khan", "pass");
        seedHoldings(alice, 100);
        exchange.registerTrader(alice);

        RetailTrader bilal = new RetailTrader("bilal", "Bilal Ahmed", "pass");
        seedHoldings(bilal, 100);
        exchange.registerTrader(bilal);

        InstitutionalTrader megafund = new InstitutionalTrader("megafund", "Mega Fund", "pass");
        seedHoldings(megafund, 500);
        exchange.registerTrader(megafund);
    }

    /** Give a trader starter shares in every default stock so they can sell. */
    private void seedHoldings(Trader trader, int sharesEach) {
        String[][] defaultStocks = {
            {"AAPL", "180.0"}, {"GOOG", "140.0"}, {"MSFT", "420.0"},
            {"TSLA", "250.0"}, {"AMZN", "185.0"}, {"NVDA", "120.0"}
        };
        for (String[] s : defaultStocks) {
            trader.getPortfolio().addHolding(s[0], sharesEach, Double.parseDouble(s[1]));
        }
    }
}
