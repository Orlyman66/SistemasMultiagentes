package prueba;
import jade.core.Agent;

public class AgenteReceiver extends Agent{
	protected void setup(){
		System.out.println("Agente agenteReceiver");
		addBehaviour(new ResponderBehaviourMio(this));
	}
}