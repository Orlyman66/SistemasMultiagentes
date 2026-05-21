package es.upm.trading.model;

import java.io.Serializable;

/**
 * Señal de trading generada por AgentePredictor.
 * Viaja en mensajes ACL INFORM hacia AgenteUI.
 */
public class TradingSignal implements Serializable {

    private static final long serialVersionUID = 2L;

    public enum Action { BUY, SELL, HOLD }

    private Action action;
    private String symbol;
    private double price;         // precio en el momento de la señal
    private double confidence;    // 0.0–1.0 (accuracy del modelo Weka)
    private String justification; // texto corto para mostrar en UI
    private long timestamp;

    public TradingSignal() {}

    public TradingSignal(Action action, String symbol, double price,
                         double confidence, String justification) {
        this.action        = action;
        this.symbol        = symbol;
        this.price         = price;
        this.confidence    = confidence;
        this.justification = justification;
        this.timestamp     = System.currentTimeMillis();
    }

    // ── Getters ──────────────────────────────────────────────

    public Action getAction()          { return action; }
    public String getSymbol()          { return symbol; }
    public double getPrice()           { return price; }
    public double getConfidence()      { return confidence; }
    public String getJustification()   { return justification; }
    public long   getTimestamp()       { return timestamp; }

    // ── Setters ──────────────────────────────────────────────

    public void setAction(Action a)       { this.action = a; }
    public void setSymbol(String s)       { this.symbol = s; }
    public void setPrice(double p)        { this.price = p; }
    public void setConfidence(double c)   { this.confidence = c; }
    public void setJustification(String j){ this.justification = j; }
    public void setTimestamp(long ts)     { this.timestamp = ts; }

    @Override
    public String toString() {
        return String.format("[%s] %s @ %.4f (conf=%.1f%%) — %s",
                symbol, action, price, confidence * 100, justification);
    }
}
