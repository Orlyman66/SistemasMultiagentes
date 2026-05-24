package es.upm.trading.ml;

import es.upm.trading.model.MarketData;
import es.upm.trading.model.TradingSignal;
import es.upm.trading.model.TradingSignal.Action;

import weka.classifiers.trees.J48;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Clasificador de señales de trading usando Weka J48.
 *
 *  - Construye un dataset ARFF en memoria con ventanas de precios históricas.
 *  - Etiqueta cada muestra como BUY / SELL / HOLD usando reglas simples
 *  - Entrena un árbol de decisión J48 y lo usa para clasificar nuevos datos.
 *  - Expone la accuracy del modelo para mostrarla en la UI.
 */
public class WekaClassifier {

    /** Número mínimo de muestras antes de entrenar */
    private static final int MIN_SAMPLES   = 30;

    /** Tamaño del buffer de histórico */
    private static final int BUFFER_SIZE   = 200;

    /** Umbral para etiquetar BUY (%): si precio sube +N% en la siguiente ventana */
    private static final double BUY_THRESHOLD  =  0.50;

    /** Umbral para etiquetar SELL (%): si precio baja -N% */
    private static final double SELL_THRESHOLD = -0.50;


    private J48 classifier;
    private Instances dataset;
    private ArrayList<Attribute> attributes;
    private boolean isTrained = false;
    private double lastAccuracy = 0.0;

    /** Buffer circular de MarketData recibidos */
    private final Queue<MarketData> buffer = new LinkedList<>();

    public WekaClassifier() {
        buildDatasetStructure();
    }

    /**
     * Construye la estructura del dataset (equivale a la cabecera .arff).
     * Features:
     *   - price: precio actual
     *   - change5m: variación % últimos 5 min
     *   - change10m: variación % últimos 10 min
     *   - change30m: variación % últimos 30 min
     *   - volume_norm: volumen 24h normalizado (log10)
     * Clase:
     *   - signal: {BUY, SELL, HOLD}
     */
    private void buildDatasetStructure() {
        attributes = new ArrayList<>();
        attributes.add(new Attribute("price"));
        attributes.add(new Attribute("change5m"));
        attributes.add(new Attribute("change10m"));
        attributes.add(new Attribute("change30m"));
        attributes.add(new Attribute("volume_norm"));

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("BUY");
        classValues.add("SELL");
        classValues.add("HOLD");
        attributes.add(new Attribute("signal", classValues));

        dataset = new Instances("TradingDataset", attributes, BUFFER_SIZE);
        dataset.setClassIndex(dataset.numAttributes() - 1);
    }


    /**
     * Añade un nuevo MarketData al buffer y genera una instancia etiquetada
     * para el dataset de entrenamiento usando las reglas de dominio.
     * 
     * @param data  muestra recibida del AgenteAdquisicion
     */
    public void addSample(MarketData data) {
        // Mantener buffer de tamaño máximo (FIFO)
        if (buffer.size() >= BUFFER_SIZE) {
            buffer.poll();
        }
        buffer.add(data);

        // Construir instancia Weka
        Instance instance = buildInstance(data, labelSample(data));
        dataset.add(instance);

        // Mantener el dataset al mismo tamaño que el buffer
        if (dataset.numInstances() > BUFFER_SIZE) {
            dataset.delete(0);
        }

        System.out.println("[Weka] Buffer: " + buffer.size() + " muestras | "
                + "Última etiqueta: " + labelSample(data));
    }

    /**
     * Regla de etiquetado heurística.
     *   Si Δ30m >  BUY_THRESHOLD  -> BUY
     *   Si Δ30m < SELL_THRESHOLD  -> SELL
     *   En caso contrario  -> HOLD
     *
     * También considera Δ10m para refinar la señal en escenarios intermedios.
     */
    private String labelSample(MarketData data) {
        double change30 = data.getPriceChange30m();
        double change10 = data.getPriceChange10m();

        if (change30 > BUY_THRESHOLD && change10 > 0) {
            return "BUY";
        } else if (change30 < SELL_THRESHOLD && change10 < 0) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }

    /**
     * Construye una instancia Weka a partir de un MarketData.
     */
    private Instance buildInstance(MarketData data, String label) {
        Instance inst = new DenseInstance(attributes.size());
        inst.setDataset(dataset);

        inst.setValue(attributes.get(0), data.getPrice());
        inst.setValue(attributes.get(1), data.getPriceChange5m());
        inst.setValue(attributes.get(2), data.getPriceChange10m());
        inst.setValue(attributes.get(3), data.getPriceChange30m());
        inst.setValue(attributes.get(4), Math.log10(data.getVolume24h() + 1));

        if (label != null) {
            inst.setClassValue(label);
        }
        return inst;
    }


