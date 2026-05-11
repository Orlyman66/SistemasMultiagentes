package prueba;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

@SuppressWarnings("serial")
public class Agente extends Agent {
	protected CyclicBehaviour cyclicBehaviour;
	
	public void setup(){
		System.out.println("Soy el Agente 1");
		cyclicBehaviour = new CyclicBehaviour(this){
			public void action(){
				block();
			}
		};
		addBehaviour(cyclicBehaviour);
	}
	// la interfaz gráfica(rma) es un propio agente
	// otro tareas de gestión del sistema
	
	
}