package ibis.satin.impl;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GlobalResultTable implements Upcall, Config {

    private Satin satin;

    private Map entries;
    
    /*used for communication with other replicas of the table*/
    
    //private SendPort send;
    private ReceivePort receive;
    
    //a quick net ibis bug fix
    private Map sends = new HashMap();
        
    private int numReplicas = 0;
    
    public int numUpdates = 0;
    public int numTryUpdates = 0;
    public int numLookups = 0;
    public int numLookupsSucceded = 0;
    public int maxNumEntries = 0;
    public int numRemoteLookups = 0;
    
    public final static int max = 20;
    
    private PortType portType;
    
    GlobalResultTable(Satin sat) {
    
	satin = sat;	
	entries = new HashMap();
	try {
	    StaticProperties s = new StaticProperties();
	    s.add("Serialization", "ibis");
	    portType = satin.ibis.createPortType("satin global result table porttype", s);
	
	    receive = portType.createReceivePort("satin global result table receive port on " +
		    satin.ident.name(), this);
	    //send = portType.createSendPort("satin global result table send  port on " +
		//    satin.ident.name());
	    receive.enableUpcalls();
	    receive.enableConnections();
	    
	} catch (IbisException e) {
	    System.err.println("SATIN '" + satin.ident.name() + "': Global result table - unable to create ports");
	    e.printStackTrace();
	} catch (IOException e) {
	    System.err.println("SATIN '" + satin.ident.name() + "': Global result table - unable to create ports - " + e.getMessage());
	    e.printStackTrace();
	}
    }
    
    Object lookupInvocationRecord(InvocationRecord r) {
	Object key = null;
	if (GLOBALLY_UNIQUE_STAMPS && satin.branchingFactor > 0) {
	    key = new Integer(r.stamp);
	} else {
	    key = r.getParameterRecord();
	}
	return lookup(key);
    }
    
    void updateInvocationRecord(InvocationRecord r) {
	Object key = null;
	if (GLOBALLY_UNIQUE_STAMPS && satin.branchingFactor > 0) {
	    key = new Integer(r.stamp);
	} else {
	    key = r.getParameterRecord();
	}
	ReturnRecord value = r.getReturnRecord();
	update(key, value);
    }
		
    
    void update(Object key, Object value) {
    
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
    
	if (GRT_STATS) {
	    numTryUpdates++;
	}
	if (GRT_DEBUG) {
		System.out.println("SATIN '" + satin.ident.name() + "': job updated by me: " + key + "," + value);
	}
	Object oldValue = entries.get(key);
	/*if (entries.size() < max)*/  entries.put(key, value);
	if (GRT_STATS) {
	    if (entries.size() > maxNumEntries) maxNumEntries = entries.size();
	}

	/* don't update if the result of this job was already in the table*/
	if (numReplicas > 0 && oldValue == null) {
	    if (GRT_DEBUG) {
		System.err.println("SATIN '" + satin.ident.name() + "': sending update: " + key + "," + value);
	    }
	    
	    //send an update message
		Iterator sendIter = sends.values().iterator();
		while (sendIter.hasNext()) {
			try {
				SendPort send = (SendPort) sendIter.next();
				WriteMessage m = send.newMessage();
				m.writeObject(key);
				if (GLOBAL_RESULT_TABLE_REPLICATED) {
				    m.writeObject(value);
				} else {
				    m.writeObject(satin.ident);
				}
				m.send();
				m.finish();
			} catch (IOException e) {
	    //always happens after a crash
			}
		}
		
		
	    //send an update message
/*	    try {
		WriteMessage m = send.newMessage();
		m.writeObject(key);
		m.writeObject(value);
		m.send();
		m.finish();
	    } catch (IOException e) {
		//always happens after the crash
	    }*/
	    if (GRT_STATS) {
		numUpdates++;
	    }
	}
	if (GRT_DEBUG) {   
	    System.err.println("SATIN '" + satin.ident.name() + "': update sent: " + key + "," + value);
	}
    }
    
    Object lookup(Object key) {
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
    
	Object value = entries.get(key);
	if (GRT_STATS) {
	    if (value != null) {
		System.err.println("SATIN '" + satin.ident.name() + "': lookup successful " + key);	    
		numLookupsSucceded ++;
		if (value instanceof IbisIdentifier) {
		    numRemoteLookups ++;
		}
	    }		
	    numLookups ++;		
	}
	return value;
    }
    
    //returns ready to send contents of the table
    Map getContents() {
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
	
	if (GLOBAL_RESULT_TABLE_REPLICATED) {
	    return (Map) ((HashMap) entries).clone();
	} else {
	    //replace "real" results with our ibis identifier
	    Map newEntries = (Map) ((HashMap) entries).clone();
	    Iterator iter = entries.entrySet().iterator();
	    while (iter.hasNext()) {
		Map.Entry element = (Map.Entry) iter.next();
		if (!(element.getValue() instanceof IbisIdentifier)) {
		    newEntries.put(element.getKey(), satin.ident);
		}
	    }
	    return newEntries;
	}
    }
    
    void addContents(Map contents) {
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
	
	entries.putAll(contents);
	
	if (GRT_STATS) {
	    if (entries.size() > maxNumEntries) maxNumEntries = entries.size();
	}
	
    }
    
    void addReplica(IbisIdentifier ident) {
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
	
	try {
	    SendPort send = portType.createSendPort("satin global result table send port on " +
		    satin.ident.name() + System.currentTimeMillis());
	    sends.put(ident, send);
	    ReceivePortIdentifier r = null;
	    r = satin.lookup("satin global result table receive port on " + ident.name());
	    Satin.connect(send, r);
	    numReplicas++;
	} catch (IOException e) {
	    System.err.println("SATN '" + satin.ident.name() + "': Transpositon table - unable to add new replica");
	    e.printStackTrace();
	}
    
    }
    
    void removeReplica(IbisIdentifier ident) {
	    if (ASSERTS) {
		Satin.assertLocked(satin);
	    }
    
	    if (sends.remove(ident) != null) {
		    numReplicas--;
	    }
    
    }
	        
    
    void exit() {
	try {
	    synchronized(satin) {
		if (numReplicas > 0) {
		    Iterator sendIter = sends.values().iterator();
		    while (sendIter.hasNext()) {
			SendPort send = (SendPort) sendIter.next();
			send.close();
		    }
		}
//		send.close();
	    }
	    receive.close();
	} catch (IOException e) {
	    System.err.println("SATIN '" + satin.ident.name() + "': Unable to free global result table ports");
	    e.printStackTrace();
	}
    }
    
    public void upcall(ReadMessage m) {
	if (GRT_TIMING) {
	    satin.handleUpdateTimer.start();
	}
	try {	    
		Object key = m.readObject();
		Object value = m.readObject();
		
		
		synchronized (satin) {
		    /*if (entries.size() < max)*/ entries.put(key, value);
		    if (GRT_STATS) {
			if (entries.size() > maxNumEntries) maxNumEntries = entries.size();
		    }			    
		}
		
		if (GRT_DEBUG) {
		    System.err.println("SATIN '" + satin.ident.name() + "': upcall finished" + key + "," + value);
		}
		
	} catch (IOException e) {
	    System.err.println("SATIN '" + satin.ident.name() + "': Global result table - error reading message");
	    e.printStackTrace();
	} catch (ClassNotFoundException e1) {
	    System.err.println("SATIN '" + satin.ident.name() + "': Global result table - error reading message");
	    e1.printStackTrace();
	}
	if (GRT_TIMING) {
	    satin.handleUpdateTimer.stop();
	}
    }
    
    
    public void print() {
	synchronized (satin) {
	    System.out.println("=GRT: " + satin.ident.name() + "=");
	    int i=0;
	    Iterator iter = entries.entrySet().iterator();
	    while (iter.hasNext()) {
		Map.Entry entry = (Map.Entry) iter.next();
		System.out.println("GRT[" + i + "]= " + entry.getKey() + ";" + entry.getValue());
		i++;
	    }
	    System.out.println("=end of GRT " + satin.ident.name() + "=");
	}
    }    
}
