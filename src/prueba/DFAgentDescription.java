package prueba;

import jade.lang.acl.*;
import jade.core.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.JOptionPane;

public class DFAgentDescription {

	protected DFAgentDescription buscarAgente(String tipo)
	{
		//indico las características el tipo de servicio que quiero encontrar
		DFAgentDescription template=new DFAgentDescription();
		ServiceDescription templateSd=new ServiceDescription();
		//como define el tipo el agente coordinador también podríamos buscar por nombre templateSd.setType(tipo); template.addServices(templateSd);
		//es posible establecer restricciones en la búsqueda, por ejemplo que el máximo de resultados sea 1
		SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults(new Long(1));
		try
		{
			DFAgentDescription [] results = DFService.search(this, template, sc);
			if (results.length > 0)
			{
				System.out.println("Agente "+getLocalName()+" encontro los siguientes agentes");
				for (int i = 0; i < results.length; ++i)
				{
					DFAgentDescription dfd = results[i];
					AID provider = dfd.getName();
					//un mismo agente puede proporcionar varios servicios, solo estamos interesados en "tipo" Iterator it = dfd.getAllServices();
					while (it.hasNext())
					{
						ServiceDescription sd = (ServiceDescription) it.next();
						if (sd.getType().equals(tipo))
						{
							System.out.println("- Servicio \""+sd.getName()+"\" proporcionado por el agente "+provider.getName());
							//JOptionPane.showMessageDialog(null, "- Servicio \""+sd.getName()+"\" proporcionado por el agente "+provider.getName());
							return dfd;
						}
					}
				}
			}
			else
			{
				//JOptionPane.showMessageDialog(null, "Agente "+getLocalName()+" no encontro ningun servicio buscador", "Error", JOptionPane.INFORMATION_MESSAGE);
			}
		}
		catch(FIPAException e)
		{
			//JOptionPane.showMessageDialog(null, "Agente "+getLocalName()+": "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		return null;
	}