package es.upm.trading;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/*
 * Clase principal que lanza la plataforma JADE y los tres agentes del sistema.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String symbol = "bitcoin";
        System.out.println("=== Sistema Multiagéntico de Trading — Iniciando. ===");

        // Crear el runtime JADE
        Runtime rt = Runtime.instance();

        // Crear el contenedor principal
        Profile profile = new ProfileImpl(); //Host: localhost, Port: 1099
        profile.setParameter(Profile.GUI, "true"); // lanzar GUI de JADE

        AgentContainer mainContainer = rt.createMainContainer(profile);

        // Crear los agentes
        // Primero se crean el UI y el Predictor ya que tienen que estar disponibles 
        // para el Adquisicion cuando los empiece a buscar

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

        // Arrancar los agentes
        uiAgent.start();
        Thread.sleep(500); // pequeña pausa para que el DF se registre

        predictorAgent.start();
        Thread.sleep(500); // pequeña pausa para que el DF se registre

        adquisicionAgent.start();

        System.out.println("=== Sistema multiagente iniciado. ===");
        System.out.println("    AgenteUI         → recibe INFORM (precios + señales)");
        System.out.println("    AgentePredictor  → recibe REQUEST (MarketData) → J48");
        System.out.println("    AgenteAdquisicion→ TickerBehaviour cada 30s");
        System.out.println("    GUI de JADE activado.");
    }
}