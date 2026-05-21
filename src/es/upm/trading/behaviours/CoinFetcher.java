package es.upm.trading.behaviours;

import es.upm.trading.model.MarketData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsula el estado y la lógica de obtención de datos para UNA moneda.
 * No es un behaviour — es una clase auxiliar usada por AllCoinsFetchBehaviour.
 *
 * Cada instancia mantiene:
 *   - El precio de simulación propio de la moneda (simPrice)
 *   - El timestamp del rate limit si CoinGecko devuelve 429
 *   - Si ya se obtuvo al menos un precio real de la API
 */
public class CoinFetcher {

    // ── Precios de referencia para simulación ────────────────────
    private static final Map<String, Double> SIM_BASE_PRICES = new HashMap<>();
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

    private final String symbol;
    private double  simPrice;
    private boolean hasRealPrice  = false;
    private long    rateLimitUntil = 0L;

    public CoinFetcher(String symbol) {
        this.symbol   = symbol;
        this.simPrice = SIM_BASE_PRICES.getOrDefault(symbol, 100.0);
    }

    // ─────────────────────────────────────────────────────────────
    //  Obtención de datos (llamada desde AllCoinsFetchBehaviour)
    // ─────────────────────────────────────────────────────────────

    /**
     * Obtiene el MarketData más reciente para esta moneda.
     * Intenta la API real primero; si falla usa simulación.
     * Nunca bloquea más de 8 s (timeout de conexión).
     */
    public MarketData fetch() {

        if (System.currentTimeMillis() < rateLimitUntil) {
            System.out.println("[Fetcher/" + symbol + "] Rate limit activo, simulando.");
            return simulate();
        }

        try {
            MarketData data = fetchFromCoinGecko();
            if (data != null) {
                simPrice    = data.getPrice();
                hasRealPrice = true;
                return data;
            }
        } catch (RateLimitException e) {
            rateLimitUntil = System.currentTimeMillis() + 60_000L;
            System.out.println("[Fetcher/" + symbol + "] 429 — pausando 60s.");
        } catch (Exception e) {
            System.out.println("[Fetcher/" + symbol + "] API error: " + e.getMessage());
        }
        return simulate();
    }

    // ── API CoinGecko ─────────────────────────────────────────────

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
        return parse(sb.toString());
    }

    private MarketData parse(String json) {
        try {
            if (json.equals("{}") || !json.contains("\"usd\":")) return null;

            double price     = extractDouble(json, "\"usd\":");
            double vol24h    = extractDouble(json, "\"usd_24h_vol\":");
            double change24h = extractDouble(json, "\"usd_24h_change\":");
            double change1h  = json.contains("\"usd_1h_change\":")
                    ? extractDouble(json, "\"usd_1h_change\":") : change24h / 24.0;

            return new MarketData(symbol,
                    price,
                    change1h  / 12.0,  // change5m
                    change1h  / 6.0,   // change10m
                    change24h / 8.0,   // change30m
                    vol24h);

        } catch (Exception e) {
            System.err.println("[Fetcher/" + symbol + "] Parse error: " + e.getMessage());
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

    // ── Simulación ────────────────────────────────────────────────

    private MarketData simulate() {
        double changePct = (Math.random() - 0.48) * 2.0;
        simPrice = simPrice * (1 + changePct / 100.0);
        double change5m  = (Math.random() - 0.5) * 1.0;
        double change10m = (Math.random() - 0.5) * 1.5;
        double volume    = simPrice * 300_000 + Math.random() * simPrice * 50_000;
        return new MarketData(symbol, simPrice, change5m, change10m, changePct, volume);
    }

    public String getSymbol() { return symbol; }

    static class RateLimitException extends Exception {
        RateLimitException() { super("429 Too Many Requests"); }
    }
}
