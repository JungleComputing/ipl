/* $Id$ */

package ibis.gmi;

import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.util.HashMap;

import org.apache.log4j.Logger;

class MulticastGroups {
    
    public static Logger logger = Logger.getLogger(Group.class.getName());
    
    private static final HashMap<String, ReceivePort> receivePorts
            = new HashMap<String, ReceivePort>();    
    private static final HashMap<String, SendPort> sendPorts
            = new HashMap<String, SendPort>();
    
    private static PortType portType;
            
    static void init(PortType type) { 
        portType = type;
    }
    
    static void handleCreateMulticastReceivePort(ReadMessage m) { 
                        
        try {         
            int rank = m.readInt();
            int ticket = m.readInt();
            String id = m.readString();
            m.finish();
        
            if (logger.isDebugEnabled()) { 
                logger.debug(Group._rank  
                        + ": MulticastGroups.handleCreateMulticastReceivePort - " 
                        + "Request for multicast receive port " + id);
            }            
                                    
            // sanity check 
            synchronized (receivePorts) {
                if (receivePorts.containsKey(id)) { 
//                  should never happen -- internal error ?
                    Group.logger.fatal(Group._rank 
                        + ": MulticastGroup.handleCreateMulticastReceivePort()"
                        + " - : Attempt to create duplicate group " + id);
                }
            }

            // TODO: Use anonymous ports instead ?
            String myID = id + "-" + Group._rank;
            
            if (logger.isDebugEnabled()) { 
                logger.debug(Group._rank  
                        + ": MulticastGroups.handleCreateMulticastReceivePort - " 
                        + "Creating multicast receive port " + myID);
            }            
            
            ReceivePort rp = Group.ibis.createReceivePort(portType, myID, 
                    new GroupCallHandler());
            rp.enableConnections();
            rp.enableMessageUpcalls();
                                    
            synchronized (receivePorts) {
                receivePorts.put(id, rp);
            }
                
            WriteMessage wm = Group.unicast[rank].newMessage();
            
            wm.writeByte(GroupProtocol.CREATE_MULTICAST_PORT_REPLY);
            wm.writeInt(ticket);
            wm.writeObject(rp.receivePortIdentifier());
            wm.finish();

            if (logger.isDebugEnabled()) { 
                logger.debug(Group._rank  
                        + ": MulticastGroups.handleCreateMulticastReceivePort - " 
                        + "Multicast receive port " + myID + " done");
            }            
            
        } catch (Exception e) {
            logger.fatal(Group._rank + 
                    ": MulticastGroup.handleCreateMulticastReceivePort()"
                    + " - : Got an exception ", e);           
        }
    }
    
    static void handleCreateMulticastReceivePortReply(ReadMessage m) {
        
        try { 
            int ticket = m.readInt();
            Object id = m.readObject();            
            Group.ticketMaster.put(ticket, id);
            // shouldn't have to finish the message here ....
            
            if (logger.isDebugEnabled()) { 
                logger.debug(Group._rank  
                        + ": MulticastGroups.handleCreateMulticastReceivePortReply - " 
                        + "Received multicast reply [" + ticket + "] - " + id);
            }            
            
        } catch (Exception e) {
            logger.fatal(Group._rank + 
                    ": MulticastGroup.handleCreateMulticastReceivePortReply()"
                    + " - : Got an exception ", e);           
        }        
    }     
    
    static SendPort getMulticastSendport(String ID, int[] hosts) {
                
        // First translate ID so it is unique accros machines 
        ID = Group._rank + "-" + ID;
        
        if (logger.isDebugEnabled()) { 
            logger.debug(Group._rank + ": MulticastGroups.getMulticastSendport"
                    + " - Looking for multicast sendport " + ID);
        }
         
        // NOT USED 
        //if (hosts.length == 1) {
            //return unicast[hosts[0]];
        //}
                        
        // First check if we already have one a sendport to this group...
        synchronized (sendPorts) {            
            SendPort s = sendPorts.get(ID);
            if (s != null) {
                
                if (logger.isDebugEnabled()) { 
                    logger.debug(Group._rank  
                            + ": MulticastGroups.getMulticastSendport - " 
                            + "Found existing multicast sendport " + ID);
                }
                                
                return s;
            }
        }
        
        if (logger.isDebugEnabled()) { 
            logger.debug(Group._rank  
                    + ": MulticastGroups.getMulticastSendport - " 
                    + "Creating new multicast sendport " + ID);
        }
                
        // If not, create one by sending a message to all machines that will 
        // be part of the group...
        try {
                        
            int [] tickets = new int[hosts.length];        
                       
            // Send all messages
            for (int i=0;i<hosts.length;i++) { 
                tickets[i] = Group.ticketMaster.get();
                
                WriteMessage wm = Group.unicast[hosts[i]].newMessage();
                wm.writeByte(GroupProtocol.CREATE_MULTICAST_PORT);
                wm.writeInt(Group._rank);
                wm.writeInt(tickets[i]);
                wm.writeString(ID);
                wm.finish();
                
                if (logger.isDebugEnabled()) { 
                    logger.debug(Group._rank  
                            + ": MulticastGroups.getMulticastSendport - " 
                            + "Send request [" + tickets[i] + "]" 
                            + " to " + hosts[i] );
                }                                
            }

            // Collect all replies
            ReceivePortIdentifier [] rids = new ReceivePortIdentifier[hosts.length];
            
            for (int i=0;i<tickets.length;i++) {

                if (logger.isDebugEnabled()) { 
                    logger.debug(Group._rank  
                            + ": MulticastGroups.getMulticastSendport - " 
                            + "Waiting for reply [" + tickets[i] + "]"); 
                }

                Object tpm = Group.ticketMaster.get(tickets[i]);
                
                if (logger.isDebugEnabled()) { 
                    logger.debug(Group._rank  
                            + ": MulticastGroups.getMulticastSendport - " 
                            + "Got for reply [" + tickets[i] + "] " + tpm); 
                }
               
                rids[i] = (ReceivePortIdentifier) tpm;
                //Group.ticketMaster.get(tickets[i]);
            }
            
            // Create a sendport
            SendPort sp = Group.ibis.createSendPort(portType); 
            
            // Connect sendport to all receiveports
            for (int i=0;i<rids.length;i++) {
                sp.connect(rids[i]);                               
            }
            
            // Store the SendPort for future use
            synchronized (sendPorts) {            
                sendPorts.put(ID, sp);
            }

            if (logger.isDebugEnabled()) { 
                logger.debug(Group._rank  
                        + ": MulticastGroups.getMulticastSendport - " 
                        + "New multicast sendport " + ID + " created");
            }
                                   
            // Done
            return sp;
            
        } catch (Exception e) {
            logger.fatal(Group._rank + 
                    ": MulticastGroup.getMulticastSendPort()"
                    + " - : Got an exception", e);           
        }     
        
        // stupid compiler
        return null;
    }    
    
    static void exit() { 
        
        // Close all send and receive ports...
        synchronized (sendPorts) {
            for (SendPort sp : sendPorts.values()) {
                try { 
                    sp.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        } 
        
        synchronized (receivePorts) {
            for (ReceivePort rp : receivePorts.values()) {
                try { 
                    rp.close();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }         
    }
}
