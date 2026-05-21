package es.upm.trading.agents;

import es.upm.trading.behaviours.AllCoinsFetchBehaviour;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import java.util.ArrayList;
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

    /** Catálogo de monedas. Mismo orden que CoinSelectorPanel. */
    public static final Map<String, String> ALL_COINS = new LinkedHashMap<>();
    static {
        ALL_COINS.put("Bitcoin",        "bitcoin");
        ALL_COINS.put("Ethereum",       "ethereum");
        ALL_COINS.put("BNB",            "binancecoin");
        ALL_COINS.put("Solana",         "solana");
        ALL_COINS.put("XRP",            "ripple");
        ALL_COINS.put("Cardano",        "cardano");
        ALL_COINS.put("Avalanche",      "avalanche-2");
        ALL_COINS.put("Dogecoin",       "dogecoin");
        ALL_COINS.put("Polkadot",       "polkadot");
        ALL_COINS.put("Chainlink",      "chainlink");
    }

    private volatile String activeCoinId = "bitcoin";
    private AllCoinsFetchBehaviour fetchBehaviour;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) activeCoinId = (String) args[0];

        System.out.println("[AgenteAdquisicion] Iniciando. Monedas: " + ALL_COINS.size());

        // Registro en el DF y en el registro estático para acceso directo desde UI
        Utils.registerAgentInstance(Utils.SERVICE_MARKET, this);
        addBehaviour(new OneShotBehaviour(this) {
            @Override
            public void action() {
                Utils.registerService(myAgent,
                        Utils.SERVICE_MARKET, "Servicio de datos multi-moneda");
            }
        });

        // Esperar a que UI y Predictor estén registrados en el DF
        doWait(3000);

        // Un único behaviour para todas las monedas → reloj global compartido
        List<String> coinIds = new ArrayList<>(ALL_COINS.values());
        fetchBehaviour = new AllCoinsFetchBehaviour(this, coinIds);
        addBehaviour(fetchBehaviour);

        System.out.println("[AgenteAdquisicion] AllCoinsFetchBehaviour añadido."
                + " Todas las monedas se actualizarán cada "
                + AllCoinsFetchBehaviour.INTERVAL / 1000 + "s simultáneamente.");
    }

    /**
     * Cambia la moneda activa que se muestra en la UI.
     * No detiene nada; el behaviour sigue acumulando datos de todas las monedas.
     */
    public void setActiveCoin(String coinId) {
        System.out.println("[AgenteAdquisicion] Activa: " + activeCoinId + " -> " + coinId);
        this.activeCoinId = coinId;
    }

    public String getActiveCoinId() { return activeCoinId; }

    @Override
    protected void takeDown() {
        if (fetchBehaviour != null) fetchBehaviour.shutdown();
        Utils.deregisterService(this);
        System.out.println("[AgenteAdquisicion] Terminado.");
    }
}
