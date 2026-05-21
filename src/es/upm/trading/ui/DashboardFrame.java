package es.upm.trading.ui;

import es.upm.trading.model.MarketData;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.model.SignalHistoryStore;
import es.upm.trading.model.TradingSignal;
import es.upm.trading.model.TradingSignal.Action;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ventana principal del sistema de trading.
 *
 * Paneles:
 *   ┌─────────────────────────────────────────────┐
 *   │  HEADER: símbolo + precio actual + señal    │
 *   ├─────────────┬───────────────────────────────┤
 *   │  GRÁFICA    │  TABLA DE SEÑALES             │
 *   │  de precios │  (scroll)                     │
 *   ├─────────────┴───────────────────────────────┤
 *   │  BARRA DE ESTADO: muestras / accuracy       │
 *   └─────────────────────────────────────────────┘
 *
 * Temas de clase:
 *   - JFrame, JPanel, JTable, JLabel, BorderLayout (PDF5)
 *   - Integración con agente mediante hilo MainGUI (PDF5, Ejemplo Weka)
 *   - paintComponent para gráfica sin librerías externas
 */
public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 7L;

    // ── Componentes UI ───────────────────────────────────────────
    private final JLabel  lblSymbol    = new JLabel("–");
    private final JLabel  lblPrice     = new JLabel("–");
    private final JLabel  lblSignal    = new JLabel("HOLD");
    private final JLabel  lblStatus    = new JLabel("Esperando datos...");

    private final PriceChartPanel   chartPanel;
    private final DefaultTableModel tableModel;
    private final JTable            signalTable;
    private final CoinSelectorPanel coinSelector;
    private final PredictionPanel   predictionPanel;

    // ── Datos ────────────────────────────────────────────────────
    private double latestPrice  = 0.0;
    private String activeCoinId = "";   // coin activa para pasarla al PredictionPanel
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    /**
     * Callback que conecta la UI con AgenteAdquisicion.
     * Se asigna desde AgenteUI tras crear el dashboard.
     * Cuando el usuario selecciona una moneda, se llama con su id CoinGecko.
     */
    private Consumer<String> onCoinSelected = coinId -> {}; // no-op por defecto

    public DashboardFrame(String initialCoinId) {
        super("Trading MAS — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);

        this.activeCoinId = initialCoinId;
        chartPanel      = new PriceChartPanel();
        tableModel      = buildTableModel();
        signalTable     = buildSignalTable();
        coinSelector    = new CoinSelectorPanel(id -> onCoinSelected.accept(id));
        predictionPanel = new PredictionPanel(() -> activeCoinId);
        coinSelector.setActiveCoin(initialCoinId);

        buildUI();
        setVisible(true);
    }

    /** Compatibilidad con código que llame al constructor sin argumento */
    public DashboardFrame() {
        this("bitcoin");
    }

    /** Registra el callback que se llama cuando el usuario cambia de moneda */
    public void setOnCoinSelected(Consumer<String> callback) {
        this.onCoinSelected = callback;
    }

    // ─────────────────────────────────────────────────────────────
    //  Construcción de la UI
    // ─────────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(4, 4));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);
        add(coinSelector,       BorderLayout.WEST);
        add(predictionPanel,    BorderLayout.EAST);
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        panel.setBackground(new Color(30, 30, 46));

        lblSymbol.setFont(new Font("Monospaced", Font.BOLD, 20));
        lblSymbol.setForeground(Color.WHITE);

        lblPrice.setFont(new Font("Monospaced", Font.BOLD, 26));
        lblPrice.setForeground(new Color(80, 220, 160));

        lblSignal.setFont(new Font("Monospaced", Font.BOLD, 18));
        lblSignal.setOpaque(true);
        lblSignal.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        setSignalBadge(Action.HOLD);

        JLabel titleLbl = new JLabel("Trading MAS");
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(150, 150, 170));

        panel.add(titleLbl);
        panel.add(lblSymbol);
        panel.add(lblPrice);
        panel.add(new JLabel(" "));
        panel.add(lblSignal);

        return panel;
    }

    private JSplitPane buildCenterPanel() {
        // Panel izquierdo: gráfica de precio
        chartPanel.setPreferredSize(new Dimension(500, 380));

        // Panel derecho: tabla de señales
        JScrollPane tableScroll = new JScrollPane(signalTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Historial de señales"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                chartPanel, tableScroll);
        split.setDividerLocation(540);
        split.setResizeWeight(0.65);
        return split;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lblStatus.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(100, 100, 120));
        bar.add(lblStatus, BorderLayout.WEST);
        return bar;
    }

    // ── Tabla de señales ─────────────────────────────────────────

    private DefaultTableModel buildTableModel() {
        String[] cols = {"Hora", "Símbolo", "Señal", "Precio", "Conf.", "Justificación"};
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable buildSignalTable() {
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // Colorear filas según la señal (columna 2)
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, col);
                if (!selected) {
                    Object signal = t.getModel().getValueAt(row, 2);
                    if ("BUY".equals(signal)) {
                        setBackground(new Color(210, 255, 220));
                        setForeground(new Color(0, 100, 30));
                    } else if ("SELL".equals(signal)) {
                        setBackground(new Color(255, 215, 215));
                        setForeground(new Color(140, 0, 0));
                    } else {
                        setBackground(Color.WHITE);
                        setForeground(Color.DARK_GRAY);
                    }
                }
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Anchos de columna
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        return table;
    }

    // ─────────────────────────────────────────────────────────────
    //  Métodos públicos llamados desde UpdateUIBehaviour (EDT)
    // ─────────────────────────────────────────────────────────────

    /**
     * Cambia la gráfica a la serie completa de una moneda.
     * Carga todos los puntos acumulados en MultiCoinDataStore desde el arranque.
     * Llamado cuando el usuario selecciona una moneda en CoinSelectorPanel.
     */
    public void switchToCoin(String coinId, String displayName) {
        this.activeCoinId = coinId;   // actualizar para PredictionPanel

        MultiCoinDataStore store = MultiCoinDataStore.getInstance();
        List<Double> prices  = store.getPrices(coinId);
        List<Double> changes = store.getChanges30m(coinId);

        chartPanel.loadSeries(prices, changes);

        // Actualizar header con el último precio disponible
        MarketData latest = store.getLatest(coinId);
        if (latest != null) {
            lblSymbol.setText(displayName.toUpperCase());
            lblPrice.setText(String.format("$ %.4f", latest.getPrice()));
            updateStatusBar(latest);
        } else {
            lblSymbol.setText(displayName.toUpperCase());
            lblPrice.setText("esperando...");
            lblStatus.setText("Acumulando datos de " + displayName + "...");
        }

        setSignalBadge(Action.HOLD);

        // Cargar las últimas 50 señales de esta moneda desde el historial
        loadSignalHistory(coinId);
    }

    /**
     * Rellena la tabla de señales con el historial almacenado para una moneda.
     * Muestra como máximo 50 filas (las más recientes primero).
     * Si hay menos de 50, muestra las que haya.
     */
    private void loadSignalHistory(String coinId) {
        tableModel.setRowCount(0); // limpiar tabla primero

        java.util.List<TradingSignal> signals =
                SignalHistoryStore.getInstance().getLastSignals(coinId, 50);

        if (signals.isEmpty()) {
            // Sin señales aún: dejar la tabla vacía con el badge en HOLD
            setSignalBadge(Action.HOLD);
            return;
        }

        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm:ss");
        for (TradingSignal s : signals) {
            tableModel.addRow(new Object[]{
                    fmt.format(new java.util.Date(s.getTimestamp())),
                    s.getSymbol(),
                    s.getAction().name(),
                    String.format("%.4f", s.getPrice()),
                    String.format("%.0f%%", s.getConfidence() * 100),
                    s.getJustification()
            });
        }

        // Mostrar badge de la señal más reciente (primera fila)
        setSignalBadge(signals.get(0).getAction());
    }

    /**
     * Actualiza el contador de puntos acumulados en el panel lateral
     * para una moneda concreta. Llamado por cada tick de cualquier moneda.
     */
    public void updateCoinPointCount(String coinId, int count) {
        coinSelector.updatePointCount(coinId, count);
    }

    /** Limpia la gráfica al cambiar de moneda (usado internamente). */
    public void clearChart() {
        chartPanel.clear();
        lblPrice.setText("–");
        lblSymbol.setText("–");
        setSignalBadge(Action.HOLD);
        lblStatus.setText("Cambiando de moneda...");
    }

    /**
     * Actualiza el precio en el header y añade el punto a la gráfica.
     * Solo se llama para la moneda activa.
     */
    public void updatePrice(MarketData data) {
        latestPrice = data.getPrice();
        lblSymbol.setText(data.getSymbol().toUpperCase());
        lblPrice.setText(String.format("$ %.4f", latestPrice));
        chartPanel.addPrice(latestPrice, data.getPriceChange30m());
        updateStatusBar(data);
    }

    /**
     * Añade una fila a la tabla de señales y actualiza el badge de señal.
     */
    public void addSignal(TradingSignal signal) {
        String hora   = sdf.format(new Date(signal.getTimestamp()));
        String conf   = String.format("%.0f%%", signal.getConfidence() * 100);
        tableModel.insertRow(0, new Object[]{
                hora,
                signal.getSymbol(),
                signal.getAction().name(),
                String.format("%.4f", signal.getPrice()),
                conf,
                signal.getJustification()
        });
        if (tableModel.getRowCount() > 100) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
        setSignalBadge(signal.getAction());
    }

    /** Actualiza el badge de señal activa en el header */
    public void setSignalBadge(Action action) {
        lblSignal.setText(action.name());
        switch (action) {
            case BUY:
                lblSignal.setBackground(new Color(40, 167, 69));
                lblSignal.setForeground(Color.WHITE);
                break;
            case SELL:
                lblSignal.setBackground(new Color(220, 53, 69));
                lblSignal.setForeground(Color.WHITE);
                break;
            default:
                lblSignal.setBackground(new Color(108, 117, 125));
                lblSignal.setForeground(Color.WHITE);
        }
    }

    /** Actualiza la barra de estado con info del tick recibido */
    public void updateStatusBar(MarketData data) {
        lblStatus.setText(String.format(
                "Último tick: %s | Δ5m: %.2f%% | Δ10m: %.2f%% | Δ30m: %.2f%% | Vol24h: %.1fB",
                sdf.format(new Date(data.getTimestamp())),
                data.getPriceChange5m(),
                data.getPriceChange10m(),
                data.getPriceChange30m(),
                data.getVolume24h() / 1_000_000_000.0
        ));
    }

    /** Actualiza el label de estado con info del clasificador */
    public void updateModelStats(int samples, int minSamples, double accuracy) {
        if (samples < minSamples) {
            lblStatus.setText(String.format(
                    "Recolectando muestras: %d/%d para entrenar J48...", samples, minSamples));
        } else {
            lblStatus.setText(String.format(
                    "Modelo J48 activo | Muestras: %d | Accuracy CV-10: %.1f%%",
                    samples, accuracy * 100));
        }
    }
}
