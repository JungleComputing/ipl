/** 
    This class handles all incoming connection requests.
 **/
package ibis.ipl.impl.tcp;

import java.net.Socket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.impl.generic.*;

final class TcpPortHandler implements Runnable, TcpProtocol, Config { 
	private ServerSocket systemServer;
//	private HashMap others;
	private ConnectionCache connectionCache = new ConnectionCache();
	private ArrayList receivePorts;
	private TcpIbisIdentifier me;
	private int port;

	TcpPortHandler(TcpIbisIdentifier me) throws IOException { 
		this.me = me;

		systemServer = IbisSocketFactory.createServerSocket(0, me.address(), true);
		port = systemServer.getLocalPort();

		if(DEBUG) {
			System.out.println("PORTHANDLER: port = " + port);
		}

//		others = new HashMap();
		receivePorts = new ArrayList();
	
		ThreadPool.createNew(this);
	}

	synchronized int register(TcpReceivePort p) {
		if(DEBUG) {
			System.err.println("TcpPortHandler registered " + p.name);
		}
		receivePorts.add(p);
		return port;
	}

	void releaseOutput(TcpReceivePortIdentifier ri, OutputStream out) {
		connectionCache.releaseOutput(ri.ibis, out);
	}

	void releaseInput(TcpSendPortIdentifier si, InputStream in) {
		connectionCache.releaseInput(si.ibis, in);
	}

	boolean connect(TcpSendPort sp, TcpReceivePortIdentifier receiver) throws IOException { 
		Socket s = null;

		try { 
			boolean reuse_connection = false;

			if (DEBUG) {
				System.err.println("Creating socket for connection to " + receiver);
			}
			s = IbisSocketFactory.createSocket(receiver.ibis.address(), receiver.port, me.address(), 0 /* retry */);

			InputStream sin = s.getInputStream();
			OutputStream sout = s.getOutputStream();
			
			sout.write(NEW_CONNECTION);

			ObjectOutputStream obj_out = new ObjectOutputStream(new DummyOutputStream(sout));
			DataInputStream data_in = new DataInputStream(new DummyInputStream(sin));

			obj_out.writeObject(receiver);
			obj_out.writeObject(sp.identifier());
			obj_out.flush();
			// This is a bug: obj_out.close();

			int result = data_in.readByte();

			if (result != RECEIVER_ACCEPTED) {
				obj_out.flush();
				obj_out.close();
				data_in.close();
				sin.close();
				sout.close();
				s.close();	
				return false;
			} 

			if(DEBUG) {
				System.err.println("Sender Accepted"); 
			}

			/* the other side accepts the connection, finds the correct 
			   stream */

			result = data_in.readByte();

//			Peer p = getPeer(receiver.ibis);
			Connection c = null;

			if (result == NEW_CONNECTION) { 
				/* no unused stream found, so reuse current one */
				c = connectionCache.newConnection(receiver.ibis, s, sin, sout);
				reuse_connection = true;

				obj_out.writeInt(c.local_id);
				obj_out.flush();
				obj_out.close();

				c.remote_id = data_in.readInt();
				data_in.close();
					
				connectionCache.addFreeInput(receiver.ibis, c);

				if(DEBUG) {
					System.err.println("Created new connection to " + receiver);
				}
				sp.connect(receiver, c.out);

				if(DEBUG) {
					System.err.println("connection to " + receiver + " done");
				}
				return true;
			} else { 
				int remote_id = data_in.readInt();
				int local_id = data_in.readInt();

				data_in.close();
				obj_out.flush();
				obj_out.close();
				
				c = connectionCache.findFreeOutput(receiver.ibis, local_id, remote_id);
				connectionCache.addUsed(receiver.ibis, c);
//				c = p.findFreeOutput(local_id, remote_id);
//				p.addUsed(c);					
				if(DEBUG) {
					System.err.println("Reused connection to " + receiver);
				}
				sp.connect(receiver, c.out);	
					
				if(DEBUG) {
					System.err.println("connection to " + receiver + " done");
				}
				sin.close();
				sout.close();
				s.close();
				return true;
			} 
		} catch (IOException e) {
			try {
				s.close();
			} catch (Exception e2) {
				// Ignore.
			}
			throw new ConnectionRefusedException("Could not connect" + e);
		}
	} 

	void quit() { 
		try { 
			Socket s = IbisSocketFactory.createSocket(me.address(), port, me.address(), 0 /* retry */);
			OutputStream sout = s.getOutputStream();
			sout.write(QUIT_IBIS);
			sout.flush();
			sout.close();			
		} catch (Exception e) { 
			// Ignore
		}
	}

	private synchronized TcpReceivePort findReceivePort(TcpReceivePortIdentifier ident) {
		TcpReceivePort rp = null;
		int i = 0;

		while (rp == null && i < receivePorts.size()) { 
						
			TcpReceivePort temp = (TcpReceivePort) receivePorts.get(i);
						
			if (ident.equals(temp.identifier())) {
				if (DEBUG) {
					System.err.println("findRecPort found " + ident + " == " + 
							   temp.identifier());
				}
				rp = temp;
			}
			i++;
		}

		return rp;
	}

