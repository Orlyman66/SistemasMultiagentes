package prueba;

import jade.core.behaviours.SimpleBehaviour;
public class ComportamientoSimple2 extends SimpleBehaviour {
	@Override
	public void action() {
		for(int i=0;i<10;i++)
			System.out.println("Soy cs2, Ejecuto tarea " + i);
	}
	@Override
	public boolean done() {
		return true;
		//return false;
	}
}
