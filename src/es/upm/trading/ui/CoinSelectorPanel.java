package es.upm.trading.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/*
 * Panel lateral con el listado predefinido de criptomonedas.
 *
 * 1. Se muestra para cada moneda cuántos cambios se han acumulado desde el inicio, actualizándose en tiempo real conforme llegan ticks de AgenteAdquisicion.
 * 2. Al seleccionar una moneda, el onCoinSelected notifica a AgenteUI para que cambie la moneda activa y se cargue su serie histórica en la interfaz.
 */
@SuppressWarnings("serial")
public class CoinSelectorPanel extends JPanel {

	private Map<String, String> MONEDAS;

	//Componentes visuales de JavaSwing
	private final JList<String>            coinList;
	private final DefaultListModel<String> listModel;
	private final JButton                  btnFollow;
	private final JLabel                   lblActive;

	private final Consumer<String> onCoinSelected;

	private String activeCoinId = "bitcoin";

	private final ConcurrentHashMap<String, Integer> pointCounts = new ConcurrentHashMap<>();

	public CoinSelectorPanel(Consumer<String> onCoinSelected) {
		this.onCoinSelected = onCoinSelected;
		es.upm.trading.utils.Utils.generarMonedas();
		MONEDAS= es.upm.trading.utils.Utils.getAllCoins();

		setLayout(new BorderLayout(4, 6));
		setPreferredSize(new Dimension(180, 0));
		setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(60, 60, 80)),
				"Criptomonedas",
				TitledBorder.LEFT, TitledBorder.TOP,
				new Font("SansSerif", Font.PLAIN, 12),
				new Color(150, 150, 200)));
		setBackground(new Color(22, 22, 36));

		listModel = new DefaultListModel<>();
		for(String name:MONEDAS.keySet()) {
			listModel.addElement(name);
		}
		
		coinList = new JList<>(listModel);
		coinList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		coinList.setFont(new Font("SansSerif", Font.PLAIN, 13));
		coinList.setBackground(new Color(30, 30, 46));
		coinList.setForeground(new Color(200, 200, 220));
		coinList.setSelectionBackground(new Color(60, 100, 180));
		coinList.setSelectionForeground(Color.WHITE);
		coinList.setFixedCellHeight(30);
		coinList.setCellRenderer(buildCellRenderer());
		coinList.setSelectedIndex(0); // seleccionar Bitcoin por defecto

		coinList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) handleSelection();
			}
		});

		JScrollPane scroll = new JScrollPane(coinList);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setBackground(new Color(30, 30, 46));
		add(scroll, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout(0, 4));
		bottom.setBackground(new Color(22, 22, 36));
		bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 6, 4));

		lblActive = new JLabel("Mostrando: Bitcoin", SwingConstants.CENTER);
		lblActive.setFont(new Font("Monospaced", Font.PLAIN, 10));
		lblActive.setForeground(new Color(100, 200, 130));

		btnFollow = new JButton("▶  Mostrar");
		btnFollow.setFont(new Font("SansSerif", Font.BOLD, 12));
		btnFollow.setBackground(new Color(40, 120, 80));
		btnFollow.setForeground(Color.WHITE);
		btnFollow.setFocusPainted(false);
		btnFollow.setBorderPainted(false);
		btnFollow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnFollow.addActionListener(e -> handleSelection());

		bottom.add(btnFollow, BorderLayout.CENTER);
		bottom.add(lblActive, BorderLayout.SOUTH);
		add(bottom, BorderLayout.SOUTH);
	}

	// Lógica de selección

	private void handleSelection() {
		String selectedName = coinList.getSelectedValue();
		es.upm.trading.utils.Utils.generarMonedas();
		MONEDAS= es.upm.trading.utils.Utils.getAllCoins();

		if (selectedName == null) return;

		String coinId = MONEDAS.get(selectedName);
		if (coinId == null || coinId.equals(activeCoinId)) return;

		activeCoinId = coinId;
		lblActive.setText("Mostrando: " + selectedName);
		coinList.repaint();

		onCoinSelected.accept(coinId);
	}

	// API pública

	public void updatePointCount(String coinId, int count) {
		pointCounts.put(coinId, count);
		coinList.repaint();
	}

	public void setActiveCoin(String coinId) {
		this.activeCoinId = coinId;
		es.upm.trading.utils.Utils.generarMonedas();
		MONEDAS= es.upm.trading.utils.Utils.getAllCoins();
		for(Map.Entry<String, String> entrada: MONEDAS.entrySet()) {
			if(entrada.getValue().equals(coinId)) {
				lblActive.setText("Mostrando: " + entrada.getKey());
				break;
			}
		}
		coinList.repaint();
	}

	public String getActiveName() {
		es.upm.trading.utils.Utils.generarMonedas();
		MONEDAS= es.upm.trading.utils.Utils.getAllCoins();

		for(Map.Entry<String, String> entrada: MONEDAS.entrySet()) {
			if(entrada.getValue().equals(activeCoinId)) {
				return entrada.getKey();
			}
		}
		return activeCoinId;
	}

	// Render

	private DefaultListCellRenderer buildCellRenderer() {
		return new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int indice, boolean isSelected, boolean cellHasFocus) {
				es.upm.trading.utils.Utils.generarMonedas();
				MONEDAS=es.upm.trading.utils.Utils.getAllCoins();

				JLabel lbl = (JLabel) super.getListCellRendererComponent(
						list, value, indice, isSelected, cellHasFocus);

				String name   = (String) value;
				String id     = MONEDAS.get(name);
				int    points = pointCounts.getOrDefault(id, 0);

				String numDatosMonedas = points > 0 ? " (" + points + ")" : " (–)";
				lbl.setText("  " + name + numDatosMonedas);
				lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

				if (!isSelected) {
					lbl.setBackground(indice % 2 == 0? new Color(30, 30, 46): new Color(36, 36, 54));
					lbl.setForeground(new Color(210, 210, 230));

					if (id != null && id.equals(activeCoinId)) {
						lbl.setForeground(new Color(80, 220, 130));
						lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
					} else if (points > 0) {
						lbl.setForeground(new Color(200, 220, 255));
					}
				}
				return lbl;
			}
		};
	}
}
