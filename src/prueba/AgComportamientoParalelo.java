package prueba;

import jade.core.Agent;
import jade.core.behaviours.*;

public class AgComportamientoParalelo extends Agent{ //para que código más eficiente
	
	protected void setup(){
		SequentialBehaviour sequentialBehaviour1 = new SequentialBehaviour(this);
		sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this){
			public void action(){
				System.out.println("Subcomportamiento 1_1");
			}
		});
				sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this){
					public void action(){
						System.out.println("Subcomportamiento 1_2");
					}
				});
				sequentialBehaviour1.addSubBehaviour(new OneShotBehaviour(this){
					public void action(){
						System.out.println("Subcomportamiento 1_3");
					}
				});
				SequentialBehaviour sequentialBehaviour2 = new SequentialBehaviour(this);
				sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this){
					public void action(){
						System.out.println("Subcomportamiento 2_1");
					}
				});
						sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this){
							public void action(){
								System.out.println("Subcomportamiento 2_2");
							}
						});
						sequentialBehaviour2.addSubBehaviour(new OneShotBehaviour(this){
							public void action(){
								System.out.println("Subcomportamiento 2_3");
							}
						});
						ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this,ParallelBehaviour.WHEN_ALL);
						parallelBehaviour.addSubBehaviour(sequentialBehaviour1);
						parallelBehaviour.addSubBehaviour(sequentialBehaviour2);
						addBehaviour(parallelBehaviour);
	}
}