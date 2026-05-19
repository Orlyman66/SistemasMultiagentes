package prueba;
import jade.core.Agent;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;

public class AgRegistrar extends Agent
{
	public String servicio_param;
	protected void setup(){
		Object [] arg = getArguments();
		servicio_param = (String) arg[0];
		System.out.println("Agente " + getLocalName() + ". Registro el servicio: " + servicio_param + " en el DF");
		registerServiceNuevo(servicio_param);
	}

	private void registerServiceNuevo(String servicio){
		//Creamos un nuevo descriptor de servicios
		DFAgentDescription dfd = new DFAgentDescription();
		//Incluimos el identificador del agente en el descriptor de servicios
		dfd.setName(this.getAID());
		//Creamos un nuevo servicio con el nombre que nos han pasado como parámetro
		ServiceDescription sd = new ServiceDescription();
		sd.setType(servicio);
		sd.setName(servicio);
		//Incluimos el nuevo servicio en el descriptor de servicios
		dfd.addServices(sd);
		//Realizamos el registro del descriptor de servicios en el DF
		try{
			DFService.register(this,dfd);
		}
		catch(FIPAException ex)
		{
			System.err.println("El Agente :" + getLocalName()+ " no ha podido registrar el servicio "
					+ ex.getMessage());
			doDelete();
		}
	}
}