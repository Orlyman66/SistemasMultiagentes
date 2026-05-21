package es.upm.trading.ui;

import es.upm.trading.ml.PriceForecaster;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.model.PredictionResult;
import es.upm.trading.model.TradingSignal.Action;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Panel de predicción de precio futuro.
 *
 * Permite al usuario seleccionar un horizonte (10, 25 o 50 intervalos)
 * y lanzar una predicción sobre la moneda activa pulsando un botón.
 *
 * La predicción se ejecuta en un SwingWorker para no bloquear el EDT.
 * El resultado se muestra en el propio panel con precio predicho,
 * variación %, recomendación coloreada y nivel de confianza (R²).
 *
 * Si no hay suficientes datos se muestra un mensaje informativo.
 */
public class PredictionPanel extends JPanel {

    private static final long serialVersionUID = 40L;

    // ── Opciones de horizonte ────────────────────────────────────
    private static final int[] STEPS = {1, 3, 5};

    // ── Componentes ──────────────────────────────────────────────
    private final JComboBox<String> cmbHorizon;
    private final JButton           btnPredict;

    private final JLabel lblResultTitle  = new JLabel(" ");
    private final JLabel lblPriceCurrent = new JLabel(" ");
    private final JLabel lblPricePred    = new JLabel(" ");
    private final JLabel lblChange       = new JLabel(" ");
    private final JLabel lblReco         = new JLabel(" ");
    private final JLabel lblConfidence   = new JLabel(" ");
    private final JLabel lblLoading      = new JLabel(" ");

    /** Provee el coinId activo (lambda inyectada desde DashboardFrame) */
    private final Supplier<String> activeCoinSupplier;

    private final PriceForecaster forecaster = new PriceForecaster();

