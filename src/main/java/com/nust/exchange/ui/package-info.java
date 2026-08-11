/**
 * JavaFX user interface: controllers and view wiring.
 *
 * <p>Login screen and the main trading dashboard (live market table, order
 * entry form, portfolio panel, order-book depth view, price chart, and the
 * admin panel). UI updates from background threads are marshalled onto the
 * JavaFX Application Thread via {@code Platform.runLater}.</p>
 */
package com.nust.exchange.ui;
