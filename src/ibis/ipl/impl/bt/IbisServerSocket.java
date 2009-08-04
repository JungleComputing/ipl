package ibis.ipl.impl.bt;

import java.io.IOException;

import javax.bluetooth.*;
import javax.microedition.io.*;


/**
 * Bluetooth accept server socket.
 */
class IbisServerSocket {
    private final String myServiceUUID = "2d26618601fb47c28d9f10b8ec891363";
    private final UUID MYSERVICEUUID_UUID = new UUID(myServiceUUID, false);
    private LocalDevice localDevice; // local Bluetooth Manager
    // Define the server connection URL
    private String connURL;
    private String openURL;
    
	StreamConnectionNotifier streamConnNotifier;
    
	IbisServerSocket() {
		streamConnNotifier = null;
    }

    IbisSocket accept() throws java.io.IOException {
    	//String ss = localDevice.getRecord(streamConnNotifier).getConnectionURL(0, false);
    	IbisSocket s = new IbisSocket(streamConnNotifier.acceptAndOpen());
        return s;
    }

    void bind() throws IOException{
    	openURL = "btspp://localhost:" + MYSERVICEUUID_UUID.toString() + ";name=Ibis";
        try{
    		localDevice = LocalDevice.getLocalDevice();
        	streamConnNotifier = (StreamConnectionNotifier) Connector.open(openURL);
        	ServiceRecord rec = localDevice.getRecord(streamConnNotifier);
        	connURL = rec.getConnectionURL(0, false);
        	try{
        		localDevice.setDiscoverable(DiscoveryAgent.GIAC);
        	}catch(Exception e){}
        }
        catch(IOException e){
        	System.err.println("BtIbis unable to register service locally");
        	return;
        }        	
    }
    
    IbisSocketAddress getLocalSocketAddress() {
    	return new IbisSocketAddress(connURL);
    }

    void close() throws java.io.IOException {
        try {
        	streamConnNotifier.close();
        } finally {
        	streamConnNotifier = null;            
        }
    }
}
