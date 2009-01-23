package ibis.ipl.benchmarks.randomsteal;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * This program is designed to simulate Satins 'worst case behaviour', i.e a storm of steal requests.
 * to be run as two instances. One is a server, the other a
 * client. The client connects to the to the server and (optionally) sends it 
 * a number of bytes. The server receives the bytes, connects to the client 
 * and returns the bytes it received.
 * 
 * By default, a new connection is created for every message. Depending on 
 * the command line options, this test uses normal, light, or ultra light messages. 
 * 
 * This version uses explicit receive.
 */

public class RandomSteal implements RegistryEventHandler {

    private static final PortType portTypeUltraLight = new PortType(
    		PortType.CONNECTION_ULTRALIGHT, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_TIMEOUT,
            PortType.CONNECTION_ONE_TO_ONE);

    private static final PortType portTypeLight = new PortType(
    		PortType.CONNECTION_LIGHT, 
    		PortType.COMMUNICATION_FIFO, 
    		PortType.COMMUNICATION_RELIABLE, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_TIMEOUT,
            PortType.CONNECTION_MANY_TO_ONE);

    private static final PortType portTypeNormal = new PortType(
    		PortType.COMMUNICATION_FIFO,
    		PortType.CONNECTION_DIRECT,
    		PortType.COMMUNICATION_RELIABLE, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_TIMEOUT,
            PortType.CONNECTION_MANY_TO_ONE);
    
    private static final PortType portTypeBarrier = new PortType(
    		PortType.CONNECTION_LIGHT, 
    		PortType.COMMUNICATION_FIFO, 
    		PortType.COMMUNICATION_RELIABLE, 
            PortType.SERIALIZATION_OBJECT, 
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY);
    
