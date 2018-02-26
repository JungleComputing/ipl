package ibis.ipl.benchmarks.connect;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

/**
 * This program is to be run as two instances. One is a server, the other a
 * client. The client connects to the to the server and (optionally) sends it 
 * a number of bytes. The server receives the bytes, connects to the client 
 * and returns the bytes it received.
 * 
 * By default, a new connection is created for every message. Depending on 
 * the command line options, this test uses normal, light, or ultra light messages. 
 * 
 * This version uses explicit receive.
 */

public class Connect {

    private static final PortType portTypeUltraLight = new PortType(
    		PortType.CONNECTION_ULTRALIGHT, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    private static final PortType portTypeLight = new PortType(
    		PortType.CONNECTION_LIGHT, 
    		PortType.COMMUNICATION_FIFO, 
    		PortType.COMMUNICATION_RELIABLE, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);

    private static final PortType portTypeNormal = new PortType(
    		PortType.COMMUNICATION_FIFO, 
    		PortType.COMMUNICATION_RELIABLE, 
            PortType.SERIALIZATION_DATA, 
            PortType.RECEIVE_EXPLICIT,
            PortType.CONNECTION_ONE_TO_ONE);
    
    private static final IbisCapabilities ibisCapabilities =
        new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT);

    private final PortType portType; 
    private final int bytes;
    
    private final int count;
    private final int repeat;
    
    private final boolean reconnect;
    
    private Connect(PortType portType, int bytes, int count, int repeat, boolean reconnect) { 
    	this.portType = portType;
    	this.bytes = bytes;
    	this.count = count;
    	this.repeat = repeat;
    	this.reconnect = reconnect;
    }
    
    private void server(Ibis myIbis) throws IOException {

        // Create a receive port and enable connections.
        ReceivePort receiver = myIbis.createReceivePort(portType, "server");
        receiver.enableConnections();

        // Create a send port for sending requests and connect.
        SendPort sender = null;
        
        final byte [] data = new byte[bytes];
        
        boolean connected = false;
        
        IbisIdentifier src = null;
        
       	sender = myIbis.createSendPort(portType);
        
        for (int r=0;r<repeat;r++) { 
        	
        	long start = System.currentTimeMillis();
        	ReceivePortIdentifier id = null;
        	
        	for (int c=0;c<count;c++) { 
        		
        //		System.out.println("R " + r + " C " + c);
        		
        		// Read the message.
        		ReadMessage rm = receiver.receive();
        	
        //		System.out.println("RM start");
            	
        		if (bytes > 0) { 
        			rm.readArray(data);
        		}
        		
        		if (!connected) { 
        		    src = rm.origin().ibisIdentifier();
        		    rm.finish();
                	
         //       	System.out.println("RM done");
                        	
        //	    	System.out.println("Connecting to " + src);
                	
        		    id = sender.connect(src, "client", 5000, true);
        		    connected = true;
        //	
        //	    	System.out.println("Connect done");
        		} else { 
        			rm.finish();
        //			System.out.println("RM done");
        		}
        		
        		WriteMessage wm = sender.newMessage();
        		
        //		System.out.println("WM start");
                
        		if (bytes > 0) {
        			wm.writeArray(data);
        		}
        		
        		wm.finish();
        //		System.out.println("WM done");
                
        		if (reconnect) { 
        			sender.disconnect(id);
        			connected = false;
        //			System.out.println("Disconnect done");
               }
        	}
        	
        	long end = System.currentTimeMillis();
        	
        	double rtt = (end-start) / ((double)count);
 
        	double tp = 0.0;
        	String unit = "bit/s";
    		
        	if (bytes > 0) { 
        		
        		tp = (bytes * count * 8.0) / ((end-start) / 1000.0);
        		
        		if (tp > 1000000) { 
        			tp = tp / 1000000.0;
        			unit = "Mbit/s";
        		} else if (tp > 1000) {
        			tp = tp / 1000.0;
        			unit = "Kbit/s";
        		}
        	}
       
         	System.out.printf("Send %d messages in %d ms. (%.2f ms/message, %.2f %s)\n", 
         		count, (end-start), rtt, tp, unit);
        }
       
        if (sender != null) { 
        	sender.close();
        }
        receiver.close();
    }

    private void client(Ibis myIbis,  IbisIdentifier server) throws IOException {

    	// Create a receive port and enable connections.
        ReceivePort receiver = myIbis.createReceivePort(portType, "client");
        receiver.enableConnections();
    	
        // Create a send port for sending requests and connect.
        SendPort sender = null;
        
        sender = myIbis.createSendPort(portType);
        
        ReceivePortIdentifier id = null;
        
        if (!reconnect) { 
        	id = sender.connect(server, "server", 5000, true);
        }
        
        System.out.println("Sending messages");
        
        final byte [] data = new byte[bytes];
        
        for (int r=0;r<repeat;r++) { 
        	
        	long start = System.currentTimeMillis();

        	for (int c=0;c<count;c++) { 

    //    		System.out.println("R " + r + " C " + c);
        		
         		if (reconnect) { 
        //	    	System.out.println("Connecting to " + server);
         	       	id = sender.connect(server, "server", 5000, true);
        //			System.out.println("Connected");
                }
        		
        		WriteMessage wm = sender.newMessage();

        		if (bytes > 0) {
        			wm.writeArray(data);
        		}

        		wm.finish();

       // 		System.out.println("WM done");
                
        		// Read the message.
        		ReadMessage rm = receiver.receive();
// 		System.out.println("RM start");
        
        		if (bytes > 0) { 
        			rm.readArray(data);
        		}
        		
        		rm.finish();
        
    //    		System.out.println("RM done");
        		
        		if (reconnect) { 
        			sender.disconnect(id);
        //			System.out.println("Disconnected");
            	}    
        	}

        	long end = System.currentTimeMillis();

        	double rtt = (end-start) / ((double)count);

        	double tp = 0.0;
        	String unit = "bit/s";

        	if (bytes > 0) { 

        		tp = (bytes * count * 8.0) / ((end-start) / 1000.0);

        		if (tp > 1000000) { 
        			tp = tp / 1000000.0;
        			unit = "Mbit/s";
        		} else if (tp > 1000) {
        			tp = tp / 1000.0;
        			unit = "Kbit/s";
        		}
        	}
  
        	System.out.printf("Send %d messages in %d ms. (%.2f ms/message, %.2f %s)\n", 
     			count, (end-start), rtt, tp, unit);
        }

        if (sender != null) { 
        	sender.close();
        }
        receiver.close();
    }
    
    private void run() throws Exception {
    	
        // Create an ibis instance.
        Ibis ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);

        // Elect a server
        IbisIdentifier server = ibis.registry().elect("Server");

        System.out.println("Server is " + server);
        
        // If I am the server, run server, else run client.
        if (server.equals(ibis.identifier())) {
            server(ibis);
        } else {
            client(ibis, server);
        }

        // End ibis.
        ibis.end();
    }

    public static void main(String args[]) {
    	
    	PortType portType = portTypeNormal;
    	int bytes = 0;
    	int count = 1000;
    	int repeat = 10;
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
    		} else { 
    			System.err.println("Unknown or incomplete option: " + args[i]);
    			System.exit(1);
    		}
    	}
    	
    	if (bytes < 0) { 
    		System.err.println("Packet size reset from " + bytes + " to 0!");
    		bytes = 0;
    	}
    	
        try {
            new Connect(portType, bytes, count, repeat, reconnect).run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
