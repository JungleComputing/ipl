package ibis.ipl.registry.btcentral;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.support.CountInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Connection {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private static HashMap<VirtualSocketAddress, Connection> activesOutbound;
    private static HashMap<VirtualServerSocket, LinkedList<Connection>> activesInbound;
    //private final VirtualSocket socket;
    
    private StreamConnection streamConnection;
    private final DataOutputStream out;

    private final DataInputStream in;
    private final CountInputStream counter;

    static final byte REPLY_ERROR = 2;

    static final byte REPLY_OK = 1;

    
    
    public Connection(IbisIdentifier ibis, int timeout, boolean fillTimeout)
    	throws IOException {
        this(VirtualSocketAddress.fromBytes(ibis.getRegistryData(), 0),
                timeout, fillTimeout);
    } 
    
    public Connection(VirtualSocketAddress address, int timeout,
            boolean fillTimeout)
            throws IOException {
        logger.debug("connecting to " + address + ", timeout = " + timeout
                + " , filltimeout = " + fillTimeout);
        /*
        if(activesOutbound == null)
        	activesOutbound = new HashMap<VirtualSocketAddress, Connection>();
        
        if(activesOutbound.containsKey(address)){
        	Connection c = activesOutbound.get(address);
        	out = c.out;
        	in = c.in;
        	counter = c.counter;
        	System.out.println("Reusing connection to " + address);
        	return;
        }        
        */
        boolean ok = false;
        //	System.out.println("Connecting connection to " + address);
        while(!ok){
        	try{
        		streamConnection = (StreamConnection)Connector.open(address.toString());
        		ok = true;
        	}catch(Exception e){
        		//System.out.print(".");
        		try{Thread.sleep((int)(1000*Math.random()));}catch(Exception e2){}
        	}
        }
        //socket = factory.createClientSocket(address, timeout, fillTimeout,
        //        lightConnection);
        //socket.setTcpNoDelay(true);
        
        out = new DataOutputStream(new BufferedOutputStream(streamConnection.openOutputStream()));
        counter = new CountInputStream(new BufferedInputStream(streamConnection.openInputStream()));
        in = new DataInputStream(counter);

        logger.debug("connection to " + address + " established");
        //System.out.println("Caching connection to " + address);
        //activesOutbound.put(address, this);

    }

    /**
     * Accept incoming connection on given serverSocket.
     */
    public Connection(VirtualServerSocket serverSocket) throws IOException {
        logger.debug("waiting for incomming connection...");
/*
        if(activesInbound==null)
        	activesInbound = new HashMap<VirtualServerSocket, LinkedList<Connection>>();
        

        if(activesInbound.containsKey(serverSocket)){
        	LinkedList<Connection> ll = activesInbound.get(serverSocket);
        	Iterator<Connection> it = ll.iterator();
        	while(it.hasNext()){
        		Connection c = it.next();
        		if(c.in.available()>0){
        			out = c.out;
        			in = c.in;
        			counter = c.counter;
        			return;
        		}
        	}
        	
        }
  */      
        streamConnection = serverSocket.accept();        
        
        counter = new CountInputStream(new BufferedInputStream(streamConnection.openInputStream()));
        in = new DataInputStream(counter);
        out = new DataOutputStream(new BufferedOutputStream(streamConnection.openOutputStream()));

/*        if(activesInbound.containsKey(serverSocket))
        	activesInbound.get(serverSocket).add(this);
        else{
        	LinkedList<Connection> ll = new LinkedList<Connection>();
        	ll.add(this);
        	activesInbound.put(serverSocket, ll);
        }*/
        logger.debug("new connection accepted");
    }

    public DataOutputStream out() {
        return out;
    }

    public DataInputStream in() {
        return in;
    }

    public int written() {
        return out.size();
    }

    public int read() {
        return counter.getCount();
    }

    public void getAndCheckReply() throws IOException {
        // flush output, just in case...
        out.flush();

        // get reply
        byte reply = in.readByte();
        if (reply == Connection.REPLY_ERROR) {
        	String message = in.readUTF();
            close();
            throw new RemoteException(message);
        } else if (reply != Connection.REPLY_OK) {
            close();
            throw new IOException("Unknown reply (" + reply + ")");
        }
    }

    public void sendOKReply() throws IOException {
        out.writeByte(Connection.REPLY_OK);
        out.flush();
    }

    public void closeWithError(String message) {
        if (message == null) {
            message = "";
        }
        try {
            out.writeByte(Connection.REPLY_ERROR);
            out.writeUTF(message);

            close();
        } catch (IOException e) {
            // IGNORE
        }
    }

    public void close() {

    	//Thread.dumpStack();
        try {
            out.flush();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            out.close();
        } catch (IOException e) {
            // IGNORE
        }

        try {
            in.close();
        } catch (IOException e) {
            // IGNORE
        }

        try {
        	streamConnection.close();
        } catch (IOException e) {
            // IGNORE
        }
        
    }

}
