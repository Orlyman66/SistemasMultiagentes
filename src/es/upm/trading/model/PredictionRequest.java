package es.upm.trading.model;

import java.io.Serializable;

/**
 * Objeto que viaja en el mensaje ACL REQUEST de AgenteUI -> AgentePredictor
 * para solicitar una predicción de precio futuro.
 *
 * Contiene el coinId de la moneda a predecir y el número de intervalos
 * hacia adelante (1, 3 o 5).
 */
public class PredictionRequest implements Serializable {

    private static final long serialVersionUID = 50L;

    private String coinId;
    private int    stepsAhead;

    public PredictionRequest(String coinId, int stepsAhead) {
        this.coinId     = coinId;
        this.stepsAhead = stepsAhead;
    }
    
    
    //Getters
    
    public String getCoinId() { return coinId; }
    public int getStepsAhead() { return stepsAhead; }

    
    //Setters
    
    public void setCoinId(String coinId) { this.coinId = coinId; }
    public void setStepsAhead(int stepsAhead) { this.stepsAhead = stepsAhead; }

    @Override
    public String toString() {
        return "PredictionRequest[coin=" + coinId + ", steps=" + stepsAhead + "]";
    }
}
