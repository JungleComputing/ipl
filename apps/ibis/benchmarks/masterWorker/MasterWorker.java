import java.util.HashMap;
import java.io.IOException;


import ibis.ipl.*;
import ibis.io.*;
import ibis.util.*;

/**
 * Simulates master worker communication model. Workers request work/return
 * results to the master, and the master replies to the workers.
 */
final class MasterWorker {
    static final int COUNT = 1000;
    static final boolean ASSERT = true;

    Ibis ibis;
    Registry registry;
    PortType oneToOneType;
    PortType manyToOneType;

    MasterWorker() {
	StaticProperties s;

	try {
	    PoolInfo info = PoolInfo.createPoolInfo();
	    int rank = info.rank();

	    s = new StaticProperties();
	    s.add("communication", 
		    "OneToOne ManyToOne Reliable ExplicitReceipt");
	    s.add("serialization", "ibis");
	    s.add("worldmodel", "closed");
	    ibis = Ibis.createIbis(s, null);

	    registry = ibis.registry();

	    manyToOneType = ibis.createPortType("many2one type", s);

	    s = new StaticProperties();
	    s.add("communication", "OneToOne Reliable ExplicitReceipt");
	    s.add("serialization", "ibis");
	    s.add("worldmodel", "closed");

	    oneToOneType = ibis.createPortType("one2one type", s);

	    if (rank == 0) {
		master();
	    } else {
		worker();
	    }
	} catch (Exception e) {
	    System.err.println("main caught exception: " + e);
	    e.printStackTrace();
	}
    }

    public void connect(SendPort s, ReceivePortIdentifier ident) {
	boolean success = false;
	do {
	    try {
		s.connect(ident);
		success = true;
	    } catch (Exception e) {
		try {
		    Thread.sleep(500);
		} catch (Exception e2) {
		}
	    }
	} while (!success);
    }

    public ReceivePortIdentifier lookup(String name) throws IOException {
	ReceivePortIdentifier temp = null;

	do {
	    temp = registry.lookup(name);

	    if (temp == null) {
		try {
		    Thread.sleep(500);
		} catch (Exception e) {
		    // ignore
		}
	    }

	} while (temp == null);

	return temp;
    } 

    void master() throws Exception {
	//map of sendports to workers, indexed on sendportidentifiers of the
	//worker's sendports
	HashMap workers = new HashMap();

	ReadMessage readMessage;
	WriteMessage writeMessage;
	ReceivePortIdentifier peer;
	SendPort sendPort;
	SendPortIdentifier origin;
	Object data;
	long start;
	long end;

	ReceivePort rport = manyToOneType.createReceivePort(
		"master receive port");
	rport.enableConnections();

	while(true) {
	    start = System.currentTimeMillis();

	    for (int i = 0; i < COUNT; i++) {

		readMessage = rport.receive();
		origin = readMessage.origin();
		data = readMessage.readObject();
		readMessage.finish();

		sendPort = (SendPort) workers.get(origin);

		if (sendPort == null) {

		    peer = lookup("receiveport @ " + origin.ibis());

		    sendPort = oneToOneType.createSendPort();
		    connect(sendPort, peer);

		    workers.put(origin, sendPort);

		    System.err.println("MASTER: new worker detected,"
			    + " total now: " + workers.size());
		}

		writeMessage = sendPort.newMessage();
		writeMessage.writeObject(data);
		writeMessage.finish();
	    }

	    end = System.currentTimeMillis();

	    System.err.println("MASTER:" +  COUNT + " requests took " 
		    + (end - start) + " millis, " 
		    + ((end - start) * 1000.0) / COUNT
		    + " microseconds per request");
	}
    }

    void worker() throws Exception {
	WriteMessage writeMessage;
	ReadMessage readMessage;
	Data original = new Data();
	Data result;
	long start;
	long end;

	ReceivePort rport = oneToOneType.createReceivePort("receiveport @ "
		+ ibis.identifier());
	rport.enableConnections();
	SendPort sport = manyToOneType.createSendPort();

	ReceivePortIdentifier master = lookup("master receive port");

	connect(sport, master);

	while (true) {
	    start = System.currentTimeMillis();

	    for (int i = 0; i < COUNT; i++) {

		writeMessage = sport.newMessage();
		writeMessage.writeObject(original);
		writeMessage.finish();

		readMessage = rport.receive();
		result = (Data) readMessage.readObject();
		readMessage.finish();

		if (ASSERT) {
		    if (!original.equals(result)) {
			System.err.println("did not receive data correctly");
			System.exit(1);
		    }
		}
	    }

	    end = System.currentTimeMillis();

	    System.err.println("WORKER:" +  COUNT + " requests took " 
		    + (end - start) + " millis, " 
		    + ((end - start) * 1000.0) / COUNT
		    + " microseconds per request");
	}
    }

    public static void main(String[] args) {
	new MasterWorker();
    }

}
