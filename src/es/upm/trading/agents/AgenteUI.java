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

/**
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
public class AgenteUI extends Agent {

    private static final long serialVersionUID = 12L;

    private DashboardFrame dashboard;
    private String symbol = "bitcoin";

    @Override
    protected void setup() {
        System.out.println("[AgenteUI] Iniciando...");

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            symbol = (String) args[0];
        }

        // ── 1. Registro en el DF (OneShotBehaviour) ──────────────
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                Utils.registerService(myAgent,
                        Utils.SERVICE_UI,
                        "Dashboard de visualización de trading");
            }
        });

        // ── 2. Lanzar interfaz Swing en hilo independiente ────────
        javax.swing.SwingUtilities.invokeLater(() -> {
            dashboard = new DashboardFrame(symbol);

            // Cuando el usuario selecciona una moneda en el panel lateral:
            //  1. Decirle al AgenteAdquisicion cuál es la activa (para filtrar INFORMs)
            //  2. Cargar la serie histórica del store en la gráfica
            dashboard.setOnCoinSelected(coinId -> {
                AgenteAdquisicion adquisicion = (AgenteAdquisicion)
                        Utils.findAgentObject(AgenteUI.this, Utils.SERVICE_MARKET);
                if (adquisicion != null) {
                    adquisicion.setActiveCoin(coinId);
                }
            	Map<String,String> ALL_COINS=es.upm.trading.utils.Utils.getAllCoins();
                // Obtener el nombre visible de la moneda seleccionada
                String displayName = ALL_COINS.entrySet().stream()
                        .filter(e -> e.getValue().equals(coinId))
                        .map(java.util.Map.Entry::getKey)
                        .findFirst().orElse(coinId);
                // Cargar el histórico completo ya acumulado en el store
                dashboard.switchToCoin(coinId, displayName);
            });

            System.out.println("[AgenteUI] Dashboard iniciado.");
            // Registrar la referencia del agente en el panel de predicción
            // para que pueda enviar mensajes ACL al AgentePredictor
            dashboard.getPredictionPanel().setAgente(AgenteUI.this);
            addBehaviour(new UpdateUIBehaviour(AgenteUI.this, dashboard));
            addBehaviour(new ForecastResultBehaviour(
                    AgenteUI.this, dashboard.getPredictionPanel()));
        });

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

    /**
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

