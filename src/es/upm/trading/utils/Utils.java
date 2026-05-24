package es.upm.trading.utils;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Iterator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
* Clase de utilidades para:
*  - Registrar / desregistrar servicios en el Directory Facilitator (DF)
*  - Buscar agentes por tipo de servicio en el DF
*  - Enviar mensajes ACL con objeto serializable como contenido
*
* Patrón tomado directamente del ejemplo Utils.java de clase (JADE_2024-25_6).
*/
public class Utils {

   /* Ontología compartida por todos los agentes del sistema */
   public static final String ONTOLOGY      = "trading-system";

   /* Nombres de servicio registrados en el DF */
   public static final String SERVICE_MARKET   = "market-data";   // AgenteAdquisicion
   public static final String SERVICE_PREDICTOR = "predictor";    // AgentePredictor
   public static final String SERVICE_UI        = "ui-display";   // AgenteUI

   /* Catálogo de monedas. Tanto para CoinSelectorPanel como AgenteAdqusición */
   private static final Map<String, String> ALL_COINS = new LinkedHashMap<>();
   
   public static void generarMonedas() { // generamos todas las monedas aquí para las clases CoinSelectorPanel y AgenteAdquisición
	   if(!ALL_COINS.isEmpty()) {
		   return;
	   }
       ALL_COINS.put("Bitcoin",        "bitcoin");
       ALL_COINS.put("Ethereum",       "ethereum");
       ALL_COINS.put("Solana",         "solana");
       ALL_COINS.put("Dogecoin",       "dogecoin");
       ALL_COINS.put("Polkadot",       "polkadot");
   }
   
   public static Map<String, String> getAllCoins() {
	   return ALL_COINS;
   }
   
   // ─────────────────────────────────────────────────────────────
   //  Registro en el DF
   // ─────────────────────────────────────────────────────────────

   /*
    * Registra un servicio en el Directory Facilitator.
    * Se llama en el setup() de cada agente (OneShotBehaviour).
    *
    * @param agent     referencia al agente que registra
    * @param serviceType  tipo del servicio (constantes SERVICE_*)
    * @param serviceName  nombre descriptivo del servicio
    */
   public static void registerService(Agent agent, String serviceType, String serviceName) {
       DFAgentDescription dfd = new DFAgentDescription();
       dfd.setName(agent.getAID());

       ServiceDescription sd = new ServiceDescription();
       sd.setType(serviceType);
       sd.setName(serviceName);

       dfd.addServices(sd);

       try {
           DFService.register(agent, dfd);
           System.out.println("[Utils] " + agent.getLocalName()
                   + " registró servicio '" + serviceType + "' en el DF.");
       } catch (FIPAException e) {
           System.err.println("[Utils] ERROR al registrar servicio en DF: " + e.getMessage());
       }
   }

   /*
    * Desregistra todos los servicios del agente del DF.
    * Se llama en takeDown() de cada agente.
    */
   public static void deregisterService(Agent agent) {
       try {
           DFService.deregister(agent);
           System.out.println("[Utils] " + agent.getLocalName() + " desregistrado del DF.");
       } catch (FIPAException e) {
           System.err.println("[Utils] ERROR al desregistrar: " + e.getMessage());
       }
   }

   // ─────────────────────────────────────────────────────────────
   //  Búsqueda en el DF
   // ─────────────────────────────────────────────────────────────

