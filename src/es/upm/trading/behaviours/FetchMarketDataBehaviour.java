package es.upm.trading.behaviours;

import es.upm.trading.model.MarketData;
import es.upm.trading.model.MultiCoinDataStore;
import es.upm.trading.utils.Utils;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Comportamiento periódico (TickerBehaviour) del AgenteAdquisicion.
 *
 * Cada INTERVAL ms:
 *  1. Llama a la API de CoinGecko para obtener precios y variaciones.
 *  2. Parsea el JSON de forma manual (sin dependencias externas).
 *  3. Guarda el dato en MultiCoinDataStore (histórico de todas las monedas).
 *  4. Si la moneda es la activa en UI, envía INFORM al AgenteUI.
 *  5. Envía REQUEST al AgentePredictor solo para la moneda activa.
 *
 * El constructor acepta un offset (ms) para escalonar los ticks de cada moneda
 * y evitar que todas llamen a la API al mismo segundo (rate limit).
 */
public class FetchMarketDataBehaviour extends TickerBehaviour {

    private static final long serialVersionUID = 3L;

    public static final long INTERVAL = 3_000L;

    private final String symbol;

    /** CoinId canónico (sin sufijos _sim / _fallback) para el store */
    private final String coinId;

    private static java.util.Map<String, Double> SIM_BASE_PRICES = new java.util.HashMap<>();
    static {
        SIM_BASE_PRICES.put("bitcoin",      67_000.0);
        SIM_BASE_PRICES.put("ethereum",      3_500.0);
        SIM_BASE_PRICES.put("binancecoin",     600.0);
        SIM_BASE_PRICES.put("solana",          170.0);
        SIM_BASE_PRICES.put("ripple",            0.55);
        SIM_BASE_PRICES.put("cardano",           0.45);
        SIM_BASE_PRICES.put("avalanche-2",      38.0);
        SIM_BASE_PRICES.put("dogecoin",          0.16);
        SIM_BASE_PRICES.put("polkadot",          8.0);
        SIM_BASE_PRICES.put("chainlink",        15.0);
    }

    private double  simPrice;
    private boolean hasRealPrice = false;
    private final double[] priceHistory = new double[6];
    private int historyCount = 0;
    private long rateLimitUntil = 0L;

    private final MultiCoinDataStore store = MultiCoinDataStore.getInstance();

    /**
     * @param agent   agente propietario
     * @param symbol  id CoinGecko de la moneda
     */
    public FetchMarketDataBehaviour(Agent agent, String symbol) {
        super(agent, INTERVAL);
        this.symbol   = symbol;
        this.coinId   = symbol;
        this.simPrice = SIM_BASE_PRICES.getOrDefault(symbol, 100.0);
    }

    /** @deprecated usar el constructor de dos parámetros */
    public FetchMarketDataBehaviour(Agent agent, String symbol, long ignoredOffset) {
        this(agent, symbol);
    }

    @Override
    protected void onTick() {
        System.out.println("[Adquisicion/" + symbol + "] Tick");

        MarketData data = fetchData();
        if (data == null) {
            System.err.println("[Adquisicion/" + symbol + "] Sin datos. Tick omitido.");
            return;
        }

        // 1. Guardar en el store (siempre, sea cual sea la moneda)
        store.addPoint(coinId, data);

        // 2. Notificar al AgenteUI con el dato (para actualizar precio en header
        //    si esta moneda es la activa, y para que el panel lateral muestre
        //    el contador de puntos)
        jade.core.AID uiAgent = Utils.findAgent(myAgent, Utils.SERVICE_UI);
        if (uiAgent != null) {
            Utils.sendInform(myAgent, uiAgent, data);
        }

        // 3. Enviar al predictor para clasificación
        Utils.sendRequest(myAgent, Utils.SERVICE_PREDICTOR, data);
    }

    // ── Obtención de datos ───────────────────────────────────────

    private MarketData fetchData() {

        if (System.currentTimeMillis() < rateLimitUntil) {
            System.out.println("[Adquisicion/" + symbol + "] Rate limit, usando simulación.");
            return generateSimulatedData();
        }

        try {
            MarketData data = fetchFromCoinGecko();
            if (data != null) {
                simPrice = data.getPrice();
                hasRealPrice = true;
                return data;
            }
        } catch (RateLimitException e) {
            rateLimitUntil = System.currentTimeMillis() + 6_000L;
            System.out.println("[Adquisicion/" + symbol + "] 429 — pausando 60s.");
        } catch (Exception e) {
            System.out.println("[Adquisicion/" + symbol + "] API error: " + e.getMessage());
        }
        return generateSimulatedData();
    }

    private MarketData fetchFromCoinGecko() throws Exception {
        String urlStr = "https://api.coingecko.com/api/v3/simple/price"
                + "?ids=" + symbol
                + "&vs_currencies=usd"
                + "&include_24hr_vol=true"
                + "&include_24hr_change=true"
                + "&include_1hr_change=true";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "TradingMAS/1.0");

        int status = conn.getResponseCode();
        if (status == 429) { conn.disconnect(); throw new RateLimitException(); }
        if (status != 200) { conn.disconnect(); throw new Exception("HTTP " + status); }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return parseJsonResponse(sb.toString());
    }

    private MarketData parseJsonResponse(String json) {
        try {
            if (json.equals("{}") || !json.contains("\"usd\":")) return null;

            double price     = extractDouble(json, "\"usd\":");
            double vol24h    = extractDouble(json, "\"usd_24h_vol\":");
            double change24h = extractDouble(json, "\"usd_24h_change\":");
            double change1h  = json.contains("\"usd_1h_change\":")
                    ? extractDouble(json, "\"usd_1h_change\":") : change24h / 24.0;

            double change5m  = change1h  / 12.0;
            double change10m = change1h  / 6.0;
            double change30m = change24h / 8.0;

            updateHistory(price);
            return new MarketData(symbol, price, change5m, change10m, change30m, vol24h);

        } catch (Exception e) {
            System.err.println("[Adquisicion/" + symbol + "] Parse error: " + e.getMessage());
            return null;
        }
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) throw new RuntimeException("Key not found: " + key);
        int start = idx + key.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        return Double.parseDouble(json.substring(start, end));
    }

    // ── Simulación ───────────────────────────────────────────────

    private MarketData generateSimulatedData() {
        double changePct = (Math.random() - 0.48) * 2.0;
        simPrice = simPrice * (1 + changePct / 100.0);

        double change5m  = (Math.random() - 0.5) * 1.0;
        double change10m = (Math.random() - 0.5) * 1.5;
        double volume    = simPrice * 300_000 + Math.random() * simPrice * 50_000;

        updateHistory(simPrice);
        return new MarketData(symbol, simPrice, change5m, change10m, changePct, volume);
    }

    private void updateHistory(double price) {
        System.arraycopy(priceHistory, 0, priceHistory, 1, priceHistory.length - 1);
        priceHistory[0] = price;
        historyCount = Math.min(historyCount + 1, priceHistory.length);
    }

    private static class RateLimitException extends Exception {
        RateLimitException() { super("429 Too Many Requests"); }
    }
}
