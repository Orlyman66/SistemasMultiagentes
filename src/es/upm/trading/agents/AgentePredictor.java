package es.upm.trading.agents;

import es.upm.trading.behaviours.AnalysisBehaviour;
import es.upm.trading.behaviours.ForecastBehaviour;
import es.upm.trading.behaviours.TradingStateFSM;
import es.upm.trading.ml.WekaClassifier;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;


/**
 * Agente predictor / inteligente del sistema de trading.
 *
 * Responsabilidades:
 *   1. Registrarse en el DF como proveedor del servicio "predictor".
 *   2. Inicializar el clasificador Weka J48 y la FSM de estados.
 *   3. Escuchar mensajes REQUEST con MarketData (AnalysisBehaviour).
 *   4. Clasificar los datos con J48 y emitir TradingSignal como INFORM.
 *   5. Mantener el estado actual de trading (BUY/SELL/HOLD) en la FSM.
 *
 * Requisitos del enunciado cubiertos:
 *   - Agente con capacidad de cálculo complejo / inteligente (árbol J48 + CV)
 *   - Comportamientos JADE: OneShotBehaviour + CyclicBehaviour + FSMBehaviour
 *   - Filtro de mensajes en modo bloqueante (MessageTemplate en AnalysisBehaviour)
 *   - Registro en DF y búsqueda de otros agentes
 */
@SuppressWarnings("serial")
public class AgentePredictor extends Agent {

	private WekaClassifier  wekaClassifier;
	private TradingStateFSM stateFSM;

	private void iniciarPredictor() {
		wekaClassifier = new WekaClassifier();
		stateFSM = new TradingStateFSM(this);
	}

	@Override
	protected void setup() {
		System.out.println("[AgentePredictor] Iniciando...");
		
		iniciarPredictor();

		//Registrarse en el DF
	    Utils.registerService(this,Utils.SERVICE_PREDICTOR, "Clasificador J48 de señales de trading");
			

		addBehaviour(stateFSM);
		
		AnalysisBehaviour comportamientoAnalisis= new AnalysisBehaviour(this, wekaClassifier, stateFSM);
		addBehaviour(comportamientoAnalisis);

		// ── Comportamiento de predicción a futuro ──────────────
		// Escucha REQUEST con ontología "trading-prediction" desde AgenteUI.
		// Filtro distinto a AnalysisBehaviour para separar ambos flujos.
		addBehaviour(new ForecastBehaviour(this));

		System.out.println("[AgentePredictor] Listo. Esperando datos de mercado.");
	}

	@Override
	protected void takeDown() {
		Utils.deregisterService(this);
		System.out.printf("[AgentePredictor] Terminado. Estado final: %s | P&L: %.2f%%%n",stateFSM.getCurrentAction(), stateFSM.getPnl());
	}
}