    private static final IbisCapabilities ibisCapabilities =
        new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT, 
        		IbisCapabilities.MALLEABLE, 
        		IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED, 
        		"nickname.smartsockets");

    private final PortType portType; 
    private final int nodes;
    
    private final int count;
    private final int repeat;
    
   // private final boolean reconnect;
    
    private Ibis ibis;
    
    private ReceivePort barrierR;
    private SendPort barrierS;
    
    
    private ReceivePort stealR;
    //private SendPort stealS;
    
    private ReceivePort replyR;
   // private SendPort replyS;
    
    
    private boolean done = false;
    private boolean server = false;    
    private boolean reconnect = true;
    
    private final byte [] message;
    
    private final ArrayList<IbisIdentifier> nodeList = new ArrayList<IbisIdentifier>();
    
    /// ************* DO NOT USE ************** NOT FINISHED ********** 
    
    private final Random random = new Random();
    
    private Statistics stats = new Statistics();
   
    private Statistics bestStats = null;
    private Statistics totalStats = new Statistics();
    
    private HashMap<PortIdentifier, SendPort> connectionCache = 
    	new HashMap<PortIdentifier, SendPort>();
    
    private static class PortIdentifier { 
    	
    	final IbisIdentifier id;
    	final String name;
		
    	public PortIdentifier(final IbisIdentifier id, final String name) {
			super();
			this.id = id;
			this.name = name;
		}

    	// Generated
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((id == null) ? 0 : id.hashCode());
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		// Generated
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final PortIdentifier other = (PortIdentifier) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
    }
    
    
    
    private class RequestHandler extends Thread { 
    	public void run() { 
    		handleSteals();
    	}
    }
    
    private RandomSteal(PortType portType, int nodes, int bytes, int count, int repeat, boolean reconnect) { 
    	this.portType = portType;
    	this.nodes = nodes;
    	this.count = count;
    	this.repeat = repeat;
    	this.reconnect = reconnect;
    
    	message = new byte[bytes];
    }
        
    private SendPort connect(IbisIdentifier id, String name) { 
    
    	if (!reconnect) { 
    		
    		PortIdentifier pid = new PortIdentifier(id, name);
    		
    		SendPort sp = connectionCache.get(pid);
    	
    		if (sp != null) { 
    			return sp;
    		}
    		
    		try { 
    			sp = ibis.createSendPort(portType);
    		} catch (Exception e) { 
				System.err.println(ibis.identifier() + ": Failed to create sendport!");
				e.printStackTrace(System.err);
				System.exit(1);
    		}
    		
    		ReceivePortIdentifier rid = null; 
		
    		while (rid == null) { 
    			try { 
    				rid = sp.connect(id, name);
    			} catch (Exception e) { 
    				System.err.println(ibis.identifier() + ": Failed to connect to " + id +  ", will retry");
    				e.printStackTrace(System.err);
    				stats.addConnectionFailed();
    			}
    		}
    		
    		connectionCache.put(pid, sp);
    		
    		return sp;
    	
    	} else { 
    	
    		PortIdentifier pid = new PortIdentifier(id, name);
    		
    		SendPort sp = connectionCache.get(pid);
    	
    		if (sp == null) { 
    			try { 
        			sp = ibis.createSendPort(portType);
        		} catch (Exception e) { 
    				System.err.println(ibis.identifier() + ": Failed to create sendport!");
    				e.printStackTrace(System.err);
    				System.exit(1);
        		}
        		
        		connectionCache.put(pid, sp);
    		}
    		
    		ReceivePortIdentifier rid = null; 
    		
    		while (rid == null) { 
    			try { 
    				rid = sp.connect(id, name, 60000, true);
    			} catch (AlreadyConnectedException e) { 
    				System.err.println(ibis.identifier() + ": Failed to connect to " + id +  ", will retry");
    				e.printStackTrace(System.err);
    				stats.addAlreadyConnected();
    			} catch (Exception e) { 
    				System.err.println(ibis.identifier() + ": Failed to connect to " + id +  ", will retry");
    				e.printStackTrace(System.err);
    				stats.addConnectionFailed();
    			}
    		}
    		
    		return sp;
    	}
    }
    
    private void disconnect(SendPort port, IbisIdentifier id, String name) { 
    	
    	if (reconnect) { 
    		try {
				port.disconnect(id, name);
			} catch (IOException e) {
				System.err.println(ibis.identifier() + ": Failed to disconnect sendport!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    	}
    }
    
    private void initBarrier(IbisIdentifier server) { 
  	
    	this.server = server.equals(ibis.identifier());

		// Make sure that we have seen all joins
		synchronized (this) {
			while (nodeList.size() < nodes) { 
				try { 
					wait(1000);
				} catch (InterruptedException e) {
					// ignored
				}
			}
		}
    	
    	if (this.server) { 
    		// I have also seen all joins, so connect to all clients
    		for (IbisIdentifier id : nodeList) { 
    			try {
    				// We do not connect to ourselves...
    				if (!id.equals(ibis.identifier())) { 
    					barrierS.connect(id, "barrier", 60000, true);
    				}
				} catch (ConnectionFailedException e) {
					System.err.println(ibis.identifier() + ": Failed to connect to barrier client at " + id);
    				e.printStackTrace(System.err);
    				System.exit(1);
				}
    		}
    	} else { 
    		// Connect to server
    		try { 
    			Thread.sleep(5000 + random.nextInt(1000));
    		} catch (Exception e) {
    			// ignore
    		}
			
    		try {
    			barrierS.connect(server, "barrier", 60000, true);
    		} catch (ConnectionFailedException e) {
    			System.err.println(ibis.identifier() + ": Failed to connect to barrier server at " + server);
    			e.printStackTrace(System.err);
    			System.exit(1);
    		}
    	}
    }
    
    private void barrier() { 
    	
    	if (server) { 
    		
    		for (int i=0;i<nodes-1;i++) { 
    			try {
					ReadMessage rm = barrierR.receive();
					rm.finish();
    			} catch (IOException e) {
					// TODO Auto-generated catch block
    				System.err.println(ibis.identifier() + ": Failed to receive barrier message!");
    				e.printStackTrace(System.err);
    				System.exit(1);
				}    		    			
    		}
    		
    		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.finish();
    		} catch (Exception e) {
				System.err.println(ibis.identifier() + ": Failed to send barrier message!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    	} else { 
     		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.finish();
    		} catch (Exception e) {
				System.err.println(ibis.identifier() + ": Failed to send barrier message to server!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    		
    		// Wait for the server's reply
    		try {
    			ReadMessage rm = barrierR.receive();
    			rm.finish();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			System.err.println(ibis.identifier() + ": Failed to receive barrier message!");
    			e.printStackTrace(System.err);
    			System.exit(1);
    		}    		    		
    	}
    }
    
    private Statistics [] gather(Statistics o) { 
    	
    	if (server) { 
    	
    		Statistics [] result = new Statistics[nodes];
    		
    		result[0] = o;
    		
    		for (int i=1;i<nodes;i++) { 
    			try {
					ReadMessage rm = barrierR.receive();
					result[i] = (Statistics) rm.readObject();
					rm.finish();
    			} catch (Exception e) {
					// TODO Auto-generated catch block
    				System.err.println(ibis.identifier() + ": Failed to receive gather message!");
    				e.printStackTrace(System.err);
    				System.exit(1);
				}  		    			
    		}

    		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.finish();
    		} catch (Exception e) {
				System.err.println(ibis.identifier() + ": Failed to send gather reply!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    		
    		return result;

    	} else { 
     		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.writeObject(o);
    			wm.finish();
    		} catch (Exception e) {
				System.err.println(ibis.identifier() + ": Failed to send gather message!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    		
    		// Wait for the server's reply
    		try {
    			ReadMessage rm = barrierR.receive();
    			rm.finish();
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			System.err.println(ibis.identifier() + ": Failed to receive gather reply!");
    			e.printStackTrace(System.err);
    			System.exit(1);
    		}    		    		
    
    		return null;
    	}
    }
    
    private IbisIdentifier selectVictim() { 
    	
    	int tmp = random.nextInt(nodes);
    	
    	IbisIdentifier victim = nodeList.get(tmp);
    	
    	if (victim.equals(ibis.identifier())) { 
    		return nodeList.get((tmp+1)%nodes);        	
    	}
    	
    	return victim;
    }

    private synchronized boolean getDone() {
		return done;
	}
    
    private synchronized void setDone() {
		done = true;
	}
    
    private void handleSteals() { 
    	
    	while (!getDone()) { 
    	
    		try { 
    			ReadMessage rm = stealR.receive(1000);
    			
    			if (rm != null) { 

    				rm.readArray(message);

    				IbisIdentifier id = rm.origin().ibisIdentifier();

    				rm.finish();

    				SendPort tmp = connect(id, "reply");
    			    	    		
    				WriteMessage wm = tmp.newMessage();
    				wm.writeArray(message);	
    				wm.finish();

    				disconnect(tmp, id, "reply");
    			}
    		} catch (ReceiveTimedOutException e) { 
    			// Perfectly legal    			
    		} catch (Exception e) { 
    			System.err.println("Failed to handle steal message");
        		e.printStackTrace(System.err);
        	}
    	}
    }
    
    private void steal(IbisIdentifier id) { 

    	try { 
    		SendPort tmp = connect(id, "steal");
    	
    		WriteMessage wm = tmp.newMessage();
    		wm.writeArray(message);	
    		wm.finish();

    		disconnect(tmp, id, "steal");
  
    		ReadMessage rm = replyR.receive();
    		rm.readArray(message);
    		rm.finish();

    		stats.addStealRequest();
    		
    	} catch (Exception e) { 
    		System.err.println("Failed to steal message");
    		e.printStackTrace(System.err);
    		System.exit(1);
    	}
    }
    
    private void benchmark() { 
    	for (int i=0;i<count;i++) { 
    		steal(selectVictim());
    	}
    }
   
    public void handleStatistics(long time) { 
    	
    	stats.setTime(time);
    	Statistics [] result = gather(new Statistics(stats));
    	stats.reset();
    	
    	if (result != null) { 
    		// This is the master
    		Statistics tmp = Statistics.sum(result);
    		
    		if (bestStats == null || tmp.getTime() < bestStats.getTime()) {
    			bestStats = tmp;
    		}
    		
    		totalStats.add(tmp);
    		
    		System.out.println(tmp.getStatistics("", nodes));
    	}
    }
 
	public void run() throws Exception {
    	
        // Create an ibis instance.
        ibis = IbisFactory.createIbis(ibisCapabilities, this, portType, portTypeBarrier);

        System.out.println("Started on: " + ibis.identifier());
        
        barrierS = ibis.createSendPort(portTypeBarrier);
        barrierR = ibis.createReceivePort(portTypeBarrier, "barrier");
        
        stealR = ibis.createReceivePort(portType, "steal");
        replyR = ibis.createReceivePort(portType, "reply");
        
        stealR.enableConnections();
        replyR.enableConnections();
        barrierR.enableConnections();

        ibis.registry().enableEvents();
        
        new RequestHandler().start();
        
        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        System.out.println("Server is " + server);
        
        initBarrier(server);

    	barrier();

        for (int i=0;i<repeat;i++) { 
        	long start = System.currentTimeMillis();

        	benchmark();

        	long end = System.currentTimeMillis();

        	barrier();
        
        	handleStatistics(end-start);
        }
        	
        setDone();
        
        barrier();
        
        if (server.equals(ibis.identifier())) { 
        
        	System.out.println(bestStats.getStatistics("BEST", nodes));
      
          	System.out.println(totalStats.getStatistics("TOTAL", nodes));
            
        	totalStats.div(repeat);
        	
        	System.out.println(totalStats.getStatistics("AVG", nodes));
        }
        
        // End ibis.
        ibis.end();
    }

	public void died(IbisIdentifier corpse) {
		if (!getDone()) { 
			System.err.println("Ibis died unexpectedly!" + corpse);
		}
	}
	
	public void electionResult(String electionName, IbisIdentifier winner) {
		// ignore
	}

	public void gotSignal(String signal, IbisIdentifier source) {
		// ignore
	}

	public synchronized void joined(IbisIdentifier joinedIbis) {
		nodeList.add(joinedIbis);		
	}

	public void left(IbisIdentifier leftIbis) {
		if (!getDone()) { 
			System.err.println("Ibis died unexpectedly!" + leftIbis);
		}
	}

	public void poolClosed() {
		// ignored
	}

	public void poolTerminated(IbisIdentifier source) {
		// ignored
	}

    public static void main(String args[]) {
    	
    	PortType portType = portTypeNormal;
    	int bytes = 1;
    	int count = 1000;
    	int repeat = 10;
    	int nodes = -1;
    	boolean reconnect = true;
    	
    	for (int i=0;i<args.length;i++) { 
    		if (args[i].equals("-light")) { 
    			portType = portTypeLight;
    		} else if (args[i].equals("-ultralight")) { 
    			portType = portTypeUltraLight;
    		} else if (args[i].equals("-normal")) { 
    			portType = portTypeNormal;
    		} else if (args[i].equals("-keepconnection")) { 
    			reconnect = false;
    		} else if (args[i].equals("-bytes") && i < args.length-1) { 
    			bytes = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-count") && i < args.length-1) { 
    			count = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-repeat") && i < args.length-1) { 
    			repeat = Integer.parseInt(args[++i]);
    		} else if (args[i].equals("-nodes") && i < args.length-1) { 
    			nodes = Integer.parseInt(args[++i]);    		
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(1);
    		}
    	}
    	
    	if (bytes < 0) { 
    		System.err.println("Packet size reset from " + bytes + " to 0!");
    		bytes = 0;
    	}
    	
    	if (nodes < 0) { 
    		System.err.println("Number of nodes not set");
    		System.exit(1);
    	}
    	
        try {
            new RandomSteal(portType, nodes, bytes, count, repeat, reconnect).run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
