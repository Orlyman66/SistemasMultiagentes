package prueba;

import jade.core.Agent;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.util.leap.Iterator;
public class AgenteBuscarRegistro extends Agent
{
	protected void setup()
	{
		System.out.println("Agente " + getLocalName() + "Buscando servicios registrados");
		//Incluimos una espera para que de tiempo a que los agentes de la plataforma se inicialicen
		doWait(1000);
		//Buscamos servicios
		buscaServicios();
		System.out.println("");
		System.out.println("Buscamos agentes que ofrezcan servicios del tipo Imprimir");
		DFAgentDescription d = buscarAgente("Imprimir");
		System.out.println("Búsqueda de agentes que ofrezcan servicios del tipo Imprimir finalizada");
		//JOptionPane.showMessageDialog(null, "Búsqueda de agentes que ofrezcan servicios del tipo Imprimir finalizada");
	}
	private void buscaServicios()
	{
		//Creamos un descriptor de servicios
		DFAgentDescription dfd = new DFAgentDescription();
		try{
			//Consultamos al DF los servicios y los devuelve en el dfd
			DFAgentDescription[] result = DFService.search(this,dfd);
			System.out.println("Total servicios Encontrados: " + result.length);
			for(int i=0;i<result.length;i++)
			{
				String out = result[i].getName()+ " proporciona ";
				Iterator iter = result[i].getAllServices();
				while (iter.hasNext())
				{
					ServiceDescription sd = (ServiceDescription) iter.next();
					System.out.println(getLocalName() + out + ": " + sd.getName());
				}
			}
		}
		catch(Exception ex){
			System.err.println("El Agente :" + getLocalName()+ " no ha podido encontrar servicios:" + ex.getMessage());
					doDelete();
		}
	}
}