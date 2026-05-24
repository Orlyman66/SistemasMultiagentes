package es.upm.trading.ml;

import es.upm.trading.model.PredictionResult;

import weka.classifiers.trees.M5P;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

/**
 * Predictor de precio futuro usando Weka M5P.
 * 
 *
 * Predicción iterativa: predice t+1, lo mete en la ventana, predice t+2,
 * y así hasta stepsAhead (1, 3 o 5).
 */
public class PriceForecaster {

    private static final int WINDOW_SIZE  = 5;
    public  static final int MIN_SAMPLES  = 20;

    private static final int NUM_FEATURES = WINDOW_SIZE + 2;

    
    /**
     * Prediuce el precio de una moneda específica en N intervalos futuros
     *
     * @param coinId  id de la moneda (e.g. "bitcoin")
     * @param priceHistory  histórico de precios de la moneda 
     * @param stepsAhead  intervalos a futuro que se quieren predecir
     * @return resultado de la predicción
     */
    public PredictionResult predict(String coinId, List<Double> priceHistory, int stepsAhead) {
        if (priceHistory == null || priceHistory.size() < MIN_SAMPLES) {
            return null;
        }

        try {
            // Normalizar todos los precios dividiendolos  entre le primero para trabajar con valores cercanos al 1
            double base = priceHistory.get(0) != 0 ? priceHistory.get(0) : 1.0;
            List<Double> norm = new ArrayList<>();
            for (double p : priceHistory) norm.add(p / base);

            // Construir dataset Weka con precios normalizados
            Instances dataset = buildDataset(norm);

            // Entrenar M5P
            M5P m5p = new M5P();
            m5p.buildClassifier(dataset);

            // R² sobre datos de entrenamiento
            double r2 = computeR2(m5p, dataset);

            // Predicción iterativa N pasos
            List<Double> window = new ArrayList<>(norm.subList(norm.size() - WINDOW_SIZE, norm.size()));

            double predictedNorm = 0;
            for (int step = 0; step < stepsAhead; step++) {
                Instance inst = buildInstance(window, dataset);
                predictedNorm = m5p.classifyInstance(inst);
                window.remove(0);
                window.add(predictedNorm);
            }

            // Desnormalizar
            double currentPrice   = priceHistory.get(priceHistory.size() - 1);
            double predictedPrice = predictedNorm * base;

            return new PredictionResult(coinId, stepsAhead, currentPrice, predictedPrice, r2);

        } catch (Exception e) {
            System.err.println("[PriceForecaster] Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construye el dataset de entrenamiento con features enriquecidas.
     *
     * Cada instancia:
     *   t-4, t-3, t-2, t-1, t  -> precios normalizados de la ventana
     *   tendencia  -> pendiente OLS (captura si sube o baja)
     *   volatilidad  -> desviación estándar (captura dispersión)
     *   target  -> precio en t+1 normalizado
     */
    private Instances buildDataset(List<Double> norm) {
        ArrayList<Attribute> attrs = new ArrayList<>();
        for (int i = 0; i < WINDOW_SIZE; i++) {
            attrs.add(new Attribute("t_minus_" + (WINDOW_SIZE - i)));
        }
        attrs.add(new Attribute("tendencia"));
        attrs.add(new Attribute("volatilidad"));
        attrs.add(new Attribute("target"));

        Instances dataset = new Instances("PriceForecast", attrs, norm.size());
        dataset.setClassIndex(NUM_FEATURES);

        for (int i = WINDOW_SIZE; i < norm.size(); i++) {
            List<Double> window = norm.subList(i - WINDOW_SIZE, i);
            Instance inst = new DenseInstance(NUM_FEATURES + 1);
            inst.setDataset(dataset);
            for (int j = 0; j < WINDOW_SIZE; j++) {
                inst.setValue(j, window.get(j));
            }
            inst.setValue(WINDOW_SIZE, computeTrend(window));
            inst.setValue(WINDOW_SIZE + 1, computeVolatility(window));
            inst.setValue(NUM_FEATURES, norm.get(i));
            dataset.add(inst);
        }
        return dataset;
    }

    
    
    
    private Instance buildInstance(List<Double> window, Instances dataset) {
        Instance inst = new DenseInstance(NUM_FEATURES + 1);
        inst.setDataset(dataset);
        for (int i = 0; i < WINDOW_SIZE; i++) {
            inst.setValue(i, window.get(i));
        }
        inst.setValue(WINDOW_SIZE, computeTrend(window));
        inst.setValue(WINDOW_SIZE + 1, computeVolatility(window));
        inst.setMissing(NUM_FEATURES);
        return inst;
    }


    /**
     * Pendiente de la recta de mínimos cuadrados sobre la ventana.
     * Positiva = tendencia al alza, negativa = a la baja.
     */
    private double computeTrend(List<Double> window) {
        int n = window.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += window.get(i);
            sumXY += i * window.get(i);
            sumX2 += i * i;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-12) return 0;
        return (n * sumXY - sumX * sumY) / denom;
    }

    /**
     * Desviación estándar de la ventana (volatilidad local).
     */
    private double computeVolatility(List<Double> window) {
        int n = window.size();
        double mean = 0;
        for (double v : window) mean += v;
        mean /= n;
        double variance = 0;
        for (double v : window) variance += Math.pow(v - mean, 2);
        return Math.sqrt(variance / n);
    }

    

    private double computeR2(M5P model, Instances dataset) throws Exception {
        double mean = 0;
        for (int i = 0; i < dataset.numInstances(); i++) {
            mean += dataset.instance(i).classValue();
        }
        mean /= dataset.numInstances();

        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < dataset.numInstances(); i++) {
            Instance inst = dataset.instance(i);
            double actual = inst.classValue();
            double predicted = model.classifyInstance(inst);
            ssTot += Math.pow(actual - mean, 2);
            ssRes += Math.pow(actual - predicted, 2);
        }
        if (ssTot == 0) return 0;
        return Math.max(0.0, Math.min(1.0, 1.0 - ssRes / ssTot));
    }

    public static int getMinSamples() { return MIN_SAMPLES; }
    public static int getWindowSize() { return WINDOW_SIZE; }
}
