package ibis.impl.tcp;

import java.util.HashMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import java.util.ArrayList;
import java.io.IOException;
import ibis.ipl.IbisError;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

class ConnectionCache {

	static final boolean DISABLE_CONNECTION_CACHE = false;

	private static class Peer { //implements Config {

		private static class Connection {
			Socket s;
			InputStream in;
			OutputStream out;
	
			Connection(Socket s, InputStream in, OutputStream out) {
				this.s   = s;
				this.in  = in;
				this.out = out;		
			}

			void kill() {
				in = null;
				out = null;
				try { 
					s.shutdownOutput();
					s.shutdownInput();
					s.close();
				} catch (IOException e) { 
					// ignore
				} 
				s = null;
			}

			public String toString() { 
				return "Connection(in = " + in + ", out = " + out + " s = " + s + ")";
			}
		} // end of inner class connection

		static final boolean DEBUG = false;
		private TcpIbisIdentifier peer;
		private ArrayList connections = new ArrayList();

		private int nextID = 0;	

		Peer(TcpIbisIdentifier peer) {
			this.peer = peer;

			if (DEBUG) { 
				System.err.println("ADDED PEER: " + peer);
			}
		} 	
/* -- not used --Rob
		synchronized boolean killSocket(OutputStream out) {
			for(int i=0; i<connections.size(); i++) {
				Connection temp = (Connection) connections.get(i);
				if(temp.out == out) { // found it
//					if (DEBUG) { 
						System.err.println("SOCKET KILL OF CONNECTION(" + out + ")");
//					}
					notifyAll();
					connections.remove(i);
					temp.kill();
					return true;
				}
			}

//			if (DEBUG) { 
				System.err.println("CONNECTION (" + out + ", ?) NOT FOUND (IN killsocket)!");
//			}

			return false;
		} 

		synchronized boolean killSocket(InputStream in) { 
			for(int i=0; i<connections.size(); i++) {
				Connection temp = (Connection) connections.get(i);
				if(temp.in == in) { // found it
//					if (DEBUG) { 
						System.err.println("SOCKET KILL OF CONNECTION(" + in + ")");
//					}
					connections.remove(i);
					temp.kill();
					notifyAll();
					return true;
				}
			}

			if (DEBUG) { 
				System.err.println("CONNECTION (" + in + ", ?) NOT FOUND (IN RELEASE INPUT)!");
			}

			return false;
		} 
*/
		synchronized boolean releaseOutput(OutputStream out) {
			for(int i=0; i<connections.size(); i++) {
				Connection temp = (Connection) connections.get(i);
				if(temp.out == out) { // found it
					if (DEBUG) { 
						System.err.println("CONNECTION(" + out + ") REMOVED");
					}
					if(temp.in != null) {
						temp.out = null;
						notifyAll();
					} else {
						connections.remove(i);
						temp.kill();
					}
					return true;
				}
			}

//			if (DEBUG) { 
				System.err.println("CONNECTION (" + out + ", ?) NOT FOUND (IN RELEASE OUTPUT)!");
//			}

			return false;
		} 

		synchronized boolean releaseInput(InputStream in) { 
			for(int i=0; i<connections.size(); i++) {
				Connection temp = (Connection) connections.get(i);
				if(temp.in == in) { // found it
					if (DEBUG) { 
						System.err.println("CONNECTION(" + in + ") REMOVED");
					}
					if(temp.out != null) {
						temp.in = null;
					} else {
						connections.remove(i);
						temp.kill();
					}
					return true;
				}
			}

//			if (DEBUG) { 
				System.err.println("CONNECTION (" + in + ", ?) NOT FOUND (IN RELEASE INPUT)!");
//			}

			return false;
		} 

		synchronized void addFreeInput(Socket s, InputStream in, OutputStream out) { 
			Connection c = new Connection(s, in, out);
			c.in = null;
			connections.add(c);
			if (DEBUG) {
				System.err.println("CONNECTION(" + c + ") ADDED TO FREEINPUT");
			}
		} 

