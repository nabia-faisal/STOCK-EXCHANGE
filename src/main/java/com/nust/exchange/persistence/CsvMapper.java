package com.nust.exchange.persistence;

/**
 * Strategy for converting a single object of type {@code T} to and from a CSV
 * row. Supplying different mappers lets one {@link CsvRepository} handle any
 * record type (Open-Closed Principle).
 *
 * @param <T> the record type
 */
public interface CsvMapper<T> {

    /** @return the header row (column names). */
    String header();

    /** Serialise one object to a single CSV line (no trailing newline). */
    String toLine(T item);

    /** Rebuild one object from its comma-split tokens. */
    T fromTokens(String[] tokens);
}
