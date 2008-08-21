package ibis.ipl.impl.smartsockets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.PortType;

import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.VirtualSocketAddress;

public class SmartSocketsUltraLightSendPort extends SendPort {

	private class ConnectionInfo extends SendPortConnectionInfo {
		ConnectionInfo(SendPort port, ReceivePortIdentifier target) 
			throws IOException {			
			super(port, target);
		}

		public void closeConnection() { /* empty */ } 
	}
		
	private final ByteArrayOutputStream bout;
	private final BufferedArrayOutputStream bufferedStream;

	private ServiceLink sl = null;
	
	SmartSocketsUltraLightSendPort(Ibis ibis, PortType type, String name, 
			Properties props) throws IOException {
        super(ibis, type, name, null, props);
        
        // TODO: This seems a bit inefficient, since it adds a copy...
        bout = new ByteArrayOutputStream();        
        bufferedStream = new BufferedArrayOutputStream(bout, 4096);
        initStream(bufferedStream);
	}	
	
	@Override
	protected void announceNewMessage() throws IOException {
		// empty ? 
	}

	@Override
	protected void closePort() throws IOException {		
		bufferedStream.close();
		bout.close();		
	}

	@Override
	protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver, 
			long timeout, boolean fillTimeout) throws IOException {
		return new ConnectionInfo(this, receiver);
	}

	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		// empty
	}

	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver, 
			SendPortConnectionInfo c) throws IOException {		
		// empty
	}
	
    void sendMessage(ReceivePortIdentifier id, int opcode, byte [][] message) 
    	throws UnknownHostException, MalformedAddressException { 
    	
    	if (sl == null) { 
    		sl = ((SmartSocketsIbis) ibis).getServiceLink();
    	}
    		
    	if (sl != null) { 
    		IbisIdentifier tmp = (IbisIdentifier) id.ibisIdentifier();
    		VirtualSocketAddress a = VirtualSocketAddress.fromBytes(tmp.getImplementationData(), 0);	
    		sl.send(a.machine(), a.hub(), id.name(), opcode, message);	
    	}
    }
   
    protected synchronized void finishMessage(WriteMessage w, long cnt)
    	throws IOException {

    	final int opcode = 0xDEADBEEF;
        final byte [][] message = new byte[2][];

    	message[0] = ((SmartSocketsIbis) ibis).identifierInBytes();
    	message[1] = bout.toByteArray();

    	for (ReceivePortIdentifier r : receivers.keySet().toArray(new ReceivePortIdentifier[0])) { 
    		try { 
    			sendMessage(r, opcode, message);
    		} catch (Exception e) {
    			// TODO: print to logger ?
    		}
    	} 

    	super.finishMessage(w, cnt);
    }
}
