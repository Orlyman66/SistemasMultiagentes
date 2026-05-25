package es.upm.trading.behaviours;

import es.upm.trading.ml.PriceForecaster;
import es.upm.trading.model.PredictionRequest;
import es.upm.trading.model.PredictionResult;
import es.upm.trading.ui.PredictionPanel;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/*
 * Comportamiento del AgenteUI que recibe la respuesta de predicción
 * enviada por AgentePredictor (ForecastBehaviour) y actualiza la UI.
 *
 * Ciclo:
 *   1. Espera bloqueante de mensajes INFORM con ontología "trading-prediction".
 *   2. Deserializa el contenido:
 *      - PredictionResult → predicción calculada, mostrar en el panel
 *      - PredictionRequest con stepsAhead=-1 → datos insuficientes
 *   3. Actualiza PredictionPanel en el EDT de Swing.
 *   4. Reactiva el botón de predicción.
 */
public class ForecastResultBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 52L;

    // Filtro: INFORM + ontología trading-prediction 
    private static final MessageTemplate MT = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology(ForecastBehaviour.FORECAST_ONTOLOGY)
    );

    private final PredictionPanel predictionPanel;

    public ForecastResultBehaviour(Agent agent, PredictionPanel predictionPanel) {
        super(agent);
        this.predictionPanel = predictionPanel;
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(MT);

        if (msg != null) {
            System.out.println("[ForecastResultBehaviour] Respuesta de predicción recibida.");
            try {
                Object content = msg.getContentObject();

                if (content instanceof PredictionResult) {
                    // Predicción calculada correctamente
                    final PredictionResult result = (PredictionResult) content;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        predictionPanel.showForecastResult(result);
                    });

                } else if (content instanceof PredictionRequest) {
                	
                    // stepsAhead == -1 → datos insuficientes
                    PredictionRequest nack = (PredictionRequest) content;
                    final int missing = PriceForecaster.getMinSamples() - nack.getStepsAhead(); // stepsAhead = -1 aquí
                    
                    // Calcular cuántos faltan usando el store directamente
                    final String coinId = nack.getCoinId();
                    int current = es.upm.trading.model.MultiCoinDataStore.getInstance().getPointCount(coinId);
                    final int faltantes = PriceForecaster.getMinSamples() - current;

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        predictionPanel.showInsufficientData(
                                PriceForecaster.getMinSamples(), faltantes);
                    });
                }

            } catch (Exception e) {
                System.err.println("[ForecastResultBehaviour] ERROR: " + e.getMessage());
                javax.swing.SwingUtilities.invokeLater(() ->
                        predictionPanel.showError("Error al recibir predicción."));
            }
        } else {
            block();
        }
    }
}
