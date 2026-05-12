package prueba;

import jade.core.*;
import jade.core.behaviours.*;

public class ThreadedAgent extends Agent{
	private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	protected void setup()
	{
		Behaviour b = new OneShotBehaviour(this)
		{
			public void action(){
				//Establecer acciones a realizar
			}
		};
		//Ejecución del comportamiento en un hilo dedicado
		//El comportamiento se encapsula dentro de un hilo
		addBehaviour(tbf.wrap(b));
	}
}