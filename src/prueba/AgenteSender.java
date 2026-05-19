package prueba;

import jade.lang.acl.*;
import jade.core.*;
import jade.core.Agent;

@SuppressWarnings("serial")
public class AgenteSender extends Agent{
	protected void setup()
	{
		System.out.println("Agente agenteSender");
		System.out.println("AID: "+getAID());
		System.out.println("Nombre AID: "+getAID().getName());
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		AID r = new AID();
		//Se debe utilizar el nombre del agente de acuerdo a la dirección IP global de nuestra máquina
		r.setName("agenteReceiver@192.168.11.100:1099/JADE");
		r.addAddresses("http://192.168.11.100:7778/acc");
		System.out.println("Envio mensaje a: "+r.getName());
		msg.addReceiver(r);
		msg.setContent("Mensaje de prueba");
		this.send(msg);
		System.out.println("Mensaje Enviado....");
	}
}
