package ibis.impl.nameServer.tcp;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.IbisRuntimeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

class ElectionServer extends Thread implements Protocol {

	private HashMap elections;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	ElectionServer() throws IOException { 
		elections = new HashMap();

		serverSocket = NameServerClient.socketFactory.createServerSocket(0, null, true /* retry */);
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
			if (candidate != null) {
			    elections.put(election, candidate);
			}
			out.writeObject(candidate);
		} else { 			
			out.writeObject(temp);
		}
	} 

	private void handleKill() throws IOException, ClassNotFoundException { 

		Object ids[] = (Object[]) in.readObject();
		for (Iterator key = elections.keySet().iterator();
		     key.hasNext();) {
		    String election = (String) key.next();
		    Object o = elections.get(election);
		    for (int i = 0; i < ids.length; i++) {
			if (o.equals(ids[i])) {
			    // result of election is dead. Make new election
			    // possible.
			    key.remove();
			    break;
			}
		    }
		}
	}


	public void run() {
		Socket s;
		int opcode;
		boolean stop = false;

		while (!stop) {

			try {
				s = NameServerClient.socketFactory.accept(serverSocket);
			} catch (Exception e) {
				throw new IbisRuntimeException("ElectionServer: got an error", e);
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
				case (ELECTION_KILL) :
					handleKill();
					break;
				case (ELECTION_EXIT):
					NameServerClient.socketFactory.close(in, out, s);
					return;
				default: 
					System.err.println("ElectionServer: got an illegal opcode " + opcode);					
				}

				NameServerClient.socketFactory.close(in, out, s);
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
