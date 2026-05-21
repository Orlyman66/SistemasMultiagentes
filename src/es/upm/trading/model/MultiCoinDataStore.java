package es.upm.trading.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén central (singleton) que guarda la serie histórica de MarketData
 * recibida por cada moneda desde que arrancó la aplicación.
 *
 * Todos los FetchMarketDataBehaviour escriben aquí en cuanto obtienen un dato.
 * La UI lee desde aquí al cambiar de moneda para pintar la gráfica completa.
 *
 * Thread-safety: ConcurrentHashMap para el mapa exterior +
 * Collections.synchronizedList para cada serie interior.
 */
public class MultiCoinDataStore {

    // ── Singleton ────────────────────────────────────────────────
    private static final MultiCoinDataStore INSTANCE = new MultiCoinDataStore();
    public static MultiCoinDataStore getInstance() { return INSTANCE; }
    private MultiCoinDataStore() {}

    /** Máximo de puntos guardados por moneda (evita crecimiento ilimitado) */
    private static final int MAX_POINTS = 500;

    /**
     * Mapa coinId → lista sincronizada de MarketData.
     * Se crea la lista la primera vez que llega un dato de esa moneda.
     */
    private final ConcurrentHashMap<String, List<MarketData>> series =
            new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    //  Escritura
    // ─────────────────────────────────────────────────────────────

    /**
     * Añade un MarketData a la serie de su moneda.
     * Llamado desde FetchMarketDataBehaviour cada tick.
     *
     * @param coinId  id CoinGecko de la moneda (e.g. "bitcoin")
     * @param data    dato recibido del agente de adquisición
     */
    public void addPoint(String coinId, MarketData data) {
        series.computeIfAbsent(coinId,
                k -> Collections.synchronizedList(new ArrayList<>()));

        List<MarketData> list = series.get(coinId);
        synchronized (list) {
            list.add(data);
            // Mantener tamaño máximo: eliminar el punto más antiguo
            if (list.size() > MAX_POINTS) {
                list.remove(0);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Lectura
    // ─────────────────────────────────────────────────────────────

    /**
     * Devuelve una copia snapshot de la serie de precios de una moneda.
     * La UI llama a esto al cambiar de moneda para redibujar la gráfica completa.
     *
     * @param coinId  id de la moneda
     * @return lista de doubles con los precios en orden cronológico,
     *         o lista vacía si aún no hay datos de esa moneda
     */
    public List<Double> getPrices(String coinId) {
        List<MarketData> list = series.get(coinId);
        if (list == null) return new ArrayList<>();

        List<Double> prices = new ArrayList<>();
        synchronized (list) {
            for (MarketData d : list) {
                prices.add(d.getPrice());
            }
        }
        return prices;
    }

    /**
     * Devuelve la lista de variaciones Δ30m en orden cronológico.
     * Usada para colorear la gráfica (verde/rojo).
     */
    public List<Double> getChanges30m(String coinId) {
        List<MarketData> list = series.get(coinId);
        if (list == null) return new ArrayList<>();

        List<Double> changes = new ArrayList<>();
        synchronized (list) {
            for (MarketData d : list) {
                changes.add(d.getPriceChange30m());
            }
        }
        return changes;
    }

    /**
     * Devuelve el último MarketData disponible de una moneda, o null.
     */
    public MarketData getLatest(String coinId) {
        List<MarketData> list = series.get(coinId);
        if (list == null || list.isEmpty()) return null;
        synchronized (list) {
            return list.get(list.size() - 1);
        }
    }

    /**
     * Número de puntos acumulados para una moneda.
     */
    public int getPointCount(String coinId) {
        List<MarketData> list = series.get(coinId);
        if (list == null) return 0;
        synchronized (list) { return list.size(); }
    }

    /**
     * Devuelve todos los coinId que tienen al menos un punto.
     */
    public java.util.Set<String> getTrackedCoins() {
        return series.keySet();
    }
}

