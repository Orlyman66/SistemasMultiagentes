package prueba;

import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;

public class FSMAgent extends Agent { //máquinas de estado finitos
	static final String START = "START";
	static final String SUCCESS = "SUCCESS";
	static final String ERROR = "ERROR";
	protected void setup() {
		FSMBehaviour fsm = new FSMBehaviour(this);
		// Estado START
		fsm.registerFirstState(new OneShotBehaviour() {
			private int result;
			public void action() {
				System.out.println("Procesando...");
				// simulación: el resultado da ok la mitad de las veces y no ok la otramitad.
				boolean ok = Math.random() > 0.5;
				if (ok) {
					result = 1;
				} else {
					result = 0;
				}
			}
			public int onEnd() {
				return result;
			}
		}, START);
		// Estado SUCCESS
		fsm.registerLastState(new OneShotBehaviour() {
			public void action() {
				System.out.println("Todo OK");
			}
		}, SUCCESS);
		// Estado ERROR
		fsm.registerLastState(new OneShotBehaviour() {
			public void action() {
				System.out.println("Hubo error");
			}
		}, ERROR);
		// TRANSICIONES
		fsm.registerTransition(START, SUCCESS, 1);
		fsm.registerTransition(START, ERROR, 0);
		addBehaviour(fsm);
	}
}