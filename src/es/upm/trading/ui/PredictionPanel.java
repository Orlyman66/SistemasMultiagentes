package es.upm.trading.ui;

import es.upm.trading.agents.AgenteUI;
import es.upm.trading.ml.PriceForecaster;
import es.upm.trading.model.PredictionResult;
import es.upm.trading.model.TradingSignal.Action;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Panel de predicción de precio futuro.
 *
 * Cuando el usuario pulsa "Predecir", este panel NO calcula nada localmente.
 * En su lugar delega en AgenteUI, que envía un mensaje ACL REQUEST al
 * AgentePredictor (ontología "trading-prediction"). El resultado llega de
 * vuelta como INFORM y es procesado por ForecastResultBehaviour, que llama
 * a showForecastResult() o showInsufficientData() en este panel.
 *
 * Flujo de mensajes:
 *   PredictionPanel → AgenteUI.sendForecastRequest()
 *     → [REQUEST trading-prediction] → AgentePredictor (ForecastBehaviour)
 *     → [INFORM  trading-prediction] → AgenteUI (ForecastResultBehaviour)
 *     → PredictionPanel.showForecastResult()
 */
public class PredictionPanel extends JPanel {

    private static final long serialVersionUID = 40L;

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

    /** Provee el coinId activo desde DashboardFrame */
    private final Supplier<String> activeCoinSupplier;

    /** Referencia al agente para enviar mensajes ACL */
    private AgenteUI agente;

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

        styleResultLabel(lblResultTitle,  13, Font.BOLD,  new Color(180, 180, 220));
        styleResultLabel(lblPriceCurrent, 12, Font.PLAIN, new Color(160, 160, 200));
        styleResultLabel(lblPricePred,    12, Font.BOLD,  new Color(80, 220, 160));
        styleResultLabel(lblChange,       12, Font.BOLD,  Color.WHITE);
        styleResultLabel(lblReco,         13, Font.BOLD,  Color.WHITE);
        styleResultLabel(lblConfidence,   12, Font.PLAIN, new Color(160, 160, 200));

        result.add(lblResultTitle);
        result.add(new label("Precio actual:",      new Color(140,140,180))); result.add(lblPriceCurrent);
        result.add(new label("Precio predicho:",    new Color(140,140,180))); result.add(lblPricePred);
        result.add(new label("Variación esperada:", new Color(140,140,180))); result.add(lblChange);
        result.add(new label("Recomendación:",      new Color(140,140,180))); result.add(lblReco);
        result.add(new label("Confianza (R²):",     new Color(140,140,180))); result.add(lblConfidence);

        result.setPreferredSize(new Dimension(400, 200));
        add(result, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    //  API pública
    // ─────────────────────────────────────────────────────────────

    /**
     * Registra la referencia al AgenteUI para poder enviarle mensajes.
     * Se llama desde AgenteUI después de crear el dashboard.
     */
    public void setAgente(AgenteUI agente) {
        this.agente = agente;
    }

    /**
     * Muestra el resultado de predicción recibido desde ForecastResultBehaviour.
     * Debe llamarse desde el EDT (SwingUtilities.invokeLater).
     */
    public void showForecastResult(PredictionResult res) {
        btnPredict.setEnabled(true);
        lblLoading.setText(" ");
        if (res == null) {
            showMessage("Error al calcular la predicción.", Color.RED);
            return;
        }
        displayResult(res);
    }

    /**
     * Muestra el mensaje de datos insuficientes.
     * Llamado desde ForecastResultBehaviour cuando el predictor no tiene datos.
     */
    public void showInsufficientData(int minSamples, int faltantes) {
        btnPredict.setEnabled(true);
        lblLoading.setText(" ");
        showMessage(
            "<html><center>Datos insuficientes para predecir.<br>"
            + "Se necesitan al menos <b>" + minSamples + " intervalos</b>.<br>"
            + "Faltan <b>" + faltantes + "</b> intervalos más.</center></html>",
            new Color(255, 160, 60));
    }

    /**
     * Muestra un mensaje de error genérico.
     */
    public void showError(String msg) {
        btnPredict.setEnabled(true);
        lblLoading.setText(" ");
        showMessage(msg, Color.RED);
    }

    // ─────────────────────────────────────────────────────────────
    //  Lógica de lanzamiento
    // ─────────────────────────────────────────────────────────────

    private void launchPrediction() {
        String coinId = activeCoinSupplier.get();
        if (coinId == null || coinId.isEmpty()) {
            showMessage("Selecciona una moneda primero.", new Color(255, 180, 80));
            return;
        }

        if (agente == null) {
            showMessage("Agente no disponible aún.", new Color(255, 180, 80));
            return;
        }

        int stepsAhead = STEPS[cmbHorizon.getSelectedIndex()];

        // Bloquear botón mientras se espera respuesta del predictor
        btnPredict.setEnabled(false);
        lblLoading.setText("Calculando...");
        clearResult();

        // Delegar en AgenteUI que enviará el REQUEST al AgentePredictor
        // La respuesta llegará a ForecastResultBehaviour, que llamará
        // a showForecastResult() o showInsufficientData() en este panel
        agente.sendForecastRequest(coinId, stepsAhead);
    }

    // ─────────────────────────────────────────────────────────────
    //  Visualización del resultado
    // ─────────────────────────────────────────────────────────────

    private void displayResult(PredictionResult res) {
        lblResultTitle.setText("Predicción a " + res.getStepsAhead() + " intervalos:");

        lblPriceCurrent.setText(String.format("$ %.4f", res.getCurrentPrice()));
        lblPricePred.setText(String.format("$ %.4f", res.getPredictedPrice()));

        double pct = res.getChangePct();
        String sign = pct >= 0 ? "+" : "";
        lblChange.setText(sign + String.format("%.2f%%", pct));
        lblChange.setForeground(pct > 0
                ? new Color(60, 220, 100)
                : pct < 0 ? new Color(240, 80, 80) : Color.WHITE);

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

        double conf = res.getConfidence();
        String confDesc = conf >= 0.80 ? "Alta" : conf >= 0.50 ? "Media" : "Baja";
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

    // ── Utilidades de estilo ─────────────────────────────────────

    private void styleResultLabel(JLabel lbl, int size, int style, Color fg) {
        lbl.setFont(new Font("Monospaced", style, size));
        lbl.setForeground(fg);
        lbl.setBackground(new Color(28, 28, 42));
    }

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