    /**
     * Entrena el árbol de decisión J48 con el dataset actual.
     * Se llama automáticamente cuando hay suficientes muestras.
     */
    public void trainModel() {
        if (dataset.numInstances() < MIN_SAMPLES) {
            System.out.println("[Weka] Muestras insuficientes para entrenar ("
                    + dataset.numInstances() + "/" + MIN_SAMPLES + ")");
            return;
        }

        try {
            // J48 con poda activada
            classifier = new J48();
            classifier.setUnpruned(false);

            Instances trainData = new Instances(dataset);

            // Entrenamiento sobre todos los datos disponibles
            classifier.buildClassifier(trainData);

            // Evaluación con cross-validation
            J48 evalClassifier = new J48();
            evalClassifier.setUnpruned(false);
            Evaluation eval = new Evaluation(trainData);
            eval.crossValidateModel(evalClassifier, trainData, 10, new Random(42));

            lastAccuracy = eval.pctCorrect() / 100.0;
            isTrained = true;

            System.out.println("[Weka] Modelo J48 entrenado.");
            System.out.println("[Weka] Árbol:\n" + classifier.toString());
            System.out.printf("[Weka] Accuracy (CV-10): %.1f%% | Instancias: %d%n",
            		eval.pctCorrect(), trainData.numInstances());
            System.out.println("[Weka] Matriz de confusión:\n" + eval.toMatrixString());

        } catch (Exception e) {
            System.err.println("[Weka] ERROR al entrenar: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Clasifica un nuevo MarketData y devuelve la señal de trading.
     * Si el modelo no está entrenado, aplica las reglas heurísticas directamente.
     *
     * @param data  datos de mercado recibidos
     * @return TradingSignal con acción BUY/SELL/HOLD y confianza
     */
    public TradingSignal classify(MarketData data) {
        // Añadir al histórico y re-entrenar si es necesario
        addSample(data);
        if (!isTrained || buffer.size() % 10 == 0) {
            trainModel();
        }

        Action action;
        String justification;

        if (isTrained) {
            try {
                // Crear instancia sin etiqueta para clasificar
                Instance instance = buildInstance(data, null);
                // Instancia temporal
                Instances tempDataset = new Instances(dataset, 0);
                tempDataset.add(instance);
                tempDataset.setClassIndex(tempDataset.numAttributes() - 1);
                Instance toClassify = tempDataset.instance(0);

                double classIndex = classifier.classifyInstance(toClassify);
                String classLabel = dataset.classAttribute().value((int) classIndex);

                action = Action.valueOf(classLabel);
                justification = buildJustification(data, classLabel, true);

            } catch (Exception e) {
                System.err.println("[Weka] ERROR clasificando, usando reglas: " + e.getMessage());
                String heuristic = labelSample(data);
                action = Action.valueOf(heuristic);
                justification = buildJustification(data, heuristic, false);
            }
        } else {
            // Sin modelo entrenado aún: reglas heurísticas puras
            String heuristic = labelSample(data);
            action = Action.valueOf(heuristic);
            justification = buildJustification(data, heuristic, false);
        }

        return new TradingSignal(action, data.getSymbol(), data.getPrice(), lastAccuracy, justification);
    }

    /**
     * Genera un texto de justificación legible para la UI.
     */
    private String buildJustification(MarketData data, String label, boolean modelUsed) {
        String src = modelUsed ? "J48" : "heurística";
        switch (label) {
            case "BUY":
                return String.format("%s: Δ10m=+%.2f%% Δ30m=+%.2f%%",
                        src, data.getPriceChange10m(), data.getPriceChange30m());
            case "SELL":
                return String.format("%s: Δ10m=%.2f%% Δ30m=%.2f%%",
                        src, data.getPriceChange10m(), data.getPriceChange30m());
            default:
                return String.format("%s: mercado lateral Δ30m=%.2f%%",
                        src, data.getPriceChange30m());
        }
    }

    // Getters

    public boolean isTrained() { return isTrained; }
    public double getLastAccuracy() { return lastAccuracy; }
    public int getSampleCount() { return buffer.size(); }
    public int getMinSamples() { return MIN_SAMPLES; }
