package ibis.impl.nameServer.tcp;

import ibis.ipl.StaticProperties;
import ibis.util.*;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

class ElectionServer extends Thread implements Protocol {

	private Hashtable elections;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	ElectionServer() throws IOException { 
		elections = new Hashtable();

		serverSocket = IbisSocketFactory.createServerSocket(0, null, true /* retry */);
		setName("NameServer ElectionServer");
		start();
	} 

	int getPort() { 
		return serverSocket.getLocalPort();
	}

	private void handleElection() throws IOException, ClassNotFoundException { 

		String election = in.readUTF();
		Object candidate = in.readObject();

		Object temp = elections.get(election);

		if (temp == null) { 
			elections.put(election, candidate);
			out.writeObject(candidate);
		} else { 			
			out.writeObject(temp);
		}
	} 

	public void run() {
		Socket s;
		int opcode;
		boolean stop = false;

		while (!stop) {

			try {
				s = IbisSocketFactory.accept(serverSocket);
			} catch (Exception e) {
				throw new RuntimeException("ElectionServer: got an error " + e.getMessage());
			}

			try {
				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new ObjectInputStream(new BufferedInputStream(di));
				DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
				out = new ObjectOutputStream(new BufferedOutputStream(dos));
				
				opcode = in.readByte();

				switch (opcode) { 
				case (ELECTION): 
					handleElection();
					break;
				case (ELECTION_EXIT):
					try {
						IbisSocketFactory.close(in, out, s);
					} catch (IOException e2) { 
						// don't care.
					} 

					return;
				default: 
					System.err.println("ElectionServer: got an illegal opcode " + opcode);					
				}

				IbisSocketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in ElectionServer.run " + e1);
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
