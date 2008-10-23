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
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY);
    
    private static final IbisCapabilities ibisCapabilities =
        new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT, 
        		IbisCapabilities.MALLEABLE, 
        		IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED, 
        		"nickname.smartsockets");

    private final PortType portType; 
    private final int bytes;    
    private final int nodes;
    
    private final int count;
    private final int repeat;
    
   // private final boolean reconnect;
    
    private Ibis ibis;
    
    private ReceivePort barrierR;
    private SendPort barrierS;
    
    private ReceivePort stealR;
    private SendPort stealS;
    
    private ReceivePort replyR;
    private SendPort replyS;
    
    private boolean done = false;
    private boolean server = false;    
    
    private final byte [] message;
    
    private final ArrayList<IbisIdentifier> nodeList = new ArrayList<IbisIdentifier>();
    
    /// ************* DO NOT USE ************** NOT FINISHED ********** 
    
    private final Random random = new Random();
    
    private long lowestTime = Long.MAX_VALUE;
    
    private long failedConnectionSetups = 0;
    private long stealRequests = 0;
    
    private class RequestHandler extends Thread { 
    	public void run() { 
    		handleSteals();
    	}
    }
    
    private RandomSteal(PortType portType, int nodes, int bytes, int count, int repeat) { 
    	this.portType = portType;
    	this.nodes = nodes;
    	this.bytes = bytes;
    	this.count = count;
    	this.repeat = repeat;
    	//this.reconnect = reconnect;
    
    	message = new byte[bytes];
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
    
    private void barrier(long time) { 
    	
    	if (server) { 
    		
    		long total = time;
    		
    		for (int i=0;i<nodes-1;i++) { 
    			try {
					ReadMessage rm = barrierR.receive();
					total += time;
					rm.finish();
    			} catch (IOException e) {
					// TODO Auto-generated catch block
    				System.err.println(ibis.identifier() + ": Failed to receive barrier message!");
    				e.printStackTrace(System.err);
    				System.exit(1);
				}    		    			
    		}
    		
    		if (total > 0 && lowestTime > total) { 
    			lowestTime = total;
    		}
    		
    		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.finish();
    		} catch (Exception e) {
				System.err.println(ibis.identifier() + ": Failed to send barrier message!");
				e.printStackTrace(System.err);
				System.exit(1);
			}
    		
    		double stealsPerSecondNode = (nodes * count * 1000.0) / total;
    		double stealsPerSecond     = (nodes * count * 1000.0) / ((1.0 * total) / nodes);
    		
        	System.out.println("Avg time: " + (total / nodes) + " ms. (" 
        			+ stealsPerSecondNode + " steals/sec/node, " + stealsPerSecond + " steals/sec)");
    	} else { 
     		try { 
    			WriteMessage wm = barrierS.newMessage();
    			wm.writeLong(time);
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

    				stealRequests++;
    				
    				rm.readArray(message);

    				IbisIdentifier id = rm.origin().ibisIdentifier();

    				rm.finish();

    				ReceivePortIdentifier rid = null; 
    	    		
    	    		while (rid == null) { 
    	    			try { 
    	    				rid = replyS.connect(id, "reply");
    	    			} catch (Exception e) { 
    	    				System.err.println(ibis.identifier() + ": Failed to connect to " + id +  ", will retry");
    	    	    		e.printStackTrace(System.err);
    	    	    		failedConnectionSetups++;
    	    			}
    	    		}

   // 	    		System.err.println(ibis.identifier() + ": Connect to " + id +  " : reply");
    	    		
    				WriteMessage wm = replyS.newMessage();
    				wm.writeArray(message);	
    				wm.finish();

    				replyS.disconnect(rid);
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
    		ReceivePortIdentifier rid = null; 
    		
    		while (rid == null) { 
    			try { 
    				rid = stealS.connect(id, "steal");
    			} catch (Exception e) { 
    				System.err.println(ibis.identifier() + ": Failed to connect to " + id +  ", will retry");
    	    		e.printStackTrace(System.err);
    	    		failedConnectionSetups++;
    			}
    		}

//    		System.err.println(ibis.identifier() + ": Connect to " + id +  " : steal");
    		
    		WriteMessage wm = stealS.newMessage();
    		wm.writeArray(message);	
    		wm.finish();

    		stealS.disconnect(rid);
  
    		ReadMessage rm = replyR.receive();
    		rm.readArray(message);
    		rm.finish();
    	
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
   
    public void run() throws Exception {
    	
        // Create an ibis instance.
        ibis = IbisFactory.createIbis(ibisCapabilities, this, portType, portTypeBarrier);

        System.out.println("Started on: " + ibis.identifier());
        
        barrierS = ibis.createSendPort(portTypeBarrier);
        barrierR = ibis.createReceivePort(portTypeBarrier, "barrier");
        
        stealS = ibis.createSendPort(portType);
        stealR = ibis.createReceivePort(portType, "steal");
    
        replyS = ibis.createSendPort(portType);
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

    	barrier(0L);

        for (int i=0;i<repeat;i++) { 
        	long start = System.currentTimeMillis();

        	benchmark();

        	long end = System.currentTimeMillis();

        	barrier(end-start);
        }
        	
        setDone();
        
        barrier(0L);
        
        if (server.equals(ibis.identifier())) { 
        	double stealsPerSecondNode = (nodes * count * 1000.0) / lowestTime;
        	double stealsPerSecond     = (nodes * count * 1000.0) / ((1.0 * lowestTime) / nodes);
		
        	System.out.println("BEST - Avg time: " + (lowestTime / nodes) + " ms. (" 
        			+ stealsPerSecondNode + " steals/sec/node, " + stealsPerSecond + " steals/sec)");
        }
        
        System.out.println(ibis.identifier() + ": Failed connection setups: " + failedConnectionSetups);
        
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
    	//boolean reconnect = true;
    	
    	for (int i=0;i<args.length;i++) { 
    		if (args[i].equals("-light")) { 
    			portType = portTypeLight;
    		} else if (args[i].equals("-ultralight")) { 
    			portType = portTypeUltraLight;
    		} else if (args[i].equals("-normal")) { 
    			portType = portTypeNormal;
    		//} else if (args[i].equals("-keepconnection")) { 
    		//	reconnect = false;
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
            new RandomSteal(portType, nodes, bytes, count, repeat).run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
