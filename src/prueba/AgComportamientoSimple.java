package prueba;

import jade.core.Agent;
import jade.core.behaviours.*;
public class AgComportamientoSimple extends Agent{ // run configurations=> java app(arguments): -gui Simple:prueba.AgComportamientoSimple
	class ComportamientoSimple extends SimpleBehaviour{
		public void action(){
			for(int i=0;i<10;i++)
				System.out.println("Ejecuto tarea " + i);
		}
		public boolean done(){
			return true;
			//return false; //bucle infinito pq no se ha acabado el comportamiento: sería equivalente a un comportamiento cíclico 
		}
	}
	protected void setup(){
		System.out.println("Primer Agente JADE con Comportamiento Simple");
		ComportamientoSimple cs= new ComportamientoSimple();
		addBehaviour(cs);
		ComportamientoSimple2 cs2 = new ComportamientoSimple2();
		addBehaviour(cs2);
	}
}