/** 
    This class handles all incoming connection requests.
 **/
package ibis.impl.tcp;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.IbisError;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;
import ibis.util.IbisSocketFactory;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

final class TcpPortHandler implements Runnable, TcpProtocol { //, Config { 
	static final boolean DEBUG = false;

	private ServerSocket systemServer;
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
/*
	void killSocket(TcpReceivePortIdentifier ri, OutputStream out) {
		connectionCache.killSocket(ri.ibis, out);
	}

	void killSocket(TcpSendPortIdentifier si, InputStream in) {
		connectionCache.killSocket(si.ibis, in);
	}
*/
	OutputStream connect(TcpSendPort sp, TcpReceivePortIdentifier receiver, long timeout) throws IOException { 
		Socket s = null;

		try { 
			boolean reuse_connection = false;

			if (DEBUG) {
				System.err.println("Creating socket for connection to " + receiver);
			}
			s = IbisSocketFactory.createSocket(receiver.ibis.address(), receiver.port, me.address(), timeout);

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
				return null;
			} 

			if(DEBUG) {
				System.err.println("Sender Accepted"); 
			}

			/* the other side accepts the connection, finds the correct 
			   stream */
			result = data_in.readByte();

			if (result == NEW_CONNECTION) { 
				/* no unused stream found, so reuse current one */
				reuse_connection = true;
				obj_out.flush();
				obj_out.close();
				data_in.close();
					
				connectionCache.addFreeInput(receiver.ibis, s, sin, sout);

				if(DEBUG) {
					System.err.println("Created new connection to " + receiver);
				}

				return sout;
			} else if (result == EXISTING_CONNECTION) {
				data_in.close();
				obj_out.flush();
				obj_out.close();
				
				OutputStream out = connectionCache.getFreeOutput(receiver.ibis);
				if(DEBUG) {
					System.err.println("Reused connection to " + receiver);
				}

				sin.close();
				sout.close();
				s.close();
				return out;
			} else {
				throw new IbisError("Illegal opcode in TcpPortHandler:connect");
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

		InputStream cin = connectionCache.getFreeInput(ibis);

		if (DEBUG) {
			if(cin != null) {
				System.err.println("S found connection " + cin);
			} else {
				System.err.println("no connection found");
			}
		}

		if (cin == null) {
			/* no unused stream found, so reuse current socket */
			data_out.writeByte(RECEIVER_ACCEPTED);
			data_out.writeByte(NEW_CONNECTION);
			data_out.flush();
			data_out.close();

			obj_in.close();
			// do not close s here, we just reused it :-)

			connectionCache.addFreeOutput(ibis, s, in, out);
			cin = in;
		} else {
			data_out.writeByte(RECEIVER_ACCEPTED);
			data_out.writeByte(EXISTING_CONNECTION);
			data_out.flush();
			data_out.close();
			obj_in.close();
			out.close();
			in.close();
			s.close();
		}

		if (DEBUG) {
			System.err.println("S connected " + cin);
		}
							
		/* add the connection to the receiveport. */
		rp.connect(send, cin);

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
				*/
				try {
					System.err.println("EEK: TcpPortHandler:run: got exception in accept ReceivePort closing!: " + e);
					e.printStackTrace();
					if(s != null) s.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				cleanup();
				throw new IbisError("Fatal: PortHandler could not do an accept");
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
