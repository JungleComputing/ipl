package ibis.impl.nameServer.tcp;

import ibis.ipl.ReceivePortIdentifier;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;
import ibis.util.IbisSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

class ReceivePortNameServer extends Thread implements Protocol {

	private Hashtable ports;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	ReceivePortNameServer() throws IOException { 
		ports = new Hashtable();
		serverSocket = IbisSocketFactory.createServerSocket(0, null, true);
		setName("ReceivePort Name Server");
		start();
	} 

	int getPort() { 
		return serverSocket.getLocalPort();
	}

	private void handlePortNew() throws IOException, ClassNotFoundException {

		ReceivePortIdentifier id, storedId;

		String name = in.readUTF();
		id = (ReceivePortIdentifier) in.readObject();

		/* Check wheter the name is in use. */
		storedId = (ReceivePortIdentifier) ports.get(name);

		if (storedId != null) { 
			out.writeByte(PORT_REFUSED);
		} else { 
			out.writeByte(PORT_ACCEPTED);			
			ports.put(name, id);
		} 
	}
	
	//gosia
	private void handlePortRebind() throws IOException, ClassNotFoundException {
	
		ReceivePortIdentifier id;
		
		String name = in.readUTF();
		id = (ReceivePortIdentifier) in.readObject();
		
		/* Don't check whether the name is in use. */
		out.writeByte(PORT_ACCEPTED);
		ports.put(name, id);
	}
	
	
	private void handlePortList() throws IOException, ClassNotFoundException {
		
		ArrayList goodNames = new ArrayList();
	
		String pattern = in.readUTF();
		Enumeration names = ports.keys();
		while (names.hasMoreElements()) {
		    String name = (String) names.nextElement();
		    //if(name.matches(pattern)) {
		    if(name.startsWith(pattern)) {
			goodNames.add(name);
		    }
		}
		
		out.writeByte(goodNames.size());
		for (int i=0; i<goodNames.size(); i++) {
		    out.writeUTF((String)goodNames.get(i));
		}
	}	    
	//end gosia	
	
	
	private void handlePortLookup() throws IOException, ClassNotFoundException {

		ReceivePortIdentifier storedId;

		String name = in.readUTF();

		storedId = (ReceivePortIdentifier) ports.get(name);

		if (storedId != null) { 
			out.writeByte(PORT_KNOWN);
			out.writeObject(storedId);
		} else { 
			out.writeByte(PORT_UNKNOWN);
		} 
	}
	
	private void handlePortFree() throws IOException, ClassNotFoundException {
		ReceivePortIdentifier id, old_id;

		String name = in.readUTF();
		ports.remove(name);
		out.writeByte(0); 
	} 

	public void run() {

		Socket s;
		boolean stop = false;
		int opcode;

		while (!stop) {

			try {
				s = IbisSocketFactory.accept(serverSocket);
			} catch (Exception e) {
				throw new RuntimeException("PortTypeNameServer: got an error " + e.getMessage());
			}

			try {
				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new ObjectInputStream(new BufferedInputStream(di));
				DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
				out = new ObjectOutputStream(new BufferedOutputStream(dos));
				
				opcode = in.readByte();

				switch (opcode) { 
				case (PORT_NEW):
					handlePortNew();
					break;
					
				//gosia
				case (PORT_REBIND):
					handlePortRebind();
					break;
					
				case (PORT_LIST):
					handlePortList();
					break;					
				//end gosia

				case (PORT_FREE): 
					handlePortFree();
					break;

				case (PORT_LOOKUP): 
					handlePortLookup();
					break;
				case (PORT_EXIT):
					IbisSocketFactory.close(in, out, s);
					return;
				default: 
					System.err.println("ReceivePortNameServer: got an illegal opcode " + opcode);					
				}

				IbisSocketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in ReceivePortNameServer.run " + e1);
				e1.printStackTrace();
				if (s != null) { 
					try { 
						s.close();
					} catch (IOException e2) { 
						// don't care.
					} 
				}
			}
		}
	}
}
