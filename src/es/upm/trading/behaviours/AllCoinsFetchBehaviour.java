package es.upm.trading.behaviours;

import es.upm.trading.model.MarketData;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.utils.Utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
 * Comportamiento único que coordina la adquisición de todas las monedas
 * en cada tick, garantizando que todas se actualicen al mismo tiempo.
 *
 * Funcionamiento:
 *   - Un solo TickerBehaviour con periodo INTERVAL (reloj global compartido).
 *   - En cada tick lanza las llamadas HTTP a la API en paralelo usando un
 *     ExecutorService, sin bloquear el hilo de JADE.
 *   - Cuando todas las llamadas terminan, procesa los resultados:
 *       · Guarda cada MarketData en MultiCoinDataStore.
 *       · Envía INFORM al AgenteUI.
 *       · Envía REQUEST al AgentePredictor.
 *
 * El paralelismo se hace en hilos del pool (no en el hilo de JADE), por lo
 * que JADE sigue siendo capaz de procesar mensajes durante las llamadas HTTP.
 * Las llamadas a send() se hacen de vuelta en el hilo de JADE (dentro de
 * onTick()) para respetar el modelo de concurrencia de JADE.
 */

public class AllCoinsFetchBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = 30L;

	// Intervalo entre rondas de actualización (todas las monedas a la vez) 
	public static final long INTERVAL = 3_000L;

	private final List<CoinFetcher> fetchers = new ArrayList<>();
	private final MultiCoinDataStore store   = MultiCoinDataStore.getInstance();

	/*
	 * Pool de hilos para las llamadas HTTP paralelas.
	 * Se usan hilos del pool, no el hilo de JADE, para no bloquear la plataforma.
	 * Tamaño = número de monedas para máximo paralelismo.
	 */
	
	private final ExecutorService pool;

	public AllCoinsFetchBehaviour(Agent agent, List<String> coinIds) {
		super(agent, INTERVAL);
		for (String id : coinIds) {
			fetchers.add(new CoinFetcher(id));
		}
		pool = Executors.newFixedThreadPool(coinIds.size());
		System.out.println("[AllCoinsFetch] Inicializado con " + fetchers.size() + 
				" monedas. Intervalo: " + INTERVAL / 1000 + "s.");
	}

	@Override
	protected void onTick() {
		System.out.println("[AllCoinsFetch] === Tick — actualizando "
				+ fetchers.size() + " monedas en paralelo ===");
		
		/*
		 * 1. Lanzar todas las llamadas HTTP en paralelo
		 * Cada Future representa la tarea de fetch de una moneda.
		 * Las llamadas HTTP se hacen en hilos del pool, no en el hilo de JADE.
		 */
		List<Future<MarketData>> futures = new ArrayList<>();
		for (CoinFetcher fetcher : fetchers) {
			futures.add(pool.submit(fetcher::fetch));
		}

		/* 
		 * 2. Recoger resultados y procesar 
		 * get() espera a que cada tarea termine. Como estamos en onTick()
		 * (hilo de JADE), esperamos aquí pero las llamadas HTTP ya están
		 * corriendo en paralelo, por lo que el tiempo total es el de la
		 * llamada más lenta, no la suma de todas.
		 */
		AID uiAgent        = Utils.findAgent(myAgent, Utils.SERVICE_UI);
		AID predictorAgent = Utils.findAgent(myAgent, Utils.SERVICE_PREDICTOR);

		for (int i = 0; i < fetchers.size(); i++) {
			try {
				MarketData data = futures.get(i).get(); // espera este fetch
				if (data == null) continue;

				String coinId = fetchers.get(i).getSymbol();
				System.out.println("[AllCoinsFetch] " + coinId + " → " + data.getPrice());

				// Guardar en el store
				store.addPoint(coinId, data);

				// Notificar a AgenteUI (actualiza contador y gráfica si es la activa)
				if (uiAgent != null) {
					Utils.sendInform(myAgent, uiAgent, data);
				}

				// Enviar al predictor para clasificación
				if (predictorAgent != null) {
					Utils.sendRequest(myAgent, predictorAgent, data);
				}

			} catch (Exception e) {
				System.err.println("[AllCoinsFetch] Error procesando "
						+ fetchers.get(i).getSymbol() + ": " + e.getMessage());
			}
		}

		System.out.println("[AllCoinsFetch] === Ronda completada ===");
	}

	/*
	 * Liberar el pool de hilos cuando el agente termina.
	 * Llamado desde AgenteAdquisicion.takeDown().
	 */
	public void shutdown() {
		pool.shutdownNow();
	}
}
