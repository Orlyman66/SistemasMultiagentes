package prueba;

import jade.core.behaviours.Behaviour;
import jade.lang.acl.*;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
public class ResponderBehaviourMio extends SimpleBehaviour{
	//Establecemos un filtro para leer mensajes de tipo REQUEST
	//private static final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

	private static final MessageTemplate mt1 =
			MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
	public ResponderBehaviourMio(Agent agent){
		super(agent);
	}

	public void action(){
		while (true){
			ACLMessage aclMessage = myAgent.receive(mt1);
			if(aclMessage!=null) {
				if (aclMessage.getPerformative()== ACLMessage.REQUEST){
					System.out.println();
					System.out.println(myAgent.getLocalName()+": Recibo el mensaje: \n"+aclMessage);
					ACLMessage mr = aclMessage.createReply();
					mr.setContent("Respuesta al mensaje con INFORM");
					mr.setPerformative(ACLMessage.INFORM);
					myAgent.send(mr);
				}
				else if (aclMessage.getPerformative()== ACLMessage.INFORM){
					System.out.println();
					System.out.println(myAgent.getLocalName()+": Recibo el mensaje:\n"+aclMessage);
					ACLMessage mr = aclMessage.createReply();
					mr.setContent("Respuesta al mensaje con REQUEST");
					mr.setPerformative(ACLMessage.REQUEST);
					myAgent.send(mr);
				}
				//			if (aclMessage!=null){
				//				//imprimimos por pantalla el contenido del mensaje recibido.
				//				System.out.println();
				//				System.out.println(myAgent.getLocalName()+": Recibo el mensaje:\n"+aclMessage);
				//				//Creamos un mensaje de respuesta de tipo INFORM y lo enviamos.
				//				ACLMessage mr = aclMessage.createReply();
				//				mr.setContent("Respuesta al mensaje");
				//				mr.setPerformative(ACLMessage.INFORM);
				//				myAgent.send(mr);
				//				System.out.println();
				//				System.out.println(myAgent.getLocalName()+": Receptor, contesto al mensaje: \n"+aclMessage);
				//			}
				else
					this.block();}
		}
	}
	public boolean done(){
		return false;
	}
}
