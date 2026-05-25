package es.upm.trading.agents;

import es.upm.trading.behaviours.ForecastResultBehaviour;

import java.util.Map;

import es.upm.trading.behaviours.ForecastBehaviour;
import es.upm.trading.behaviours.UpdateUIBehaviour;
import es.upm.trading.model.PredictionRequest;
import es.upm.trading.ui.DashboardFrame;
import es.upm.trading.utils.Utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

/*
 * Agente de visualización / interfaz de usuario.
 *
 * Responsabilidades:
 *   1. Registrarse en el DF como receptor de actualizaciones de UI.
 *   2. Lanzar el JFrame Swing (DashboardFrame) en un hilo independiente.
 *   3. Escuchar mensajes INFORM con MarketData y TradingSignal (UpdateUIBehaviour).
 *   4. Delegar las actualizaciones de UI al EDT de Swing.
 *
 * Temas de clase:
 *   - Integración Swing con JADE: lanzar JFrame en hilo separado (PDF5, Ejemplo Weka)
 *   - OneShotBehaviour para registro en DF
 *   - CyclicBehaviour con MessageTemplate INFORM (UpdateUIBehaviour)
 *
 * Requisitos del enunciado cubiertos:
 *   - Agente de visualización con interfaz de usuario
 *   - Comportamientos JADE
 *   - Consumo de servicios vía DF
 */
@SuppressWarnings("serial")
public class AgenteUI extends Agent {

	private DashboardFrame dashboard;
	private String moneda = "bitcoin";

	private void leerParametros() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			moneda = ((String) args[0]).toLowerCase();
		}
	}

	private void registrarUI() {
	     Utils.registerService(this,Utils.SERVICE_UI, "Dashboard de visualización de trading");
	}

	private String obtenerNombreMoneda(String monedaId) {
		String nombre = monedaId;
		Map<String, String> MONEDAS= es.upm.trading.utils.Utils.getAllCoins();

		for (Map.Entry<String, String> entrada : MONEDAS.entrySet()) {
			if (entrada.getValue().equals(monedaId)) {
				nombre = entrada.getKey();
				break;
			}
		}
		return nombre;
	}

	private void cambiarMoneda(String monedaId) {
		AgenteAdquisicion adquisicion = (AgenteAdquisicion) Utils.findAgentObject(AgenteUI.this, Utils.SERVICE_MARKET);

		if (adquisicion != null)
			adquisicion.setActiveCoin(monedaId);

		String nombreMoneda = obtenerNombreMoneda(monedaId);
		dashboard.switchToCoin(monedaId, nombreMoneda);
	}

	private void crearUI() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dashboard = new DashboardFrame(moneda);

				dashboard.setOnCoinSelected(monedaId -> cambiarMoneda(monedaId));

				System.out.println("[AgenteUI] Dashboard iniciado.");

				dashboard.getPredictionPanel().setAgente(AgenteUI.this);

				UpdateUIBehaviour comportamientoUI = new UpdateUIBehaviour(AgenteUI.this, dashboard);
				addBehaviour(comportamientoUI);

				ForecastResultBehaviour comportamientoResultado = new ForecastResultBehaviour(AgenteUI.this, dashboard.getPredictionPanel());
				addBehaviour(comportamientoResultado);
			}
		});
	}

	@Override
	protected void setup() {
		System.out.println("[AgenteUI] Iniciando...");

		leerParametros();
		registrarUI(); // 1. Registro en el DF (OneShotBehaviour)
		crearUI(); // 2. Lanzar interfaz Swing en hilo independiente (EDT)

		System.out.println("[AgenteUI] Esperando mensajes INFORM...");
	}

	@Override
	protected void takeDown() {
		Utils.deregisterService(this);
		if (dashboard != null) {
			dashboard.dispose();
		}
		System.out.println("[AgenteUI] Agente terminado.");
	}

	/*
	 * Envía una petición de predicción al AgentePredictor.
	 * Llamado desde PredictionPanel cuando el usuario pulsa el botón.
	 *
	 * @param coinId     moneda a predecir
	 * @param stepsAhead 1, 3 o 5 intervalos
	 */
	public void sendForecastRequest(String coinId, int stepsAhead) {
		AID predictor = Utils.findAgent(this, Utils.SERVICE_PREDICTOR);
		if (predictor == null) {
			System.err.println("[AgenteUI] AgentePredictor no encontrado en DF.");
			return;
		}
		PredictionRequest req = new PredictionRequest(coinId, stepsAhead);
		Utils.sendRequest(this, predictor, req, ForecastBehaviour.FORECAST_ONTOLOGY);
		System.out.println("[AgenteUI] Petición de predicción enviada: " + req);
	}
}

