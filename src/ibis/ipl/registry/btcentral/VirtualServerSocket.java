package ibis.ipl.registry.btcentral;

import java.io.IOException;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class VirtualServerSocket {
	  //private final String myServiceUUID = "2d26618601fb47c28d9f10b8ec891363";
	    //private final UUID MYSERVICEUUID_UUID = new UUID(myServiceUUID, false);
	    //private LocalDevice localDevice; // local Bluetooth Manager
	    // Define the server connection URL
	    private String connURL;
	    private String openURL;
	    
		StreamConnectionNotifier streamConnNotifier;
		
		public VirtualServerSocket(VirtualSocketAddress addr, boolean master){
			openURL = addr.toString(); 
			try{
				openURL = "btspp://localhost:" + addr.toString() + ";name=IbisRegistry;master=" + master;
				System.out.println("Opening openURL " + openURL);
				streamConnNotifier = (StreamConnectionNotifier)Connector.open(openURL);				
				connURL = LocalDevice.getLocalDevice().getRecord(streamConnNotifier).getConnectionURL(0, false);
			}catch(Exception e){
				//TODO: something...		
				e.printStackTrace();
			}
		}
		
		public void close() throws IOException {
			streamConnNotifier.close();			
		}
		
		public VirtualSocketAddress getLocalSocketAddress(){
			return new VirtualSocketAddress(connURL);
		}
		
		public StreamConnection accept() throws IOException{
			return streamConnNotifier.acceptAndOpen();			
		}
}