    public PredictionPanel(Supplier<String> activeCoinSupplier) {
        this.activeCoinSupplier = activeCoinSupplier;

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 120)),
                "Predicción de precio",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.PLAIN, 12),
                new Color(150, 150, 200)));
        setBackground(new Color(22, 22, 36));
        setPreferredSize(new Dimension(400, 210));

        // ── Fila de controles ────────────────────────────────────
        cmbHorizon = new JComboBox<>(new String[]{
                "Próxima variación (1)",
                "Próximas 3 variaciones",
                "Próximas 5 variaciones"
        });
        cmbHorizon.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cmbHorizon.setBackground(new Color(40, 40, 60));
        cmbHorizon.setForeground(Color.WHITE);

        btnPredict = new JButton("▶Predecir");
        btnPredict.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnPredict.setBackground(new Color(70, 100, 200));
        btnPredict.setForeground(Color.WHITE);
        btnPredict.setFocusPainted(false);
        btnPredict.setBorderPainted(false);
        btnPredict.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPredict.addActionListener(e -> launchPrediction());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setBackground(new Color(22, 22, 36));
        controls.add(new label("Horizonte:", Color.LIGHT_GRAY));
        controls.add(cmbHorizon);
        controls.add(btnPredict);

        lblLoading.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblLoading.setForeground(new Color(150, 150, 200));
        controls.add(lblLoading);

        add(controls, BorderLayout.NORTH);

        // ── Panel de resultado ───────────────────────────────────
        JPanel result = new JPanel(new GridLayout(0, 1, 6, 4));
        result.setBackground(new Color(28, 28, 42));
        result.setBorder(BorderFactory.createEmptyBorder(0, 12, 3, 12));

        styleResultLabel(lblResultTitle, 13, Font.BOLD, new Color(180, 180, 220));
        styleResultLabel(lblPriceCurrent, 12, Font.PLAIN, new Color(160, 160, 200));
        styleResultLabel(lblPricePred,    12, Font.BOLD,  new Color(80, 220, 160));
        styleResultLabel(lblChange,       12, Font.BOLD,  Color.WHITE);
        styleResultLabel(lblReco,         13, Font.BOLD,  Color.WHITE);
        styleResultLabel(lblConfidence,   12, Font.PLAIN, new Color(160, 160, 200));

        result.add(lblResultTitle);
        result.add(new label("Precio actual:",   new Color(140,140,180))); result.add(lblPriceCurrent);
        result.add(new label("Precio predicho:", new Color(140,140,180))); result.add(lblPricePred);
        result.add(new label("Variación esperada:", new Color(140,140,180))); result.add(lblChange);
        result.add(new label("Recomendación:",  new Color(140,140,180))); result.add(lblReco);
        result.add(new label("Confianza (R²):", new Color(140,140,180))); result.add(lblConfidence);

        result.setPreferredSize(new Dimension(400, 200)); 
        add(result, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    //  Lógica de predicción
    // ─────────────────────────────────────────────────────────────

    private void launchPrediction() {
        String coinId = activeCoinSupplier.get();
        if (coinId == null || coinId.isEmpty()) {
            showMessage("Selecciona una moneda primero.", new Color(255, 180, 80));
            return;
        }

        int stepsAhead = STEPS[cmbHorizon.getSelectedIndex()];
        List<Double> prices = MultiCoinDataStore.getInstance().getPrices(coinId);

        // Comprobar datos mínimos antes de lanzar el worker
        if (prices.size() < PriceForecaster.getMinSamples()) {
            int missing = PriceForecaster.getMinSamples() - prices.size();
            showMessage(
                "<html><center>Datos insuficientes para predecir.<br>"
                + "Se necesitan al menos <b>" + PriceForecaster.getMinSamples()
                + " intervalos</b>.<br>"
                + "Faltan <b>" + missing + "</b> intervalos más.</center></html>",
                new Color(255, 160, 60));
            return;
        }

        // Bloquear botón mientras se calcula
        btnPredict.setEnabled(false);
        lblLoading.setText("Calculando...");
        clearResult();

        // Ejecutar en background para no bloquear el EDT
        SwingWorker<PredictionResult, Void> worker = new SwingWorker<PredictionResult, Void>() {
            @Override
            protected PredictionResult doInBackground() {
                return forecaster.predict(coinId, prices, stepsAhead);
            }

            @Override
            protected void done() {
                btnPredict.setEnabled(true);
                lblLoading.setText(" ");
                try {
                    PredictionResult res = get();
                    if (res == null) {
                        showMessage("Error al calcular la predicción.", Color.RED);
                    } else {
                        displayResult(res);
                    }
                } catch (Exception ex) {
                    showMessage("Error: " + ex.getMessage(), Color.RED);
                }
            }
        };
        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────
    //  Visualización del resultado
    // ─────────────────────────────────────────────────────────────

    private void displayResult(PredictionResult res) {
        int steps = res.getStepsAhead();
        lblResultTitle.setText("Predicción a " + steps + " intervalos:");

        lblPriceCurrent.setText(String.format("$ %.4f", res.getCurrentPrice()));
        lblPricePred.setText(String.format("$ %.4f", res.getPredictedPrice()));

        // Variación con color
        double pct = res.getChangePct();
        String sign = pct >= 0 ? "+" : "";
        lblChange.setText(sign + String.format("%.2f%%", pct));
        lblChange.setForeground(pct > 0
                ? new Color(60, 220, 100)
                : pct < 0 ? new Color(240, 80, 80) : Color.WHITE);

        // Recomendación con badge de color
        Action reco = res.getRecommendation();
        lblReco.setText("  " + reco.name() + "  ");
        lblReco.setOpaque(true);
        switch (reco) {
            case BUY:
                lblReco.setBackground(new Color(40, 167, 69));
                lblReco.setForeground(Color.WHITE);
                break;
            case SELL:
                lblReco.setBackground(new Color(220, 53, 69));
                lblReco.setForeground(Color.WHITE);
                break;
            default:
                lblReco.setBackground(new Color(90, 90, 110));
                lblReco.setForeground(Color.WHITE);
        }

        // Confianza con descripción cualitativa
        double conf = res.getConfidence();
        String confDesc = conf >= 0.80 ? "Alta"
                        : conf >= 0.50 ? "Media"
                        : "Baja";
        lblConfidence.setText(String.format("%.1f%%  (%s)", conf * 100, confDesc));
        lblConfidence.setForeground(conf >= 0.80
                ? new Color(60, 220, 100)
                : conf >= 0.50 ? new Color(255, 200, 60) : new Color(240, 120, 80));
    }

    private void showMessage(String msg, Color color) {
        clearResult();
        lblResultTitle.setText("<html>" + msg + "</html>");
        lblResultTitle.setForeground(color);
    }

    private void clearResult() {
        lblResultTitle.setText(" ");
        lblResultTitle.setForeground(new Color(180, 180, 220));
        lblPriceCurrent.setText(" ");
        lblPricePred.setText(" ");
        lblChange.setText(" ");
        lblReco.setText(" ");
        lblReco.setOpaque(false);
        lblConfidence.setText(" ");
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilidades de estilo
    // ─────────────────────────────────────────────────────────────

    private void styleResultLabel(JLabel lbl, int size, int style, Color fg) {
        lbl.setFont(new Font("Monospaced", style, size));
        lbl.setForeground(fg);
        lbl.setBackground(new Color(28, 28, 42));
    }

    /** JLabel inline con texto y color para las etiquetas de fila */
    private static class label extends JLabel {
        label(String text, Color fg) {
            super(text);
            setFont(new Font("SansSerif", Font.PLAIN, 12));
            setForeground(fg);
            setBackground(new Color(28, 28, 42));
            setOpaque(true);
        }
    }
}
 