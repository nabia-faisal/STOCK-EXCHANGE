package com.nust.exchange.ui;

import com.nust.exchange.engine.Exchange;
import com.nust.exchange.model.InstitutionalTrader;
import com.nust.exchange.model.RetailTrader;
import com.nust.exchange.model.Stock;
import com.nust.exchange.model.Trader;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * The login / registration screen. On a successful login it hands the
 * authenticated {@link Trader} back to the caller through a callback.
 *
 * <p>Demonstrates JavaFX controls and <b>event handling</b> (button actions).</p>
 */
public class LoginView {

    private final Exchange exchange;
    private final Consumer<Trader> onLoginSuccess;
    private final StackPane root = new StackPane();

    public LoginView(Exchange exchange, Consumer<Trader> onLoginSuccess) {
        this.exchange = exchange;
        this.onLoginSuccess = onLoginSuccess;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    private void build() {
        Label title = new Label("NUST Exchange");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Stock Exchange Trading (Full)");
        subtitle.getStyleClass().add("subtitle");

        TextField userField = new TextField();
        userField.setPromptText("Username (e.g. alice, megafund, admin)");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password (default: pass / admin123)");

        ChoiceBox<String> roleChoice = new ChoiceBox<>();
        roleChoice.getItems().addAll("Retail", "Institutional");
        roleChoice.setValue("Retail");

        Label message = new Label();
        message.getStyleClass().add("error");

        Button loginBtn = new Button("Log In");
        loginBtn.getStyleClass().add("primary");
        loginBtn.setDefaultButton(true);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin(userField.getText(), passField.getText(), message));

        Button registerBtn = new Button("Register");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> handleRegister(
                userField.getText(), passField.getText(), roleChoice.getValue(), message));

        HBox roleRow = new HBox(10, new Label("New account type:"), roleChoice);
        roleRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, title, subtitle,
                userField, passField, loginBtn, roleRow, registerBtn, message);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(32));

        root.getChildren().add(card);
        root.getStyleClass().add("login-bg");
        StackPane.setMargin(card, new Insets(40));
    }

    private void handleLogin(String id, String password, Label message) {
        if (isBlank(id) || isBlank(password)) {
            message.setText("Please enter a username and password.");
            return;
        }
        Trader trader = exchange.authenticate(id.trim(), password);
        if (trader == null) {
            message.setText("Invalid username or password.");
            return;
        }
        onLoginSuccess.accept(trader);
    }

    private void handleRegister(String id, String password, String role, Label message) {
        if (isBlank(id) || isBlank(password)) {
            message.setText("Choose a username and password to register.");
            return;
        }
        id = id.trim();
        if (exchange.getTrader(id) != null) {
            message.setText("That username is already taken.");
            return;
        }
        Trader trader = "Institutional".equals(role)
                ? new InstitutionalTrader(id, id, password)
                : new RetailTrader(id, id, password);
        // Seed starter shares so the new trader can sell from day one.
        for (Stock s : exchange.getStocks()) {
            trader.getPortfolio().addHolding(s.getSymbol(), 50, s.getLastPrice());
        }
        exchange.registerTrader(trader);
        message.getStyleClass().remove("error");
        message.setText("Account created - you can now log in.");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
