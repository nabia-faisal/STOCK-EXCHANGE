package com.nust.exchange.app;

/**
 * Plain launcher entry point.
 *
 * <p>This class exists so the application can be started from a normal
 * (non-modular) jar. Launching {@link App} indirectly avoids the
 * "JavaFX runtime components are missing" error that occurs when the main
 * class directly extends {@code javafx.application.Application}.</p>
 */
public final class Main {

    private Main() {
        // Utility launcher - not instantiable.
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
