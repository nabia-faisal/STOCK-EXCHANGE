/**
 * Persistence layer (File Handling).
 *
 * <p>Generic {@code Repository<T>} interface with {@code CsvRepository} and
 * {@code BinaryRepository} implementations, plus a {@code DataStore} facade
 * that saves state on exit and loads it on startup (CSV for human-readable
 * data, binary serialization for full-state snapshots, append-only log for
 * the transaction history).</p>
 */
package com.nust.exchange.persistence;
