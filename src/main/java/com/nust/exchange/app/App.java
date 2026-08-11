package com.nust.exchange.app;

import com.nust.exchange.engine.BotTrader;
import com.nust.exchange.engine.Exchange;
import com.nust.exchange.engine.PriceSimulator;
import com.nust.exchange.model.Trader;
import com.nust.exchange.persistence.DataStore;
import com.nust.exchange.ui.DashboardView;
import com.nust.exchange.ui.LoginView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX application entry point.
 *
 * <p>Wires the whole system together: loads persisted state, starts the
 * background threads (matching engine, price simulator, bot traders), shows the
 * login screen, and saves everything on exit.</p>
 */
public class App extends Application {

    private static final String DATA_DIR = "data";
    private static final int SCENE_W = 1180;
    private static final int SCENE_H = 760;

    private final Exchange exchange = Exchange.getInstance();
    private final DataStore dataStore = new DataStore(DATA_DIR);
    private PriceSimulator simulator;
    private final List<BotTrader> bots = new ArrayList<>();

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        // 1. Load persisted state (seeds defaults on first run).
        dataStore.loadAll(exchange);

        // 2. Every executed trade is appended to the transaction log.
        exchange.addListener(dataStore.newTransactionLogger());

        // 3. Start the matching-engine consumer thread.
        exchange.startEngine();

        // 4. Start the price simulator.
        simulator = new PriceSimulator(exchange, 800, 0.012);
        simulator.start();

        // 5. Start a few bot traders so the market is active.
        startBots();

        // 6. Show the login screen.
        showLogin();

        stage.setTitle("NUST Exchange - Stock Exchange Trading (Full)");
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    private void startBots() {
        String[] botIds = {"megafund", "bilal", "admin"};
        for (String id : botIds) {
            Trader t = exchange.getTrader(id);
            if (t != null) {
                BotTrader bot = new BotTrader(exchange, t, 1200);
                bot.start();
                bots.add(bot);
            }
        }
    }

    private void showLogin() {
        LoginView login = new LoginView(exchange, this::showDashboard);
        setRoot(login.getRoot());
    }

    private void showDashboard(Trader user) {
        DashboardView dashboard = new DashboardView(exchange, user, this::showLogin);
        setRoot(dashboard.getRoot());
    }

    private void setRoot(javafx.scene.Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, SCENE_W, SCENE_H);
            scene.getStylesheets().add(styleSheet());
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    private String styleSheet() {
        return App.class.getResource("/styles.css").toExternalForm();
    }

    /** Save state and stop all threads on exit. */
    private void shutdown() {
        for (BotTrader bot : bots) {
            bot.stop();
        }
        if (simulator != null) {
            simulator.stop();
        }
        exchange.stopEngine();
        dataStore.saveAll(exchange);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
