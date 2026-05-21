package es.upm.trading.model;

import es.upm.trading.model.TradingSignal.Action;

/**
 * Resultado de una predicción de precio solicitada desde la UI.
 *
 * Contiene:
 *  - El precio actual en el momento de la predicción
 *  - El precio predicho N intervalos en el futuro
 *  - La variación porcentual esperada
 *  - La recomendación BUY / SELL / HOLD
 *  - El nivel de confianza del modelo (R² de la regresión)
 *  - Los intervalos objetivo solicitados (10, 25 o 50)
 *  - El coinId al que pertenece la predicción
 */
public class PredictionResult {

    private final String coinId;
    private final int    stepsAhead;    // 10, 25 o 50 intervalos
    private final double currentPrice;
    private final double predictedPrice;
    private final double changePct;     // variación % entre actual y predicho
    private final Action recommendation;
    private final double confidence;    // 0.0–1.0 (R² del modelo)
    private final long   timestamp;

    /** Umbrales para la recomendación (mismo criterio que WekaClassifier) */
    private static final double BUY_THRESHOLD  =  2.0;
    private static final double SELL_THRESHOLD = -2.0;

    public PredictionResult(String coinId, int stepsAhead,
                            double currentPrice, double predictedPrice,
                            double confidence) {
        this.coinId         = coinId;
        this.stepsAhead     = stepsAhead;
        this.currentPrice   = currentPrice;
        this.predictedPrice = predictedPrice;
        this.confidence     = Math.max(0.0, Math.min(1.0, confidence));
        this.changePct      = currentPrice > 0
                ? (predictedPrice - currentPrice) / currentPrice * 100.0
                : 0.0;
        this.recommendation = deriveAction(this.changePct);
        this.timestamp      = System.currentTimeMillis();
    }

    private static Action deriveAction(double changePct) {
        if (changePct >  BUY_THRESHOLD)  return Action.BUY;
        if (changePct < SELL_THRESHOLD)  return Action.SELL;
        return Action.HOLD;
    }

    // ── Getters ──────────────────────────────────────────────────

    public String getCoinId()          { return coinId; }
    public int    getStepsAhead()      { return stepsAhead; }
    public double getCurrentPrice()    { return currentPrice; }
    public double getPredictedPrice()  { return predictedPrice; }
    public double getChangePct()       { return changePct; }
    public Action getRecommendation()  { return recommendation; }
    public double getConfidence()      { return confidence; }
    public long   getTimestamp()       { return timestamp; }
}
