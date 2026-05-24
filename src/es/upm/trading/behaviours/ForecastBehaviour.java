package es.upm.trading.behaviours;

import es.upm.trading.ml.PriceForecaster;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.model.PredictionRequest;
import es.upm.trading.model.PredictionResult;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.Serializable;
import java.util.List;

/**
 * Comportamiento del AgentePredictor que atiende peticiones de predicción
 * de precio futuro enviadas desde AgenteUI.
 *
 * Ciclo:
 *   1. Espera bloqueante de mensajes REQUEST con ontología "trading-prediction".
 *   2. Deserializa el PredictionRequest con coinId y stepsAhead.
 *   3. Obtiene el histórico de precios de MultiCoinDataStore.
 *   4. Ejecuta PriceForecaster (M5P) para calcular el precio futuro.
 *   5. Responde con INFORM + PredictionResult al agente que lo solicitó.
 *
 * El filtro usa la ontología "trading-prediction" para distinguir estos
 * mensajes de los REQUEST de MarketData que gestiona AnalysisBehaviour
 * con ontología "trading-system".
 *
 * Temas de clase:
 *   - CyclicBehaviour con MessageTemplate y filtro bloqueante (PDF2, PDF3)
 *   - msg.createReply() para responder al remitente (PDF3)
 *   - Ontologías como mecanismo de filtrado (PDF3)
 */
public class ForecastBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 51L;

    /** Ontología exclusiva para mensajes de predicción a futuro */
    public static final String FORECAST_ONTOLOGY = "trading-prediction";

    /** Filtro: REQUEST + ontología trading-prediction */
    private static final MessageTemplate MT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology(FORECAST_ONTOLOGY)
    );

    private final PriceForecaster forecaster = new PriceForecaster();

    public ForecastBehaviour(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(MT);

        if (msg != null) {
            System.out.println("[ForecastBehaviour] Petición de predicción de: "
                    + msg.getSender().getLocalName());
            try {
                // ── 1. Deserializar la petición ───────────────────
                PredictionRequest req = (PredictionRequest) msg.getContentObject();
                System.out.println("[ForecastBehaviour] " + req);

                // ── 2. Verificar datos mínimos ────────────────────
                List<Double> prices = MultiCoinDataStore.getInstance()
                        .getPrices(req.getCoinId());

                PredictionResult result;
                if (prices.size() < PriceForecaster.getMinSamples()) {
                    // Datos insuficientes: devolver resultado nulo
                    // PredictionPanel lo detectará y mostrará el mensaje al usuario
                    result = null;
                    System.out.println("[ForecastBehaviour] Datos insuficientes: "
                            + prices.size() + "/" + PriceForecaster.getMinSamples());
                } else {
                    // ── 3. Ejecutar predicción con M5P ────────────
                    result = forecaster.predict(
                            req.getCoinId(), prices, req.getStepsAhead());
                    System.out.println("[ForecastBehaviour] Predicción completada: "
                            + (result != null ? result.getPredictedPrice() : "null"));
                }

                // ── 4. Responder con INFORM al solicitante ────────
                // createReply() crea el mensaje de respuesta con el
                // receptor ya apuntando al agente que envió el REQUEST
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setOntology(FORECAST_ONTOLOGY);

                if (result != null) {
                    reply.setContentObject((Serializable) result);
                } else {
                    // Enviar PredictionRequest con stepsAhead=-1 como señal
                    // de datos insuficientes (el receptor lo detecta)
                    PredictionRequest nack = new PredictionRequest(
                            req.getCoinId(), -1);
                    reply.setContentObject(nack);
                }

                myAgent.send(reply);
                System.out.println("[ForecastBehaviour] Respuesta enviada a: "
                        + msg.getSender().getLocalName());

            } catch (Exception e) {
                System.err.println("[ForecastBehaviour] ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            block();
        }
    }
}