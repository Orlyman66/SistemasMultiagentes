package prueba;

import jade.core.Agent;
import jade.core.behaviours.*;
//
//public class AgComportamientoCiclico2 extends Agent{
//	ComportamientoCiclico2 cs2;
//	ComportamientoCiclico1 cs1;
//	protected void setup(){
//		System.out.println("Primer Agente JADE con 2 Comportamientos");
//		cs1= new ComportamientoCiclico1();
//		cs2= new ComportamientoCiclico2();
//		addBehaviour(cs1);
//		addBehaviour(cs2);
//		System.out.println("Despues de añadir los comportamientos");
//	}
//
//	class ComportamientoCiclico1 extends CyclicBehaviour{
//		int limite=0;
//		public void action()
//		{
//			limite++;
//			System.out.println("Ejecuto tarea C1Lim" + limite);
//			if (limite>5000)
//				removeBehaviour(cs2);
//		}
//	}
//	class ComportamientoCiclico2 extends CyclicBehaviour{
//		int limite=0;
//		public void action(){
//			limite++;
//			System.out.println("Ejecuto tarea C2Lim" + limite);
//		}
//	}
//}

public class AgComportamientoCiclico2 extends Agent {
	ComportamientoCiclico2 cs2;
	ComportamientoCiclico3 cs3;
	protected void setup()
	{
		System.out.println("Primer Agente JADE con 2 Comportamientos");
		ComportamientoCiclico1 cs1= new ComportamientoCiclico1();
		cs2= new ComportamientoCiclico2();
		cs3= new ComportamientoCiclico3();
		addBehaviour(cs1);
		addBehaviour(cs2);
		addBehaviour(cs3);
		cs3.block();
		System.out.println("Despues de añadir los comportamientos");
	}

	class ComportamientoCiclico1 extends CyclicBehaviour {
		int limite=0;
		public void action() {
			limite++;
			System.out.println("Ejecuto tarea C1Lim" + limite);
			if (limite>500000) {
				removeBehaviour(cs2);
				cs3.restart();
			}
		}
	}
	class ComportamientoCiclico2 extends CyclicBehaviour {
		int limite=0;
		public void action() {
			limite++;
			System.out.println("Ejecuto tarea C2Lim" + limite);
		}
	}
	class ComportamientoCiclico3 extends CyclicBehaviour {
		int limite=0;
		public void action() {
			limite++;
			System.out.println("Ejecuto tarea C3Lim" + limite);
		}
	}
}
