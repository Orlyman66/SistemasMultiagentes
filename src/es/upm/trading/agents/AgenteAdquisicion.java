package es.upm.trading.agents;

import es.upm.trading.behaviours.AllCoinsFetchBehaviour;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agente de percepción / adquisición de información financiera.
 *
 * Usa un único AllCoinsFetchBehaviour que actualiza TODAS las monedas
 * en cada tick con el mismo reloj, garantizando sincronización completa.
 * Las llamadas HTTP se hacen en paralelo dentro del behaviour para que
 * el tiempo por ronda sea el de la llamada más lenta, no la suma de todas.
 */
public class AgenteAdquisicion extends Agent {

	private static final long serialVersionUID = 10L;

	private volatile String moneda = "bitcoin";
	private AllCoinsFetchBehaviour comportamiento;

	@Override
	protected void setup() {
		es.upm.trading.utils.Utils.generarMonedas();
		Map<String,String> ALL_COINS=es.upm.trading.utils.Utils.getAllCoins();
		Object[] listaparametros = getArguments();
		if (listaparametros != null && listaparametros.length > 0) {
			moneda = ((String) listaparametros[0]).toLowerCase();
		}

		System.out.println("[AgenteAdquisicion] Iniciando. Monedas: " + ALL_COINS.size());

		// Registro en el DF y en el registro estático para acceso directo desde UI
		Utils.registerAgentInstance(Utils.SERVICE_MARKET, this);
		Utils.registerService(this, Utils.SERVICE_MARKET, "Servicio de datos multi-moneda");

		// Esperar a que UI y Predictor estén registrados en el DF
		System.out.println("Entrando en espera");
		doWait(3000);
		System.out.println("Saliendo de espera");

		List<String> coinIds = new ArrayList<>(ALL_COINS.values());
		comportamiento= new AllCoinsFetchBehaviour(this, coinIds);
		addBehaviour(comportamiento);

		System.out.println("[AgenteAdquisicion] AllCoinsFetchBehaviour añadido."
				+ " Todas las monedas se actualizarán cada "
				+ AllCoinsFetchBehaviour.INTERVAL / 1000 + "s simultáneamente.");
	}

	public void setActiveCoin(String coinId) {
		System.out.println("[AgenteAdquisicion] Activa: " + moneda + " -> " + coinId);
		moneda = coinId;
	}

	public String getActiveCoinId() { 
		return moneda; 
	}

	@Override
	protected void takeDown() {
		if (comportamiento != null) comportamiento.shutdown();
		Utils.deregisterService(this);
		System.out.println("[AgenteAdquisicion] Terminado.");
	}
}
