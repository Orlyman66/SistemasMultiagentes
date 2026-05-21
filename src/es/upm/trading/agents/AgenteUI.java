package es.upm.trading.agents;

import es.upm.trading.behaviours.UpdateUIBehaviour;
import es.upm.trading.ui.DashboardFrame;
import es.upm.trading.utils.Utils;

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
                // Obtener el nombre visible de la moneda seleccionada
                String displayName = AgenteAdquisicion.ALL_COINS.entrySet().stream()
                        .filter(e -> e.getValue().equals(coinId))
                        .map(java.util.Map.Entry::getKey)
                        .findFirst().orElse(coinId);
                // Cargar el histórico completo ya acumulado en el store
                dashboard.switchToCoin(coinId, displayName);
            });

            System.out.println("[AgenteUI] Dashboard iniciado.");
            addBehaviour(new UpdateUIBehaviour(AgenteUI.this, dashboard));
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
}

