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
import java.util.Hashtable;

class ElectionServer extends Thread implements Protocol {

	private Hashtable elections;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	ElectionServer() throws IOException { 
		elections = new Hashtable();

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
			elections.put(election, candidate);
			out.writeObject(candidate);
		} else { 			
			out.writeObject(temp);
		}
	} 

	private void handleReelection() throws IOException, ClassNotFoundException { 

		String election = in.readUTF();
		Object candidate = in.readObject();
		Object formerRuler = in.readObject();

		Object temp = elections.get(election);

		if (temp == null) { 
			elections.put(election, candidate);
			out.writeObject(candidate);
		} else if (temp.equals(formerRuler)) {
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
				case (REELECTION) :
					handleReelection();
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
