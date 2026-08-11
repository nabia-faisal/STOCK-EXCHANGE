package com.nust.exchange.persistence;

import com.nust.exchange.enums.AssetType;
import com.nust.exchange.model.Stock;

/** Maps a {@link Stock} to/from a CSV row. */
public class StockCsvMapper implements CsvMapper<Stock> {

    @Override
    public String header() {
        return "Symbol,Name,Type,OpenPrice,LastPrice";
    }

    @Override
    public String toLine(Stock s) {
        return String.join(",",
                s.getSymbol(), s.getName(), s.getType().name(),
                String.valueOf(s.getOpenPrice()), String.valueOf(s.getLastPrice()));
    }

    @Override
    public Stock fromTokens(String[] t) {
        Stock stock = new Stock(t[0].trim(), t[1].trim(),
                AssetType.valueOf(t[2].trim()), Double.parseDouble(t[3].trim()));
        double lastPrice = Double.parseDouble(t[4].trim());
        if (lastPrice != stock.getOpenPrice()) {
            stock.setLastPrice(lastPrice); // restore current price
        }
        return stock;
    }
}
