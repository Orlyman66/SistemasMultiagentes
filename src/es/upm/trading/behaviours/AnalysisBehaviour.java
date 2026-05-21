package es.upm.trading.behaviours;

import es.upm.trading.model.MarketData;
import es.upm.trading.model.SignalHistoryStore;
import es.upm.trading.model.TradingSignal;
import es.upm.trading.ml.WekaClassifier;
import es.upm.trading.utils.Utils;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Comportamiento principal del AgentePredictor.
 *
 * Ciclo:
 *   1. Espera bloqueante de mensajes REQUEST con ontología "trading-system"
 *      y cuyo contenido sea un objeto MarketData.
 *   2. Clasifica los datos con el árbol J48 de WekaClassifier.
 *   3. Pasa la señal a la FSM para actualizar el estado de la cartera.
 *   4. Envía el TradingSignal como INFORM al AgenteUI.
 *
 * Temas de clase:
 *   - CyclicBehaviour (PDF2, diap. 39–41)
 *   - Filtro de mensajes con MessageTemplate (PDF3) — REQUISITO del enunciado
 *   - blockingReceive en modo bloqueante (PDF3) — REQUISITO del enunciado
 *   - Envío de respuesta INFORM (PDF3)
 */
public class AnalysisBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 5L;

    /**
     * Filtro de mensajes:
     *   - Performativa REQUEST
     *   - Ontología "trading-system"
     * Solo se leerán mensajes que cumplan AMBAS condiciones.
     * Patrón directo del ejemplo de clase (PDF3, MessageTemplate.and).
     */
    private static final MessageTemplate MT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology(Utils.ONTOLOGY)
    );

    private final WekaClassifier  weka;
    private final TradingStateFSM fsm;

    public AnalysisBehaviour(Agent agent, WekaClassifier weka, TradingStateFSM fsm) {
        super(agent);
        this.weka = weka;
        this.fsm  = fsm;
    }

    @Override
    public void action() {
        // ── Recepción bloqueante con filtro ──────────────────────
        // El comportamiento se bloquea hasta que llegue un REQUEST con la ontología correcta.
        // Ningún otro comportamiento se ve afectado (block() solo afecta a ESTE behaviour).
        ACLMessage msg = myAgent.receive(MT);

        if (msg != null) {
            System.out.println("[Predictor] Mensaje recibido de: "
                    + msg.getSender().getLocalName());
            try {
                // Deserializar el objeto MarketData del cuerpo del mensaje
                MarketData data = (MarketData) msg.getContentObject();
                System.out.println("[Predictor] Datos: " + data);

                // ── Clasificar con Weka J48 ──────────────────────
                TradingSignal signal = weka.classify(data);
                System.out.println("[Predictor] Señal generada: " + signal);

                // ── Guardar en historial por moneda ──────────────
                // Usamos el símbolo del MarketData original como coinId
                // para asociar la señal a la moneda correcta en el store
                SignalHistoryStore.getInstance().addSignal(data.getSymbol(), signal);

                // ── Actualizar FSM ───────────────────────────────
                fsm.processSignal(signal);

                // ── Enviar señal al AgenteUI como INFORM ─────────
                AID uiAgent = Utils.findAgent(myAgent, Utils.SERVICE_UI);
                if (uiAgent != null) {
                    Utils.sendInform(myAgent, uiAgent, signal);
                } else {
                    System.err.println("[Predictor] AgenteUI no encontrado en DF.");
                }

            } catch (Exception e) {
                System.err.println("[Predictor] ERROR procesando mensaje: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Ningún mensaje disponible: bloquear el behaviour
            // (equivalente al patrón receive() + block() del PDF3)
            block();
        }
    }
}
