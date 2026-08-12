package com.nust.exchange.ui;

import com.nust.exchange.engine.Exchange;
import com.nust.exchange.engine.OrderBook;
import com.nust.exchange.enums.OrderSide;
import com.nust.exchange.enums.OrderType;
import com.nust.exchange.exceptions.TradingException;
import com.nust.exchange.model.Holding;
import com.nust.exchange.model.LimitOrder;
import com.nust.exchange.model.Order;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trade;
import com.nust.exchange.model.Trader;
import com.nust.exchange.patterns.MarketListener;
import com.nust.exchange.patterns.OrderFactory;
import com.nust.exchange.util.Money;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * The main trading dashboard shown after login.
 *
 * <p>Registers itself as a {@link MarketListener} so the exchange pushes live
 * updates; every UI mutation triggered by a background thread is marshalled onto
 * the JavaFX Application Thread with {@link Platform#runLater(Runnable)} - the
 * correct, freeze-free way to update JavaFX from worker threads.</p>
 */
public class DashboardView implements MarketListener {

    private final Exchange exchange;
    private final Trader user;
    private final Runnable onLogout;
    private final BorderPane root = new BorderPane();

    // Header
    private final Label cashLabel = new Label();

    // Market
    private final ObservableList<Stock> marketItems = FXCollections.observableArrayList();
    private final TableView<Stock> marketTable = new TableView<>(marketItems);
    private XYChart.Series<Number, Number> priceSeries;
    private String chartSymbol;

    // Order form
    private ComboBox<String> orderSymbol;
    private ChoiceBox<OrderSide> sideBox;
    private ChoiceBox<OrderType> typeBox;
    private Spinner<Integer> qtySpinner;
    private TextField limitField;
    private final Label orderMsg = new Label();

    // Portfolio
    private final ObservableList<Holding> holdingItems = FXCollections.observableArrayList();
    private final TableView<Holding> holdingsTable = new TableView<>(holdingItems);
    private final Label portfolioValueLabel = new Label();

    // Order book
    private ComboBox<String> bookSymbol;
    private final ObservableList<LimitOrder> bidItems = FXCollections.observableArrayList();
    private final ObservableList<LimitOrder> askItems = FXCollections.observableArrayList();
    private final TableView<LimitOrder> bidsTable = new TableView<>(bidItems);
    private final TableView<LimitOrder> asksTable = new TableView<>(askItems);
    private final Label spreadLabel = new Label();

    // History
    private final ObservableList<Trade> tradeItems = FXCollections.observableArrayList();
    private final TableView<Trade> tradesTable = new TableView<>(tradeItems);

    // Admin
    private final ObservableList<Trader> traderItems = FXCollections.observableArrayList();

    public DashboardView(Exchange exchange, Trader user, Runnable onLogout) {
        this.exchange = exchange;
        this.user = user;
        this.onLogout = onLogout;
        build();
        exchange.addListener(this);
        refreshAll();
    }

    public Parent getRoot() {
        return root;
    }

    // ------------------------------------------------------------------ build

    private void build() {
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        root.getStyleClass().add("dashboard");
    }

    private HBox buildHeader() {
        Label welcome = new Label("Welcome, " + user.getName() + "  (" + user.role() + ")");
        welcome.getStyleClass().add("header-title");
        cashLabel.getStyleClass().add("header-cash");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logout = new Button("Log Out");
        logout.setOnAction(e -> {
            exchange.removeListener(this);
            onLogout.run();
        });

        HBox header = new HBox(16, welcome, spacer, cashLabel, logout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.getStyleClass().add("header");
        return header;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().add(new Tab("Market", buildMarketTab()));
        tabs.getTabs().add(new Tab("Portfolio", buildPortfolioTab()));
        tabs.getTabs().add(new Tab("Order Book", buildOrderBookTab()));
        tabs.getTabs().add(new Tab("Trade History", buildHistoryTab()));
        if (user.canManageMarket()) {
            tabs.getTabs().add(new Tab("Admin", buildAdminTab()));
        }
        return tabs;
    }

    // ------------------------------------------------------------- market tab

    private Parent buildMarketTab() {
        marketTable.getColumns().add(col("Symbol", (Stock s) -> s.getSymbol()));
        marketTable.getColumns().add(col("Name", (Stock s) -> s.getName()));
        marketTable.getColumns().add(col("Last", (Stock s) -> Money.format(s.getLastPrice())));
        marketTable.getColumns().add(col("Change", (Stock s) -> String.format("%+.2f%%", s.getChangePercent())));
        marketTable.getColumns().add(col("Bid", (Stock s) -> priceOrDash(bestBid(s.getSymbol()))));
        marketTable.getColumns().add(col("Ask", (Stock s) -> priceOrDash(bestAsk(s.getSymbol()))));
        marketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marketTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectSymbol(sel.getSymbol());
            }
        });

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Tick");
        yAxis.setLabel("Price");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        priceSeries = new XYChart.Series<>();
        chart.getData().add(priceSeries);
        chart.setPrefHeight(260);

        VBox left = new VBox(10, sectionTitle("Live Market"), marketTable, sectionTitle("Price History"), chart);
        left.setPadding(new Insets(12));
        VBox.setVgrow(marketTable, Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox content = new HBox(12, left, buildOrderForm());
        content.setPadding(new Insets(6));
        return content;
    }

    private VBox buildOrderForm() {
        orderSymbol = new ComboBox<>();
        sideBox = new ChoiceBox<>(FXCollections.observableArrayList(OrderSide.values()));
        sideBox.setValue(OrderSide.BUY);
        typeBox = new ChoiceBox<>(FXCollections.observableArrayList(OrderType.values()));
        typeBox.setValue(OrderType.LIMIT);
        qtySpinner = new Spinner<>(1, 1_000_000, 10);
        qtySpinner.setEditable(true);
        limitField = new TextField();
        limitField.setPromptText("Limit price");

        // Disable limit price for market orders.
        typeBox.valueProperty().addListener((o, old, val) ->
                limitField.setDisable(val == OrderType.MARKET));

        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        int r = 0;
        form.add(new Label("Symbol"), 0, r);
        form.add(orderSymbol, 1, r++);
        form.add(new Label("Side"), 0, r);
        form.add(sideBox, 1, r++);
        form.add(new Label("Type"), 0, r);
        form.add(typeBox, 1, r++);
        form.add(new Label("Quantity"), 0, r);
        form.add(qtySpinner, 1, r++);
        form.add(new Label("Limit Price"), 0, r);
        form.add(limitField, 1, r++);

        Button submit = new Button("Submit Order");
        submit.getStyleClass().add("primary");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> submitOrder());
        orderMsg.setWrapText(true);

        VBox panel = new VBox(12, sectionTitle("Place Order"), form, submit, orderMsg);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(320);
        return panel;
    }

    private void submitOrder() {
        String symbol = orderSymbol.getValue();
        Stock stock = symbol == null ? null : exchange.getStock(symbol);
        if (stock == null) {
            showOrderMsg("Select a symbol first.", true);
            return;
        }
        double limit = 0;
        if (typeBox.getValue() == OrderType.LIMIT) {
            try {
                limit = Double.parseDouble(limitField.getText().trim());
            } catch (NumberFormatException ex) {
                showOrderMsg("Enter a valid limit price.", true);
                return;
            }
        }
        try {
            Order order = OrderFactory.create(user, stock, sideBox.getValue(),
                    typeBox.getValue(), qtySpinner.getValue(), limit);
            exchange.submitOrder(order);
            showOrderMsg("Order submitted: " + order.getSide().getLabel() + " "
                    + qtySpinner.getValue() + " " + symbol, false);
        } catch (TradingException ex) {
            showOrderMsg(ex.getMessage(), true);
        }
    }

    private void showOrderMsg(String text, boolean error) {
        orderMsg.setText(text);
        orderMsg.getStyleClass().removeAll("error", "success");
        orderMsg.getStyleClass().add(error ? "error" : "success");
    }

    // ---------------------------------------------------------- portfolio tab

    private Parent buildPortfolioTab() {
        holdingsTable.getColumns().add(col("Symbol", (Holding h) -> h.getSymbol()));
        holdingsTable.getColumns().add(col("Quantity", (Holding h) -> String.valueOf(h.getQuantity())));
        holdingsTable.getColumns().add(col("Avg Cost", (Holding h) -> Money.format(h.getAverageCost())));
        holdingsTable.getColumns().add(col("Market Price", (Holding h) -> priceOrDash(currentPrice(h.getSymbol()))));
        holdingsTable.getColumns().add(col("Market Value", (Holding h) -> marketValue(h)));
        holdingsTable.getColumns().add(col("Unrealized P&L", (Holding h) -> unrealizedPnl(h)));
        holdingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        portfolioValueLabel.getStyleClass().add("big-value");

        VBox box = new VBox(12, sectionTitle("Your Portfolio"), portfolioValueLabel, holdingsTable);
        box.setPadding(new Insets(16));
        VBox.setVgrow(holdingsTable, Priority.ALWAYS);
        return box;
    }

    // ---------------------------------------------------------- order-book tab

    private Parent buildOrderBookTab() {
        bookSymbol = new ComboBox<>();
        bookSymbol.valueProperty().addListener((o, old, val) -> refreshOrderBook());

        bidsTable.getColumns().add(col("Bid Price", (LimitOrder l) -> Money.format(l.getLimitPrice())));
        bidsTable.getColumns().add(col("Quantity", (LimitOrder l) -> String.valueOf(l.getRemainingQuantity())));
        bidsTable.getColumns().add(col("Trader", (LimitOrder l) -> l.getOwner().getId()));
        bidsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        asksTable.getColumns().add(col("Ask Price", (LimitOrder l) -> Money.format(l.getLimitPrice())));
        asksTable.getColumns().add(col("Quantity", (LimitOrder l) -> String.valueOf(l.getRemainingQuantity())));
        asksTable.getColumns().add(col("Trader", (LimitOrder l) -> l.getOwner().getId()));
        asksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox bidsBox = new VBox(6, sectionTitle("Bids (buyers)"), bidsTable);
        VBox asksBox = new VBox(6, sectionTitle("Asks (sellers)"), asksTable);
        HBox.setHgrow(bidsBox, Priority.ALWAYS);
        HBox.setHgrow(asksBox, Priority.ALWAYS);
        VBox.setVgrow(bidsTable, Priority.ALWAYS);
        VBox.setVgrow(asksTable, Priority.ALWAYS);
        HBox tables = new HBox(12, bidsBox, asksBox);
        VBox.setVgrow(tables, Priority.ALWAYS);

        HBox top = new HBox(10, new Label("Symbol:"), bookSymbol, spreadLabel);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, top, tables);
        box.setPadding(new Insets(16));
        return box;
    }

    // -------------------------------------------------------------- history tab

    private Parent buildHistoryTab() {
        tradesTable.getColumns().add(col("Trade ID", (Trade t) -> t.getId()));
        tradesTable.getColumns().add(col("Symbol", (Trade t) -> t.getSymbol()));
        tradesTable.getColumns().add(col("Quantity", (Trade t) -> String.valueOf(t.getQuantity())));
        tradesTable.getColumns().add(col("Price", (Trade t) -> Money.format(t.getPrice())));
        tradesTable.getColumns().add(col("Value", (Trade t) -> Money.format(t.getValue())));
        tradesTable.getColumns().add(col("Buyer", (Trade t) -> t.getBuyerId()));
        tradesTable.getColumns().add(col("Seller", (Trade t) -> t.getSellerId()));
        tradesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox box = new VBox(12, sectionTitle("Recent Trades (most recent first)"), tradesTable);
        box.setPadding(new Insets(16));
        VBox.setVgrow(tradesTable, Priority.ALWAYS);
        return box;
    }

    // ---------------------------------------------------------------- admin tab

    private Parent buildAdminTab() {
        TextField symField = new TextField();
        symField.setPromptText("Symbol");
        TextField nameField = new TextField();
        nameField.setPromptText("Company name");
        TextField priceField = new TextField();
        priceField.setPromptText("Opening price");
        Label adminMsg = new Label();

        Button addBtn = new Button("List New Stock");
        addBtn.getStyleClass().add("primary");
        addBtn.setOnAction(e -> {
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                Stock s = new Stock(symField.getText().trim(), nameField.getText().trim(), price);
                exchange.listStock(s);
                refreshSymbols();
                refreshMarket();
                adminMsg.getStyleClass().setAll("success");
                adminMsg.setText("Listed " + s.getSymbol());
                symField.clear();
                nameField.clear();
                priceField.clear();
            } catch (RuntimeException ex) {
                adminMsg.getStyleClass().setAll("error");
                adminMsg.setText("Could not list stock: " + ex.getMessage());
            }
        });

        HBox addRow = new HBox(10, symField, nameField, priceField, addBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TableView<Trader> tradersTable = new TableView<>(traderItems);
        tradersTable.getColumns().add(col("ID", (Trader t) -> t.getId()));
        tradersTable.getColumns().add(col("Name", (Trader t) -> t.getName()));
        tradersTable.getColumns().add(col("Role", (Trader t) -> t.role()));
        tradersTable.getColumns().add(col("Cash", (Trader t) -> Money.format(t.getPortfolio().getCashBalance())));
        tradersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        VBox box = new VBox(12, sectionTitle("List a New Instrument"), addRow, adminMsg,
                sectionTitle("All Traders"), tradersTable);
        box.setPadding(new Insets(16));
        VBox.setVgrow(tradersTable, Priority.ALWAYS);
        return box;
    }

    // -------------------------------------------------------- MarketListener

    @Override
    public void onPriceUpdate(Stock stock) {
        Platform.runLater(() -> {
            marketTable.refresh();
            updateCash();
            if (stock.getSymbol().equals(chartSymbol)) {
                refreshChart();
            }
        });
    }

    @Override
    public void onTrade(Trade trade) {
        Platform.runLater(() -> {
            refreshHistory();
            refreshPortfolio();
            updateCash();
        });
    }

    @Override
    public void onOrderBookChanged(String symbol) {
        Platform.runLater(() -> {
            marketTable.refresh();
            if (symbol.equals(bookSymbol == null ? null : bookSymbol.getValue())) {
                refreshOrderBook();
            }
        });
    }

    // ---------------------------------------------------------------- refresh

    private void refreshAll() {
        refreshSymbols();
        refreshMarket();
        refreshPortfolio();
        refreshHistory();
        refreshOrderBook();
        updateCash();
        if (!marketItems.isEmpty()) {
            marketTable.getSelectionModel().selectFirst();
        }
    }

    private void refreshSymbols() {
        List<String> symbols = FXCollections.observableArrayList();
        for (Stock s : exchange.getStocks()) {
            symbols.add(s.getSymbol());
        }
        Collections.sort(symbols);
        if (orderSymbol != null) {
            String keep = orderSymbol.getValue();
            orderSymbol.getItems().setAll(symbols);
            orderSymbol.setValue(keep != null && symbols.contains(keep) ? keep
                    : (symbols.isEmpty() ? null : symbols.get(0)));
        }
        if (bookSymbol != null) {
            String keep = bookSymbol.getValue();
            bookSymbol.getItems().setAll(symbols);
            bookSymbol.setValue(keep != null && symbols.contains(keep) ? keep
                    : (symbols.isEmpty() ? null : symbols.get(0)));
        }
    }

    private void refreshMarket() {
        marketItems.setAll(exchange.getStocks());
        marketTable.refresh();
    }

    private void refreshPortfolio() {
        holdingItems.setAll(user.getPortfolio().getHoldings().values());
        double total = user.getPortfolio().getTotalValue(exchange.priceLookup());
        portfolioValueLabel.setText("Total Account Value: " + Money.format(total)
                + "    |    Cash: " + Money.format(user.getPortfolio().getCashBalance()));
    }

    private void refreshHistory() {
        List<Trade> all = exchange.getTradeHistory();
        int from = Math.max(0, all.size() - 200);
        List<Trade> recent = new java.util.ArrayList<>(all.subList(from, all.size()));
        Collections.reverse(recent);
        tradeItems.setAll(recent);
    }

    private void refreshOrderBook() {
        String symbol = bookSymbol == null ? null : bookSymbol.getValue();
        OrderBook book = symbol == null ? null : exchange.getOrderBook(symbol);
        if (book == null) {
            bidItems.clear();
            askItems.clear();
            spreadLabel.setText("");
            return;
        }
        bidItems.setAll(book.snapshotBids());
        askItems.setAll(book.snapshotAsks());
        Double spread = book.getSpread();
        spreadLabel.setText(spread == null ? "Spread: -" : "Spread: " + Money.format(spread));
    }

    private void refreshChart() {
        if (chartSymbol == null) {
            return;
        }
        Stock stock = exchange.getStock(chartSymbol);
        if (stock == null) {
            return;
        }
        List<Double> history = stock.getPriceHistory();
        int from = Math.max(0, history.size() - 120);
        priceSeries.getData().clear();
        for (int i = from; i < history.size(); i++) {
            priceSeries.getData().add(new XYChart.Data<>(i, history.get(i)));
        }
    }

    private void selectSymbol(String symbol) {
        chartSymbol = symbol;
        if (orderSymbol != null && symbol != null) {
            orderSymbol.setValue(symbol);
        }
        refreshChart();
    }

    private void updateCash() {
        cashLabel.setText("Cash: " + Money.format(user.getPortfolio().getCashBalance()));
    }

    // ------------------------------------------------------------------ helpers

    private <S> TableColumn<S, String> col(String title, Function<S, String> extractor) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(extractor.apply(data.getValue())));
        return c;
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private Double bestBid(String symbol) {
        OrderBook b = exchange.getOrderBook(symbol);
        return b == null ? null : b.getBestBid();
    }

    private Double bestAsk(String symbol) {
        OrderBook b = exchange.getOrderBook(symbol);
        return b == null ? null : b.getBestAsk();
    }

    private Double currentPrice(String symbol) {
        Stock s = exchange.getStock(symbol);
        return s == null ? null : s.getLastPrice();
    }

    private String marketValue(Holding h) {
        Double p = currentPrice(h.getSymbol());
        return p == null ? "-" : Money.format(h.getMarketValue(p));
    }

    private String unrealizedPnl(Holding h) {
        Double p = currentPrice(h.getSymbol());
        return p == null ? "-" : Money.format(h.getUnrealizedPnL(p));
    }

    private String priceOrDash(Double price) {
        return price == null ? "-" : Money.format(price);
    }
}
