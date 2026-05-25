package es.upm.trading.behaviours;

import es.upm.trading.agents.AgenteAdquisicion;
import es.upm.trading.model.MarketData;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.model.TradingSignal;
import es.upm.trading.ui.DashboardFrame;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
 * Comportamiento del AgenteUI que escucha mensajes INFORM entrantes y
 * actualiza el dashboard Swing en el Event Dispatch Thread (EDT).
 *
 * Para MarketData:
 *   -Siempre actualiza el contador de puntos del panel lateral.
 *   -Solo actualiza el precio del header y la gráfica si la moneda recibida es la que está activa en la UI (evita que Dogecoin sobreescriba Bitcoin).
 *
 * Para TradingSignal:
 *   - Solo muestra señales de la moneda activa.
 */
public class UpdateUIBehaviour extends CyclicBehaviour {

	private static final long serialVersionUID = 6L;

	private static final MessageTemplate MT = MessageTemplate.and(
			MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			MessageTemplate.MatchOntology(Utils.ONTOLOGY));

	private final DashboardFrame dashboard;

	public UpdateUIBehaviour(Agent agent, DashboardFrame dashboard) {
		super(agent);
		this.dashboard = dashboard;
	}

	@Override
	public void action() {
		ACLMessage msg = myAgent.receive(MT);

		if (msg != null) {
			try {
				Object content = msg.getContentObject();

				if (content instanceof MarketData) {
					final MarketData data = (MarketData) content;
					final String coinId   = data.getSymbol();

					// Obtener la moneda activa del agente adquisidor
					AgenteAdquisicion adq = (AgenteAdquisicion)
							Utils.findAgentObject(myAgent, Utils.SERVICE_MARKET);
					final String activeCoin = (adq != null) ? adq.getActiveCoinId() : "";

					javax.swing.SwingUtilities.invokeLater(() -> {
						// Siempre: actualizar el contador en el panel lateral
						int count = MultiCoinDataStore.getInstance().getPointCount(coinId);
						dashboard.updateCoinPointCount(coinId, count);

						// Solo si es la moneda activa: actualizar header y gráfica
						if (coinId.equals(activeCoin)) {
							dashboard.updatePrice(data);
						}
					});

				} else if (content instanceof TradingSignal) {
					final TradingSignal signal = (TradingSignal) content;

					// Solo mostrar señales de la moneda activa
					AgenteAdquisicion adq = (AgenteAdquisicion)
							Utils.findAgentObject(myAgent, Utils.SERVICE_MARKET);
					String activeCoin = (adq != null) ? adq.getActiveCoinId() : "";

					if (signal.getSymbol() != null
							&& signal.getSymbol().startsWith(activeCoin)) {
						javax.swing.SwingUtilities.invokeLater(
								() -> dashboard.addSignal(signal));
					}
				}

			} catch (Exception e) {
				System.err.println("[UI] ERROR procesando INFORM: " + e.getMessage());
			}
		} else {
			block();
		}
	}
}
