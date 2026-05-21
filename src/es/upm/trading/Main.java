package es.upm.trading;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Clase principal que lanza la plataforma JADE y los tres agentes del sistema.
 *
 * Uso:
 *   java -cp jade.jar:weka.jar:. es.upm.trading.Main [simbolo]
 *
 * Ejemplo:
 *   java -cp lib/jade.jar:lib/weka.jar:bin es.upm.trading.Main bitcoin
 *
 *
 * Equivalente al patrón Main.java del ejemplo JADE_2024-25_7_Ejemplo_Image.pdf
 * que lanza JADE desde código Java en lugar de usar jade.Boot.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String symbol = (args.length > 0) ? args[0] : "bitcoin";
        System.out.println("=== Trading MAS — Iniciando con símbolo: " + symbol + " ===");

        // ── 1. Crear el runtime JADE ──────────────────────────────
        Runtime rt = Runtime.instance();

        // ── 2. Crear el contenedor principal ─────────────────────
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.GUI, "true"); // lanzar RMA GUI de JADE

        AgentContainer mainContainer = rt.createMainContainer(profile);

        // ── 3. Crear los agentes ──────────────────────────────────
        // El orden importa: UI y Predictor primero (deben estar en DF
        // cuando Adquisicion empiece a buscarlos)

        AgentController uiAgent = mainContainer.createNewAgent(
                "AgenteUI",
                "es.upm.trading.agents.AgenteUI",
                new Object[]{}
        );

        AgentController predictorAgent = mainContainer.createNewAgent(
                "AgentePredictor",
                "es.upm.trading.agents.AgentePredictor",
                new Object[]{}
        );

        AgentController adquisicionAgent = mainContainer.createNewAgent(
                "AgenteAdquisicion",
                "es.upm.trading.agents.AgenteAdquisicion",
                new Object[]{symbol}
        );

        // ── 4. Arrancar los agentes ───────────────────────────────
        uiAgent.start();
        Thread.sleep(500); // pequeña pausa para que el DF se registre

        predictorAgent.start();
        Thread.sleep(500);

        adquisicionAgent.start();

        System.out.println("=== Sistema multiagente iniciado. ===");
        System.out.println("    AgenteUI         → recibe INFORM (precios + señales)");
        System.out.println("    AgentePredictor  → recibe REQUEST (MarketData) → J48");
        System.out.println("    AgenteAdquisicion→ TickerBehaviour cada 30s");
        System.out.println("    Usa el GUI JADE (RMA) para inspeccionar agentes y mensajes.");
    }
}