package com.nust.exchange.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Repository} that stores objects as human-readable CSV text.
 *
 * <p>Demonstrates the file-handling topics from the course: writing with
 * {@link FileWriter}/{@link BufferedWriter} and reading with
 * {@link FileReader}/{@link BufferedReader}. The per-record conversion is
 * delegated to a {@link CsvMapper}, so this one class works for any type.</p>
 *
 * @param <T> the record type
 */
public class CsvRepository<T> implements Repository<T> {

    private final File file;
    private final CsvMapper<T> mapper;

    public CsvRepository(String path, CsvMapper<T> mapper) {
        this.file = new File(path);
        this.mapper = mapper;
    }

    @Override
    public void save(List<T> items) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(mapper.header());
            writer.newLine();
            for (T item : items) {
                writer.write(mapper.toLine(item));
                writer.newLine();
            }
        }
    }

    @Override
    public List<T> load() throws IOException {
        List<T> items = new ArrayList<>();
        if (!file.exists()) {
            return items; // nothing stored yet
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    items.add(mapper.fromTokens(line.split(",")));
                } catch (RuntimeException badRow) {
                    // Skip a single corrupt row rather than failing the whole load.
                    System.err.println("Skipping malformed CSV row: " + line);
                }
            }
        }
        return items;
    }
}
