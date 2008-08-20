package ibis.ipl.impl.smartsockets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.PortType;

import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;

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
	
	 protected synchronized void finishMessage(WriteMessage w, long cnt)
	 	throws IOException {
		 
		 for (ReceivePortIdentifier r : receivers.keySet().toArray(new ReceivePortIdentifier[0])) { 
			 
			 
			 
			 
		 }
		 
		 super.finishMessage(w, cnt);
	 }
	
}
