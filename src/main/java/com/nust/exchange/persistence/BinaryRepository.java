package com.nust.exchange.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Repository} that stores objects with Java <b>object serialization</b>
 * (a course topic): {@link ObjectOutputStream} to save and
 * {@link ObjectInputStream} to load.
 *
 * <p>Unlike CSV, this preserves an entire object graph - a {@code Trader} is
 * saved together with its {@code Portfolio} and every {@code Holding} - which is
 * why the trader accounts use binary storage.</p>
 *
 * @param <T> a {@link Serializable} record type
 */
public class BinaryRepository<T extends Serializable> implements Repository<T> {

    private final File file;

    public BinaryRepository(String path) {
        this.file = new File(path);
    }

    @Override
    public void save(List<T> items) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeObject(new ArrayList<>(items));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> load() throws IOException {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return (List<T>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Incompatible saved data: " + e.getMessage(), e);
        }
    }
}
