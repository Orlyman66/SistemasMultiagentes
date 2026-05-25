package es.upm.trading.behaviours;

import es.upm.trading.model.TradingSignal;
import es.upm.trading.model.TradingSignal.Action;


import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;

/*
 * Máquina de estados finitos (FSMBehaviour) para el estado de trading.
 *
 * Estados:
 *   HOLD ←→ BUY  ←→ SELL
 *
 * Cada transición se dispara cuando el clasificador Weka emite una señal nueva.
 * En estado BUY/SELL se registra el precio de entrada para calcular beneficios y pérdidas.
 */
public class TradingStateFSM extends FSMBehaviour {

    private static final long serialVersionUID = 4L;

    // Identificadores de estado 
    public static final String STATE_HOLD = "HOLD";
    public static final String STATE_BUY = "BUY";
    public static final String STATE_SELL = "SELL";

    // Códigos de transición 
    private static final int TO_HOLD = 0;
    private static final int TO_BUY = 1;
    private static final int TO_SELL = 2;

    // Estado compartido entre sub-comportamientos 
    private volatile TradingSignal pendingSignal;
    private volatile Action currentAction = Action.HOLD;
    private volatile double entryPrice = 0.0;
    private volatile double pnl = 0.0;  // beneficios y perdidas acumuladas

    public TradingStateFSM(Agent agent) {
        super(agent);
        buildFSM();
    }

    //  Construcción de la FSM

    private void buildFSM() {

        // Estado HOLD 
        registerFirstState(new OneShotBehaviour(myAgent) {
            private int result = TO_HOLD;
            @Override
            public void action() {
                if (pendingSignal == null) { result = TO_HOLD; return; }
                System.out.println("[FSM] Estado HOLD — señal recibida: " + pendingSignal.getAction());
                switch (pendingSignal.getAction()) {
                    case BUY:  result = TO_BUY;  break;
                    case SELL: result = TO_SELL; break;
                    default:   result = TO_HOLD; break;
                }
                currentAction = pendingSignal.getAction();
                pendingSignal = null;
            }
            @Override public int onEnd() { return result; }
        }, STATE_HOLD);

        // Estado BUY 
        registerState(new OneShotBehaviour(myAgent) {
            private int result = TO_HOLD;
            @Override
            public void action() {
                if (pendingSignal == null) { result = TO_BUY; return; }
                System.out.println("[FSM] Estado BUY — señal: " + pendingSignal.getAction() + " @ " + pendingSignal.getPrice());

                // Registrar precio de entrada si venimos de HOLD
                if (currentAction != Action.BUY) {
                    entryPrice = pendingSignal.getPrice();
                    System.out.println("[FSM] Entrada BUY @ " + entryPrice);
                }

                switch (pendingSignal.getAction()) {
                    case SELL:
                        // Cerrar posición: calcular beneficio y pérdida
                        double closingPrice = pendingSignal.getPrice();
                        double trade = (closingPrice - entryPrice) / entryPrice * 100.0;
                        pnl += trade;
                        System.out.printf("[FSM] Posición cerrada: entrada=%.2f salida=%.2f trade=%.2f%% P&L=%.2f%%%n", entryPrice, closingPrice, trade, pnl);
                        result = TO_SELL;
                        break;
                    case HOLD: result = TO_HOLD; break;
                    default:   result = TO_BUY;  break;
                }
                currentAction = pendingSignal.getAction();
                pendingSignal = null;
            }
            @Override public int onEnd() { return result; }
        }, STATE_BUY);

        // Estado SELL 
        registerState(new OneShotBehaviour(myAgent) {
            private int result = TO_HOLD;
            @Override
            public void action() {
                if (pendingSignal == null) { result = TO_SELL; return; }
                System.out.println("[FSM] Estado SELL — señal: " + pendingSignal.getAction());

                if (currentAction != Action.SELL) {
                    entryPrice = pendingSignal.getPrice();
                    System.out.println("[FSM] Entrada SELL (short) @ " + entryPrice);
                }

                switch (pendingSignal.getAction()) {
                    case BUY:  result = TO_BUY;  break;
                    case HOLD: result = TO_HOLD; break;
                    default:   result = TO_SELL; break;
                }
                currentAction = pendingSignal.getAction();
                pendingSignal = null;
            }
            @Override public int onEnd() { return result; }
        }, STATE_SELL);

        // Transiciones 
        // Desde HOLD
        registerTransition(STATE_HOLD, STATE_HOLD, TO_HOLD);
        registerTransition(STATE_HOLD, STATE_BUY,  TO_BUY);
        registerTransition(STATE_HOLD, STATE_SELL, TO_SELL);
        // Desde BUY
        registerTransition(STATE_BUY, STATE_BUY,   TO_BUY);
        registerTransition(STATE_BUY, STATE_HOLD,  TO_HOLD);
        registerTransition(STATE_BUY, STATE_SELL,  TO_SELL);
        // Desde SELL
        registerTransition(STATE_SELL, STATE_SELL, TO_SELL);
        registerTransition(STATE_SELL, STATE_HOLD, TO_HOLD);
        registerTransition(STATE_SELL, STATE_BUY,  TO_BUY);
    }
    
    /*
     * Alimenta la FSM con una nueva señal del clasificador.
     * Llamado desde AnalysisBehaviour.
     */
    public void processSignal(TradingSignal signal) {
        this.pendingSignal = signal;
        restart(); // Desbloquear la FSM para que procese el nuevo estado
    }

    public Action  getCurrentAction() { return currentAction; }
    public double  getPnl() { return pnl; }
    public double  getEntryPrice() { return entryPrice; }
}
