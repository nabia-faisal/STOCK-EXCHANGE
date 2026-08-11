package com.nust.exchange.persistence;

import com.nust.exchange.model.Trade;

/** Maps a {@link Trade} to/from a CSV row (reuses Trade's own format). */
public class TradeCsvMapper implements CsvMapper<Trade> {

    @Override
    public String header() {
        return Trade.csvHeader();
    }

    @Override
    public String toLine(Trade trade) {
        return trade.toCsv();
    }

    @Override
    public Trade fromTokens(String[] tokens) {
        return Trade.fromCsv(tokens);
    }
}
