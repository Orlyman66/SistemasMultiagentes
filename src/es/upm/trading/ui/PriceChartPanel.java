package es.upm.trading.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Panel personalizado para pintar la gráfica de precios en tiempo real usando paintComponent y Graphics2D sin usar librerías externas.
*/
public class PriceChartPanel extends JPanel {

	private static final long serialVersionUID = 8L;

	private static final int MAX_POINTS     = 60;   // puntos visibles en la gráfica
	private static final int PADDING_LEFT   = 70;
	private static final int PADDING_RIGHT  = 20;
	private static final int PADDING_TOP    = 30;
	private static final int PADDING_BOTTOM = 40;

	private final List<Double> prices    = new ArrayList<>();
	private final List<Double> changes   = new ArrayList<>(); // variación 30m para color

	public PriceChartPanel() {
		setBackground(new Color(18, 18, 30));
		setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(60, 60, 80)),
				"Precio en tiempo real",
				javax.swing.border.TitledBorder.LEFT,
				javax.swing.border.TitledBorder.TOP,
				new Font("SansSerif", Font.PLAIN, 12),
				new Color(150, 150, 200)));
	}

	public void clear() { // Limpia todos los puntos de la gráfica (al cambiar de moneda).
		prices.clear();
		changes.clear();
		repaint();
	}

	/*
	 * Método para cargar los precios guardados cuando se cambia de criptomoneda
	 */
	public void loadSeries(java.util.List<Double> historicPrices,
			java.util.List<Double> historicChanges) {
		prices.clear();
		changes.clear();
		// Mostrar solo los últimos MAX_POINTS puntos
		int start = Math.max(0, historicPrices.size() - MAX_POINTS);
		for (int i = start; i < historicPrices.size(); i++) {
			prices.add(historicPrices.get(i));
		}
		int startC = Math.max(0, historicChanges.size() - MAX_POINTS);
		for (int i = startC; i < historicChanges.size(); i++) {
			changes.add(historicChanges.get(i));
		}
		repaint();
	}

	/*
	 * Añade un nuevo punto de precio.
	 */
	public void addPrice(double price, double change30m) {
		prices.add(price);
		changes.add(change30m);
		if (prices.size() > MAX_POINTS) {
			prices.remove(0);
			changes.remove(0);
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (prices.size() < 2) {
			drawWaiting(g);
			return;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth()  - PADDING_LEFT - PADDING_RIGHT;
		int h = getHeight() - PADDING_TOP  - PADDING_BOTTOM;

		double minP = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0);
		double maxP = prices.stream().mapToDouble(Double::doubleValue).max().orElse(1);
		double range = (maxP - minP) == 0 ? 1 : (maxP - minP);

		// Eje Y
		g2.setColor(new Color(80, 80, 100));
		g2.drawLine(PADDING_LEFT, PADDING_TOP, PADDING_LEFT, PADDING_TOP + h);
		g2.drawLine(PADDING_LEFT, PADDING_TOP + h, PADDING_LEFT + w, PADDING_TOP + h);

		// Etiquetas eje Y (4 niveles)
		g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
		g2.setColor(new Color(130, 130, 160));
		for (int i = 0; i <= 4; i++) {
			double val  = minP + (range * i / 4.0);
			int    yPos = PADDING_TOP + h - (int)(h * i / 4.0);
			g2.drawLine(PADDING_LEFT - 4, yPos, PADDING_LEFT, yPos);
			g2.drawString(String.format("%.0f", val), 4, yPos + 4);
		}

		// Área rellena bajo la curva
		int n = prices.size();
		int[] xPts = new int[n + 2];
		int[] yPts = new int[n + 2];

		for (int i = 0; i < n; i++) {
			xPts[i] = PADDING_LEFT + (int)((double) i / (MAX_POINTS - 1) * w);
			yPts[i] = PADDING_TOP  + h - (int)((prices.get(i) - minP) / range * h);
		}
		xPts[n]     = xPts[n - 1];
		yPts[n]     = PADDING_TOP + h;
		xPts[n + 1] = xPts[0];
		yPts[n + 1] = PADDING_TOP + h;

		// Color del área: verde si tendencia alcista, rojo si bajista
		double lastChange = changes.isEmpty() ? 0 : changes.get(changes.size() - 1);
		Color areaColor = lastChange >= 0
				? new Color(40, 200, 100, 40)
						: new Color(220, 60, 60, 40);
		g2.setColor(areaColor);
		g2.fillPolygon(xPts, yPts, n + 2);

		// Línea de precio
		Color lineColor = lastChange >= 0 ? new Color(80, 220, 130) : new Color(240, 80, 80);
		g2.setColor(lineColor);
		g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < n - 1; i++) {
			g2.drawLine(xPts[i], yPts[i], xPts[i + 1], yPts[i + 1]);
		}

		// Punto actual (último precio)
		int lastX = xPts[n - 1];
		int lastY = yPts[n - 1];
		g2.setColor(Color.WHITE);
		g2.fillOval(lastX - 4, lastY - 4, 8, 8);
		g2.setColor(lineColor);
		g2.setStroke(new BasicStroke(1.5f));
		g2.drawOval(lastX - 4, lastY - 4, 8, 8);

		// Etiqueta precio actual
		g2.setFont(new Font("Monospaced", Font.BOLD, 11));
		g2.setColor(Color.WHITE);
		g2.drawString(String.format("$ %.2f", prices.get(n - 1)), lastX + 6, lastY + 4);
	}

	private void drawWaiting(Graphics g) {
		g.setColor(new Color(100, 100, 130));
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		int cx = getWidth()  / 2;
		int cy = getHeight() / 2;
		g.drawString("Esperando datos del mercado...", cx - 110, cy);
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.drawString("(el agente adquisición actualizará en 30 s)", cx - 130, cy + 22);
	}
}
