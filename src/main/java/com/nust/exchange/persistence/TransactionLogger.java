package com.nust.exchange.persistence;

import com.nust.exchange.model.Trade;
import com.nust.exchange.patterns.MarketListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes every executed trade to an <b>append-only</b> log file as it happens.
 *
 * <p>Combines two ideas from the course: the <b>Observer pattern</b> (it is a
 * {@link MarketListener} that reacts to {@code onTrade}) and <b>file handling</b>
 * (appends a line with {@link FileWriter} in append mode). The write is
 * {@code synchronized} so concurrent trade notifications can never interleave
 * and corrupt a line.</p>
 */
public class TransactionLogger implements MarketListener {

    private final File file;

    public TransactionLogger(String path) {
        this.file = new File(path);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

    @Override
    public synchronized void onTrade(Trade trade) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) { // append
            writer.write(trade.toCsv());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write transaction log: " + e.getMessage());
        }
    }
}
