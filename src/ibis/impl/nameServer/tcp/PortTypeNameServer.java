package ibis.ipl.impl.nameServer.tcp;

import ibis.ipl.IbisException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.generic.*;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.EOFException;

import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

class PortTypeNameServer extends Thread implements Protocol {

	private Hashtable portTypes;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	OutputStream out;

	PortTypeNameServer() throws IOException { 
		portTypes = new Hashtable();

		serverSocket = IbisSocketFactory.createServerSocket(0, null, true);
		setName("PortType Name Server");
		start();
	} 

	int getPort() { 
		return serverSocket.getLocalPort();
	}

	private void handlePortTypeNew() throws IOException { 

		StaticProperties p = new StaticProperties();

		String name = in.readUTF();
		int size    = in.readInt();

		for (int i=0;i<size;i++) { 
			String key = in.readUTF();
			String value = in.readUTF();

			try { 
				p.add(key, value);
			} catch (IbisException e) {
				// never happens
			} 
		}

		StaticProperties temp = (StaticProperties) portTypes.get(name);

		if (temp == null) { 
			portTypes.put(name, p);
			out.write(PORTTYPE_ACCEPTED);
		} else { 			
			if (temp.equals(p)) {
				out.write(PORTTYPE_ACCEPTED);
			} else { 
				out.write(PORTTYPE_REFUSED);
				System.err.println("PortTypeNameServer: PortType " + name + " refused because of incompatible properties\n" + 
						   temp + "----\n" + p);				
			}
		}
	} 

	public void run() {
		
		Socket s;
		int opcode;

		while (true) {

			try {
				s = IbisSocketFactory.accept(serverSocket);
			} catch (Exception e) {
				throw new RuntimeException("PortTypeNameServer: got an error " + e.getMessage());
			}

			try {
				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new ObjectInputStream(new BufferedInputStream(di));

				out = new DummyOutputStream(s.getOutputStream());

				opcode = in.readByte();

				switch (opcode) { 
				case (PORTTYPE_NEW): 
					handlePortTypeNew();
					break;
				case (PORTTYPE_EXIT):
					try { 
						IbisSocketFactory.close(in, out, s);
					} catch (IOException e2) { 
						// don't care.
					} 
					return;
				default: 
					System.err.println("PortTypeNameServer: got an illegal opcode " + opcode);					
				}
				IbisSocketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in PortTypeNameServer.run " + e1);
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
