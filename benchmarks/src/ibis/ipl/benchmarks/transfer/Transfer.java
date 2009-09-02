package ibis.ipl.benchmarks.transfer;

/* $Id: Throughput.java 6546 2007-10-05 13:21:40Z ceriel $ */


import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

class Transfer extends Thread {

    int count = 1000;
    
    boolean master = false;
    
    boolean runElect = true;
    
    int transferSize = 0;

    int rank;

    int remoteRank;

    ReceivePort rport;

    SendPort sport;

    byte[] data;

    public static void main(String[] args) {
        new Transfer(args).start();
    }

    void send() throws IOException {

        System.err.println("SEND count = " + count + " len = " + data.length + " bytes");
        for (int i = 0; i < count; i++) {
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeArray(data);
            writeMessage.finish();          
        }
    }

    void rcve() throws IOException {
    	System.err.println("RCVE count = " + count + " len = " + data.length + " bytes");
        for (int i = 0; i < count; i++) {
            ReadMessage readMessage = rport.receive();
            readMessage.readArray(data);
            readMessage.finish();           
        }
    }

    Transfer(String[] args) {
        /* parse the commandline */
        for (int i = 0; i < args.length; i++) {
        	if (args[i].equalsIgnoreCase("--count")) {
                i++;
                count = new Integer(args[i]);
        	} else if (args[i].equalsIgnoreCase("--size")) {
        		i++;
        		transferSize = new Integer(args[i]);
        	} else if (args[i].equalsIgnoreCase("--role")) {        		
        		i++;
        		runElect = false;
        		master = args[i].equalsIgnoreCase("master");
        	}
        	else{
                System.err.println("Throughput --count <count> --size <size> --role [master|slave]");
                System.exit(11);       		
        	}
        }

        data = new byte[transferSize];
    }

    public void run() {
        try {
        	
            IbisCapabilities s = new IbisCapabilities(
                    runElect ? IbisCapabilities.ELECTIONS_STRICT :
                    	IbisCapabilities.MEMBERSHIP_UNRELIABLE
                    );
            PortType t = new PortType(
                    PortType.SERIALIZATION_OBJECT,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT);
                     
            Ibis ibis = IbisFactory.createIbis(s, null, true, null, t);

            Registry r = ibis.registry();
        	IbisIdentifier remote;
            rport = ibis.createReceivePort(t, "test port");
            rport.enableConnections();
            sport = ibis.createSendPort(t);        	
        	
            if(runElect){
            	IbisIdentifier master = r.elect("throughput");
            	if (master.equals(ibis.identifier())) {
            		rank = 0;
            		remote = r.getElectionResult("1");
            		System.err.println(">>>>>>>> Righto, I'm the master");
            	} else {
            		r.elect("1");
            		rank = 1;
            		remote = master;
            		System.err.println(">>>>>>>> Righto, I'm the slave");
            	}
            }
            else{
            	rank = master ? 0 : 1;
            	
            	IbisIdentifier[] ids = ibis.registry().joinedIbises();
            	while(ids.length == 0){
            		try{Thread.sleep(500);}
            		catch(Exception e){}
            		ids = ibis.registry().joinedIbises();
            	}
            	remote = ids[0];
            }
            

            sport.connect(remote, "test port");

            if (rank == 0) {
                WriteMessage writeMessage = sport.newMessage();
                writeMessage.finish();
                long time = System.currentTimeMillis();
                send();
                time = System.currentTimeMillis() - time;
                double dataSent = ((double) (transferSize) / (1024.0));
                System.out.println("Latency: " + count + "x" +  dataSent + "kbytes calls took "
                        + (time / 1000.0) + " seconds");
                System.out.println("Throughput: "
                        + ((dataSent*count) / (time / 1000.0)) + " KByte/s");
            } else {
                rcve();
            }

            /* free the send ports first */
            sport.close();
            rport.close();
            ibis.end();

            System.exit(0);

        } catch (Exception e) {
            System.out.println("Got exception " + e);
            System.out.println("StackTrace:");
            e.printStackTrace();
        }
    }
}
