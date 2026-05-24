package es.upm.trading.model;

import java.io.Serializable;

/**
 * Objeto que viaja en el cuerpo de los mensajes ACL REQUEST
 * desde AgenteAdquisicion -> AgentePredictor y ACL INFORM 
 * desde AgenteAdquisicion -> AgenteUI.
 * Debe ser Serializable para poder usarse con setContentObject().
 */
public class MarketData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol; // e.g. "bitcoin", "ethereum"
    private double price; // precio actual en USD
    private double priceChange5m; // variación % últimos 5 min
    private double priceChange10m; // variación % últimos 10 min
    private double priceChange30m; // variación % últimos 30 min
    private double volume24h; // volúmen 24h en USD
    private long timestamp; // epoch millis

    public MarketData(String symbol, double price, double priceChange5m, double priceChange10m,
                      double priceChange30m, double volume24h) {
        this.symbol = symbol;
        this.price = price;
        this.priceChange5m = priceChange5m;
        this.priceChange10m = priceChange10m;
        this.priceChange30m = priceChange30m;
        this.volume24h = volume24h;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters

    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getPriceChange5m() { return priceChange5m; }
    public double getPriceChange10m() { return priceChange10m; }
    public double getPriceChange30m() { return priceChange30m; }
    public double getVolume24h() { return volume24h; }
    public long getTimestamp() { return timestamp; }

    // Setters

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setPrice(double price) { this.price = price; }
    public void setPriceChange5m(double v) { this.priceChange5m = v; }
    public void setPriceChange10m(double v) { this.priceChange10m = v; }
    public void setPriceChange30m(double v) { this.priceChange30m = v; }
    public void setVolume24h(double v) { this.volume24h = v; }
    public void setTimestamp(long ts) { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("[%s] price=%.4f | Δ5m=%.2f%% | Δ10m=%.2f%% | Δ30m=%.2f%%",
                symbol, price, priceChange5m, priceChange10m, priceChange30m);
    }
}
