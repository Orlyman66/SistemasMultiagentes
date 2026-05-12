package prueba;

import jade.core.Agent;
import jade.core.behaviours.*;

public class AgComportamientoSecuencial extends Agent{
	protected void setup(){
		SequentialBehaviour sequentialBehaviour = new SequentialBehaviour(this);
		sequentialBehaviour.addSubBehaviour(new OneShotBehaviour(this){
			public void action(){
				System.out.println("Subcomportamiento 1");
			}
		});
				sequentialBehaviour.addSubBehaviour(new OneShotBehaviour(this){
					public void action(){
						System.out.println("Subcomportamiento 2");
					}
				});
				sequentialBehaviour.addSubBehaviour(new OneShotBehaviour(this){
					public void action(){
						System.out.println("Subcomportamiento 3");
					}
				});
				addBehaviour(sequentialBehaviour);
	}
} 