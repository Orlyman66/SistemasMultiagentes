package es.upm.trading.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase singleton que guarda la serie histórica de MarketData
 * recibida por cada moneda desde que arrancó la aplicación.
 *
 * Cada vez que se obtiene un dato de una moneda desde la API se guarda aquí
 * Cada vez que se cambia de moneda desde la UI, se recuperan los datos para pintar la gráfica.
 */
public class MultiCoinDataStore {

    // Singleton 
    private static final MultiCoinDataStore INSTANCE = new MultiCoinDataStore();
    public static MultiCoinDataStore getInstance() { return INSTANCE; }
    private MultiCoinDataStore() {}

    /** Máximo de puntos guardados por moneda para evitar datos infinitos */
    private static final int MAX_POINTS = 500;

    /**
     * Listas sincronizadas y ordenadas de los MarketData de las monedas.
     * Se crea la lista la primera vez que llega un dato de esa moneda.
     */
    private final ConcurrentHashMap<String, List<MarketData>> series = new ConcurrentHashMap<>();


    /**
     * Añade un MarketData a la lista de su moneda
     * llamado desde FetchMarketDataBehaviour en cada tick.
     *
     * @param coinId  id de la moneda (e.g. "bitcoin")
     * @param data    dato recibido del agente de adquisición
     */
    public void addPoint(String coinId, MarketData data) {
        series.computeIfAbsent(coinId, k -> Collections.synchronizedList(new ArrayList<>()));

        List<MarketData> list = series.get(coinId);
        synchronized (list) {
            list.add(data);
            // Si se ha superado el tamaño maximo se elimina el MarketData más antiguo de esa moneda
            if (list.size() > MAX_POINTS) {
                list.remove(0);
            }
        }
    }


    /**
     * Devuelve la serie de precios de una moneda.
     * Se usa cada vez que se cambia una moneda desde la UI para poder pintar la gráfica.
     *
     * @param coinId  id de la moneda
     * @return lista con los precios de la moneda con ese id en orden cronológico,
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
     * Devuelve la lista de variaciones Δ30m en orden cronológico de una moneda.
     * Usada para colorear la gráfica (verde/rojo).
     * 
     * @param coinId  id de la moneda
     * @return lista con las variaciones de la moneda con ese id en orden cronológico,
     *         o lista vacía si aún no hay datos de esa moneda
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
     * Número de MarketData acumulados para una moneda.
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