	/* returns: was it a close i.e. do we need to exit this thread */
	private boolean handleRequest(Socket s) throws Exception {
		if (DEBUG) { 
			System.err.println("portHandler on " + me + " got new connection from " + 
					   s.getInetAddress() + ":" + s.getPort() + 
					   " on local port " + s.getLocalPort());
		}

		OutputStream out = s.getOutputStream();
		InputStream in   = s.getInputStream();

		if (DEBUG) {
			System.err.println("Getting streams DONE"); 
		}

		if (in.read() == QUIT_IBIS) { 
			if (DEBUG) {
				System.err.println("it is a quit"); 
			}

			systemServer.close();
			s.close();
			if (DEBUG) {
				System.err.println("it is a quit: RETURN"); 
			}

			return true;
		}

		if (DEBUG) {
			System.err.println("it isn't a quit"); 
		}
				
		ObjectInputStream obj_in  = new ObjectInputStream(new DummyInputStream(in));
		DataOutputStream data_out = new DataOutputStream(new DummyOutputStream(out));

		if (DEBUG) {
			System.err.println("S Reading Data"); 
		}

		TcpReceivePortIdentifier receive = (TcpReceivePortIdentifier) obj_in.readObject();
		TcpSendPortIdentifier send       = (TcpSendPortIdentifier) obj_in.readObject();
		TcpIbisIdentifier ibis           = send.ibis;
					
		if (DEBUG) {
			System.err.println("S finding RP"); 
		}
					
		/* First, try to find the receive port this message is for... */
		TcpReceivePort rp = findReceivePort(receive);

		if (DEBUG) {
			System.err.println("S  RP = " + (rp == null ? "not found" : rp.identifier().toString() )); 
		}
					
		if (rp == null || !rp.connectionAllowed(send)) {
			/* If we cannot find it, return access denied */
			data_out.writeByte(RECEIVER_DENIED);
			data_out.flush();
			data_out.close();
			obj_in.close();	
			out.close();
			in.close();
			s.close();
			return false;
		}

		/* It accepts the connection, now we try to find an unused stream 
		   originating at the sending machine */ 
		if (DEBUG) {
			System.err.println("S getting peer");
		}

//		Peer p = getPeer(ibis);
		Connection c = connectionCache.findFreeInput(ibis);

		if (DEBUG) {
			System.err.println("S found connection " + c);
		}
				
		if (c == null) { 
			/* no unused stream found, so reuse current socket */
			c = connectionCache.newConnection(ibis, s, in, out);

			data_out.writeByte(RECEIVER_ACCEPTED);
			data_out.writeByte(NEW_CONNECTION);
			data_out.writeInt(c.local_id);
			data_out.flush();
			data_out.close();

			c.remote_id = obj_in.readInt();
			obj_in.close();
			// do not close s here, we just reused it :-)

			connectionCache.addFreeOutput(ibis, c);
		} else { 
			data_out.writeByte(RECEIVER_ACCEPTED);
			data_out.writeByte(EXISTING_CONNECTION);
			data_out.writeInt(c.local_id);
			data_out.writeInt(c.remote_id);
			data_out.flush();
			data_out.close();
			obj_in.close();						
			out.close();
			in.close();
			s.close();

			connectionCache.addUsed(ibis, c);
		}
							
		if (DEBUG) {
			System.err.println("S connected " + c);
		}
							
		/* add the connection to the receiveport. */
		rp.connect(send, c.in, c.local_id);

		if (DEBUG) {
			System.err.println("S connect done ");
		}
		
		return false;
	}

	public void run() { 
		/* this thread handles incoming connection request from the connect(TcpSendPort) call */

		if(DEBUG) {
			System.err.println("TcpPortHandler running");
		}

		while (true) {
			Socket s = null;
		
			if (DEBUG) { 
				System.err.println("PortHandler on " + me + " doing new accept()");
			}

			try {
				s = IbisSocketFactory.accept(systemServer);
			} catch (Exception e ) {
				/* if the accept itself fails, we have a fatal problem.
				   Close this receiveport.
				   @@@ needs fixing, probably report error to user,
				   and cleanup the ReceivePort state. --Rob
				*/
				try {
					System.err.println("EEK: TcpPortHandler:run: got exception in accept ReceivePort closing!: " + e);
					e.printStackTrace();
					if(s != null) s.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				cleanup();
				return;
			}

			boolean exit = false;
			try {
				exit = handleRequest(s);
			} catch (Exception e) { 
				try {
					System.err.println("EEK: TcpPortHandler:run: got exception (closing this socket only: " + e);
					if(s != null) s.close();
				} catch (Exception e1) {
					e.printStackTrace();
					e1.printStackTrace();
				}
			}
			if(exit) {
				cleanup();
				return;
			}
		}
	}

	private void cleanup() {
		try {
			if(systemServer != null) systemServer.close();
			systemServer = null;
		} catch (Exception e) {
			// Ignore
		}
	}
}
