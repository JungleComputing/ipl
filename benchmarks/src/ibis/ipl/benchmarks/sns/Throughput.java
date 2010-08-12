package ibis.ipl.benchmarks.sns;

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
import ibis.ipl.impl.stacking.sns.SNSIbisCapabilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class Throughput extends Thread{

    int count = 1000;

    int transferSize = 0;

    int rank;

    int remoteRank;

    int windowSize = Integer.MAX_VALUE;

    ReceivePort rport;

    SendPort sport;

    byte[] data;
    
	public static void main(String args[]) throws InterruptedException {
		new Throughput(args).start();
	}
	

    void send() throws IOException {
        int w = windowSize;

//        System.err.println("count = " + count + " len = " + data.length);
        for (int i = 0; i < count; i++) {
            WriteMessage writeMessage = sport.newMessage();
            writeMessage.writeArray(data);
            writeMessage.finish();

            if (--w == 0) {
                System.err.println("EEEEEEEEEEEK");
                ReadMessage readMessage = rport.receive();
                readMessage.readArray(data);
                readMessage.finish();
                w = windowSize;
            }
        }
    }

    void rcve() throws IOException {
        int w = windowSize;
        for (int i = 0; i < count; i++) {
            ReadMessage readMessage = rport.receive();
            readMessage.readArray(data);
            readMessage.finish();

            if (--w == 0) {
                System.err.println("EEEEEEEEEEEK");
                WriteMessage writeMessage = sport.newMessage();
                writeMessage.writeArray(data);
                writeMessage.finish();
                w = windowSize;
            }
        }
    }

    Throughput(String[] args) {
        /* parse the commandline */
        int options = 0;
        for (int i = 0; i < args.length; i++) {
            if (false) {
            } else if (args[i].equals("-window")) {
                windowSize = Integer.parseInt(args[++i]);
                if (windowSize <= 0) {
                    windowSize = Integer.MAX_VALUE;
                }
            } else if (options == 0) {
                count = Integer.parseInt(args[i]);
                options++;
            } else if (options == 1) {
                transferSize = Integer.parseInt(args[i]);
                options++;
            }
        }

        if (options != 2) {
            System.err.println("Throughput <count> <size>");
            System.exit(11);
        }

        data = new byte[transferSize];
    }

    public void run() {
        try {
            IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.ELECTIONS_STRICT,
        			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED
        			,SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY
//        			,SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY
                    );
            PortType t = new PortType(
                    PortType.SERIALIZATION_OBJECT,
                    PortType.CONNECTION_ONE_TO_ONE,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT);
            
//        	IbisCapabilities s = new IbisCapabilities(IbisCapabilities.ELECTIONS_STRICT, 
//        			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED, SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY);
//
//            PortType t =
//                new PortType(PortType.COMMUNICATION_RELIABLE,
//                        PortType.SERIALIZATION_DATA, PortType.RECEIVE_AUTO_UPCALLS,
//                        PortType.CONNECTION_MANY_TO_ONE, PortType.CONNECTION_UPCALLS);
                     
    		Properties property = new Properties();
    		property.setProperty("ibis.pool.name", "Benchmark");
    		property.setProperty("ibis.server.address","localhost");
    		property.setProperty("ibis.implementation", "sns,smartsockets");
    		property.setProperty("ibis.verbose", "true");
    		property.setProperty("sns.keystorename", "KEYSTORE");
    		
    		Properties snsProperties = new Properties();
    		FileInputStream fis = new FileInputStream("snsProperties");
    		snsProperties.load(fis);
            
    		for (Enumeration propertyNames = snsProperties.propertyNames(); propertyNames.hasMoreElements(); )
    		{
    			Object key = propertyNames.nextElement();
    			property.put(key, snsProperties.get(key));
    		}    
            
            Ibis ibis = IbisFactory.createIbis(s, property, true, null, t);
            Registry r = ibis.registry();

            IbisIdentifier master = r.elect("throughput");
            IbisIdentifier remote;

            IbisIdentifier me = ibis.identifier();
            
            if (me.equals(master)) {
//            if (master.equals(ibis.identifier())) {
                rank = 0;
                remote = r.getElectionResult("1");
                System.err.println(">>>>>>>> Righto, I'm the master");
            } else {
                r.elect("1");
                rank = 1;
                remote = master;
                System.err.println(">>>>>>>> Righto, I'm the slave");
            }

            rport = ibis.createReceivePort(t, "test port");
            rport.enableConnections();
            sport = ibis.createSendPort(t);
            sport.connect(remote, "test port");

            if (rank == 0) {
            	int i = 100;
            	while(i > 0) {
                // warmup
                send();
                long time = System.currentTimeMillis();
                send();
                time = System.currentTimeMillis() - time;
                double speed = (time * 1000.0) / count;
                double dataSent = ((double) transferSize * (count + count
                        / windowSize))
                        / (1024.0 * 1024.0);
                System.out.print("Latency: " + count + " calls took "
                        + (time / 1000.0) + " seconds, time/call = " + speed
                        + " micros, ");
                System.out.println("Throughput: "
                        + (dataSent / (time / 1000.0)) + " MByte/s");
            	
            	i--;
            	}
            } else {
            	int i = 100;
            	while (i > 0) {
                rcve();
                rcve();
                
                i--;
            	}
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
