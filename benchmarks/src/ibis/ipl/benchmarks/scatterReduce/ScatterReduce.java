package ibis.ipl.benchmarks.scatterReduce;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.util.HashMap;

/**
 * Simulates master worker communication model. Workers request work/return
 * results to the master, and the master replies to the workers.
 */
final class ScatterReduce implements ReceivePortConnectUpcall {
    static final int COUNT = 10000;

    static final boolean ASSERT = false;

    Ibis ibis;

    Registry registry;

    PortType oneToManyType;

    PortType manyToOneType;

    IbisIdentifier masterID;

    ScatterReduce() {

        try {

            IbisCapabilities s = new IbisCapabilities(
                    IbisCapabilities.ELECTIONS_STRICT,
                    IbisCapabilities.CLOSED_WORLD
                    );
            
            manyToOneType = new PortType(
                    PortType.SERIALIZATION_OBJECT,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT,
                    PortType.CONNECTION_MANY_TO_ONE,
                    PortType.CONNECTION_UPCALLS
                    );
            
            oneToManyType = new PortType(PortType.SERIALIZATION_OBJECT,
                    PortType.CONNECTION_ONE_TO_MANY,
                    PortType.COMMUNICATION_RELIABLE,
                    PortType.RECEIVE_EXPLICIT);

            ibis = IbisFactory.createIbis(s, null, manyToOneType, oneToManyType);

            registry = ibis.registry();

            masterID = registry.elect("master");
            boolean master = masterID.equals(ibis.identifier());
            
 
            if (master) {
                master();
            } else {
                worker();
            }
        } catch (Exception e) {
            System.err.println("main caught exception: " + e);
            e.printStackTrace();
        }
    }

    void master() throws Exception {

        ReadMessage readMessage;
        WriteMessage writeMessage;
        SendPort sendPort;
        SendPortIdentifier origin;
        Data data;
        Data original;
        long start;
        long end;
        int max = 0;

        ReceivePort rport = ibis.createReceivePort(manyToOneType,
                "master receive port", this);
        rport.enableConnections();
        int poolsize = registry.getPoolSize();
        SendPortIdentifier[]  workers = rport.connectedTo();
        
        synchronized(this) {
        	while(workers.length < poolsize - 1) {
        		try {
        			wait(1000);
        		} catch(InterruptedException e) {
        			//ignore
        		}
        		workers = rport.connectedTo();
        	}
        }
        
        sendPort = ibis.createSendPort(oneToManyType);
        
        for (int i = 0; i < workers.length; i++) {
            sendPort.connect(workers[i].ibisIdentifier(), "receiveport");
        }
        System.err.println("Pool full, starting benchmark...");
        
        while (true) {
            start = System.currentTimeMillis();

            for (int i = 0; (i < COUNT); i++) {
	            writeMessage = sendPort.newMessage();
	            original = new Data();
	            writeMessage.writeObject(original);
	            writeMessage.finish();
	
	            for (int j = 0; j < workers.length; j++) {
	            	readMessage = rport.receive();
	                data = (Data) readMessage.readObject();
	                readMessage.finish();
	                
	                if (ASSERT) {
	                    if (!original.equals(data)) {
	                        System.err.println("did not receive data correctly");
	                        System.exit(1);
	                    }
	                }
	            }
            }
            end = System.currentTimeMillis();

            int speed = (int) (((COUNT * 1.0) / (end - start)) * 1000.0);

            if (speed > max) {
                max = speed;
            }

            System.err.println("MASTER: " + COUNT + " requests / "
                    + (end - start) + " ms (" + speed + " requests/s), max: "
                    + max);

        }
    }

    void worker() throws Exception {
        WriteMessage writeMessage;
        ReadMessage readMessage;
        Object data;
        ReceivePort rport = ibis.createReceivePort(oneToManyType, "receiveport");
        rport.enableConnections();
        SendPort sport = ibis.createSendPort(manyToOneType);

        sport.connect(masterID, "master receive port");

        while (true) {
            readMessage = rport.receive();
            data = readMessage.readObject();
            readMessage.finish();
        	
            writeMessage = sport.newMessage();
            writeMessage.writeObject(data);
            writeMessage.finish();
        }
    }

    public static void main(String args[]) {
        new ScatterReduce();
    }

	@Override
	public synchronized boolean gotConnection(ReceivePort receiver,
			SendPortIdentifier applicant) {
		// TODO Auto-generated method stub
		notifyAll();
		return true;
	}

	@Override
	public void lostConnection(ReceivePort receiver, SendPortIdentifier origin,
			Throwable cause) {
		// TODO Auto-generated method stub
		
	}
}

