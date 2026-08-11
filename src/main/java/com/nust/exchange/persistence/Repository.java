package com.nust.exchange.persistence;

import java.io.IOException;
import java.util.List;

/**
 * Generic persistence abstraction (a <b>generic interface</b> - a course
 * topic). A {@code Repository<T>} can save and reload a list of {@code T}s,
 * regardless of whether the underlying storage is CSV text or a binary file.
 *
 * <p>Depending on this interface (rather than a concrete file class) is the
 * Dependency-Inversion Principle in action: the rest of the app can swap CSV
 * for binary storage without any other code changing.</p>
 *
 * @param <T> the type of object stored
 */
public interface Repository<T> {

    /** Persist all items, overwriting any previous contents. */
    void save(List<T> items) throws IOException;

    /** Load all items, or an empty list if nothing has been stored yet. */
    List<T> load() throws IOException;
}
