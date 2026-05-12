package prueba;
//import jade.core.Agent;

//public class AgBasico extends Agent{
//	protected void setup(){
//		System.out.println("Primer Agente JADE");
//	}
//}
//
//public class AgBasico extends Agent{
//	protected void setup(){
//		System.out.println("Primer Agente JADE");
//		System.out.println("AID: " + this.getAID());
//		System.out.println("Entrando en espera");
//		this.doWait(10000);
//		System.out.println("Saliendo de espera, entrando en suspendido");
//		this.doSuspend();
//		System.out.println("Saliendo de suspendido");
//	}
//}

import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
public class AgBasico extends Agent{
	protected void setup(){
		System.out.println("Primer Agente JADE");
		System.out.println("AID: " + this.getAID());
		System.out.println("Entrando en espera");
		this.doWait(10000);
		System.out.println("Saliendo de espera, entrando en suspendido");
		this.doSuspend();
		System.out.println("Saliendo de suspendido");
		AgentContainer container=(AgentContainer) getContainerController();
		Object[] params=new Object[1];
		params[0]="nuevo_parametro";
		try{
			AgentController agnt=container.createNewAgent("nuevoAgente", "prueba.AgBasicoParams", params);
			agnt.start();
		}
		catch(Exception e){e.printStackTrace();}
	}
}