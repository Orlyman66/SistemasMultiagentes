package es.upm.trading.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase singleton que guarda el historial de TradingSignal
 * generadas por AgentePredictor para cada moneda.
 *
 * Cada vez que clasifica un MarketData se guarda el resultado aquí.
 * Se muestran las últimas N señales al cambiar de moneda activa en la UI.
 */
public class SignalHistoryStore {

    // Singleton
    private static final SignalHistoryStore INSTANCE = new SignalHistoryStore();
    public static SignalHistoryStore getInstance() { return INSTANCE; }
    private SignalHistoryStore() {}

    /** Máximo de señales guardadas por moneda */
    private static final int MAX_SIGNALS = 200;

    /**
     * Listas sincronizadas y ordenadas de las TradinSignals de las monedas.
     * Se crea la lista la primera vez que llega un dato de esa moneda.
     */
    private final ConcurrentHashMap<String, List<TradingSignal>> history = new ConcurrentHashMap<>();

    /**
     * Añade una señal al historial de su moneda.
     * Llamado desde AnalysisBehaviour tras cada clasificación.
     *
     * @param coinId  id CoinGecko de la moneda (e.g. "bitcoin")
     * @param signal  señal generada por el clasificador J48
     */
    public void addSignal(String coinId, TradingSignal signal) {
        history.computeIfAbsent(coinId, k -> Collections.synchronizedList(new ArrayList<>()));

        List<TradingSignal> list = history.get(coinId);
        synchronized (list) {
            list.add(signal);
            // Si se ha superado el tamaño maximo se elimina la TradingSignal más antigua de esa moneda
            if (list.size() > MAX_SIGNALS) {
                list.remove(0);
            }
        }
    }

    /**
     * Devuelve las últimas N señales de una moneda en orden cronológico
     * inverso (más reciente primero), listo para insertar en la tabla.
     *
     * @param coinId  id de la moneda
     * @param maxRows número máximo de filas a devolver
     * @return lista de señales, vacía si no hay datos aún
     */
    public List<TradingSignal> getLastSignals(String coinId, int maxRows) {
        List<TradingSignal> list = history.get(coinId);
        if (list == null || list.isEmpty()) return new ArrayList<>();

        List<TradingSignal> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }

        // Tomar los últimos maxRows y devolver en orden inverso (más reciente primero)
        int from = Math.max(0, snapshot.size() - maxRows);
        List<TradingSignal> result = snapshot.subList(from, snapshot.size());
        List<TradingSignal> reversed = new ArrayList<>(result);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Número total de señales almacenadas para una moneda.
     */
    public int getSignalCount(String coinId) {
        List<TradingSignal> list = history.get(coinId);
        if (list == null) return 0;
        synchronized (list) { return list.size(); }
    }
}