		synchronized void addFreeOutput(Socket s, InputStream in, OutputStream out) { 
			Connection c = new Connection(s, in, out);
			c.out = null;
			connections.add(c);
			notifyAll();
			if (DEBUG) { 
				System.err.println("CONNECTION(" + c + ") ADDED TO  FREEOUTPUT");
			}
		} 

		synchronized void addUsed(Connection c) { 
			connections.add(c);
			if (DEBUG) { 
				System.err.println("CONNECTION(" + c + ") ADDED TO USED");
			}
		}

		synchronized InputStream getFreeInput() { 
			for (int i=0; i<connections.size(); i++) {
				Connection temp = (Connection) connections.get(i);
				if(temp.in == null) {
					if (DEBUG) { 
						System.err.println("FOUND FREE INPUT CONNECTION(" + temp + ")");
					}
					try {
						temp.in = temp.s.getInputStream();
					} catch (Exception e) {
						System.err.println("EEK1");
					}
					Connection res = (Connection) connections.remove(i);
					addUsed(res);
					return res.in;
				}
			}
			if (DEBUG) { 
				System.err.println("COULD NOT FIND FREE INPUT CONNECTION");
			}
			return null;
		}

		synchronized OutputStream getFreeOutput() { 
			while (true) { 
				for(int i=0; i<connections.size(); i++) {
					Connection temp = (Connection) connections.get(i);
					if (temp.out == null) { 
						if (DEBUG) { 
							System.err.println("FOUND FREE OUTPUT CONNECTION(" + temp + ")");
						}
						try {
							temp.out = temp.s.getOutputStream();
						} catch (Exception e) {
							System.err.println("EEK2");
						}

						Connection res = (Connection) connections.remove(i);
						addUsed(res);
						return res.out;
					}
				}
			
				// not found. Wait until it is added
				try { 
					if (DEBUG) { 
						System.err.println("WAITING FOR CONNECTION TO BE ADDED");
					}
					wait();
				} catch (InterruptedException e) { 
				// ignore
				}
			}
		}
	} 

	// end of innner class Peer


	// start of ConnectionCache class.
	private HashMap peers = new HashMap();

	private synchronized Peer getPeer(TcpIbisIdentifier ibis) {
		Peer p = (Peer) peers.get(ibis);

		if (p == null) {
			p = new Peer(ibis);
			peers.put(ibis, p);
		}

		return p;
	}

	OutputStream getFreeOutput(TcpIbisIdentifier ibis) {
		Peer p = getPeer(ibis);
		return p.getFreeOutput();
	}

	InputStream getFreeInput(TcpIbisIdentifier ibis) {
		if(DISABLE_CONNECTION_CACHE) return null;

		Peer p = getPeer(ibis);
		return p.getFreeInput();
	}

	void releaseOutput(TcpIbisIdentifier ibis, OutputStream out) {
		Peer p = getPeer(ibis);
		p.releaseOutput(out);
	}

	void releaseInput(TcpIbisIdentifier ibis, InputStream in) {
		Peer p = getPeer(ibis);
		p.releaseInput(in);
	}
	/*
	void killSocket(TcpIbisIdentifier ibis, OutputStream out) {
		Peer p = getPeer(ibis);
		p.killSocket(out);
	}

	void killSocket(TcpIbisIdentifier ibis, InputStream in) {
		Peer p = getPeer(ibis);
		p.killSocket(in);
	}
	*/
	void addFreeInput(TcpIbisIdentifier ibis, Socket s, InputStream in, OutputStream out) {
		Peer p = getPeer(ibis);
		p.addFreeInput(s, in, out);
	}

	void addFreeOutput(TcpIbisIdentifier ibis, Socket s, InputStream in, OutputStream out) {
		Peer p = getPeer(ibis);
		p.addFreeOutput(s, in, out);
	}
}