   /*
    * Busca en el DF el primer agente que ofrezca el tipo de servicio indicado.
    *
    * @param agent       agente que realiza la búsqueda
    * @param serviceType tipo de servicio buscado
    * @return AID del agente encontrado, o null si no existe
    */
   public static AID findAgent(Agent agent, String serviceType) {
       DFAgentDescription template   = new DFAgentDescription();
       ServiceDescription templateSd = new ServiceDescription();
       templateSd.setType(serviceType);
       template.addServices(templateSd);

       SearchConstraints sc = new SearchConstraints();
       sc.setMaxResults(1L);

       try {
           DFAgentDescription[] results = DFService.search(agent, template, sc);
           if (results != null && results.length > 0) {
               Iterator it = results[0].getAllServices();
               while (it.hasNext()) {
                   ServiceDescription sd = (ServiceDescription) it.next();
                   if (sd.getType().equals(serviceType)) {
                       AID provider = results[0].getName();
                       System.out.println("[Utils] " + agent.getLocalName()
                               + " encontró agente '" + provider.getLocalName()
                               + "' para servicio '" + serviceType + "'");
                       return provider;
                   }
               }
           }
       } catch (FIPAException e) {
           System.err.println("[Utils] ERROR buscando servicio '" + serviceType + "': " + e.getMessage());
       }
       return null;
   }

   /*
    * Registro estático de instancias de agentes accesibles por tipo de servicio.
    * Permite a AgenteUI obtener la referencia directa a AgenteAdquisicion
    * para llamar a changeCoin() sin pasar por mensajes ACL.
    */
   private static final java.util.Map<String, Agent> AGENT_REGISTRY =
           new java.util.concurrent.ConcurrentHashMap<>();

   /* Registra la instancia del agente (llamar en setup()) */
   public static void registerAgentInstance(String serviceType, Agent agent) {
       AGENT_REGISTRY.put(serviceType, agent);
   }

   /* Recupera la instancia registrada (puede ser null si aún no arrancó) */
   public static Agent findAgentObject(Agent caller, String serviceType) {
       return AGENT_REGISTRY.get(serviceType);
   }

   // ─────────────────────────────────────────────────────────────
   //  Envío de mensajes ACL
   // ─────────────────────────────────────────────────────────────

   /*
    * Envía un mensaje ACL de tipo REQUEST con un objeto serializable.
    * El receptor se descubre automáticamente buscando su servicio en el DF.
    */
   public static void sendRequest(Agent sender, String serviceType, Serializable content) {
       AID receiver = findAgent(sender, serviceType);
       if (receiver == null) {
           System.err.println("[Utils] No se encontró receptor para servicio: " + serviceType);
           return;
       }
       sendMessage(sender, receiver, ACLMessage.REQUEST, content, ONTOLOGY);
   }

   /*
    * Envía un mensaje ACL de tipo REQUEST con un objeto serializable a un AID concreto.
    */
   public static void sendRequest(Agent sender, AID receiver, Serializable content) {
       sendMessage(sender, receiver, ACLMessage.REQUEST, content, ONTOLOGY);
   }

   /*
    * Envía un mensaje ACL de tipo REQUEST con ontología personalizada.
    * Usado para mensajes de predicción a futuro (ontología "trading-prediction").
    */
   public static void sendRequest(Agent sender, AID receiver,
                                  Serializable content, String ontology) {
       sendMessage(sender, receiver, ACLMessage.REQUEST, content, ontology);
   }

   /*
    * Envía un mensaje ACL de tipo INFORM con un objeto serializable a un AID concreto.
    */
   public static void sendInform(Agent sender, AID receiver, Serializable content) {
       sendMessage(sender, receiver, ACLMessage.INFORM, content, ONTOLOGY);
   }
   
   /*
    * Envía un mensaje ACL genérico con la ontología indicada.
    */
   private static void sendMessage(Agent sender, AID receiver,
                                   int performative, Serializable content,
                                   String ontology) {
       try {
           ACLMessage msg = new ACLMessage(performative);
           msg.addReceiver(receiver);
           msg.setOntology(ontology);
           msg.setContentObject(content);
           sender.send(msg);
           System.out.println("[Utils] " + sender.getLocalName()
                   + " → " + receiver.getLocalName()
                   + " [" + ACLMessage.getPerformative(performative) + "]"
                   + " ont=" + ontology);
       } catch (Exception e) {
           System.err.println("[Utils] ERROR al enviar mensaje: " + e.getMessage());
       }
   }
}