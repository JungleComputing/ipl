package ibis.ipl.impl.tcp;

import java.net.Socket;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.Vector;

import java.io.PrintStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;

// import ibis.ipl.*;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.impl.generic.*;

final class TcpPortHandler implements Runnable, TcpProtocol, Config { 

	private FileWriter f;
//	private PrintWriter print;

	private ServerSocket systemServer;
	private Hashtable others;
	private Vector receivePorts;
	private Thread thread;
	private TcpIbisIdentifier me;
	private int port;

	TcpPortHandler(TcpIbisIdentifier me) throws IOException { 
		
//		f = new FileWriter(me.name + ".TcpPortHandler");
//		print = new PrintWriter(f);
		
		this.me = me;

		systemServer = IbisSocketFactory.createServerSocket(0, me.address(), true);
		port = systemServer.getLocalPort();

		if(DEBUG) {
			System.out.println("PORTHANDLER: port = " + port);
		}

		others = new Hashtable();
		receivePorts = new Vector();
		
		thread = new Thread(this, "TCP Port Handler");
		thread.start();
	}

	int register(TcpReceivePort p) {
		if(DEBUG) {
			System.err.println("TcpPortHandler registered " + p.name);
		}
		receivePorts.add(p);
		//receivePorts.put(p.identifier(), p);
		return port;
	} 

	private synchronized Peer getPeer(TcpIbisIdentifier ibis) { 
		Peer p = (Peer) others.get(ibis);

		if (p == null) { 					
			p = new Peer(ibis);
			others.put(ibis, p);
		} 

		return p;
	} 

	void releaseOutput(TcpReceivePortIdentifier ri, int id) {
		Peer p = getPeer(ri.ibis);

		if (p == null || !p.releaseOutput(id)) { 
			System.err.println("EEEK : releasing an unknown connection!!!");
			new Exception().printStackTrace();
		}
	}

	void releaseInput(TcpSendPortIdentifier si, int id) {
		Peer p = getPeer(si.ibis);

		if (p == null || !p.releaseInput(id)) { 
			System.err.println("EEEK : releasing an unknown connection!!!");
			new Exception().printStackTrace();			
		}	
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

			if (result == RECEIVER_ACCEPTED) {
				if(DEBUG) {
					System.err.println("Sender Accepted"); 
				}

				/* the other side accepts the connection, finds the correct 
				   stream */

				Peer p = getPeer(receiver.ibis);

				result = data_in.readByte();

				Connection c = null;

				if (result == NEW_CONNECTION) { 
					/* no unused stream found, so reuse current one */						
					c = new Connection(s, sin, sout);					
					reuse_connection = true;
					c.local_id = p.getID();

					obj_out.writeInt(c.local_id);
					obj_out.flush();
					obj_out.close();

					c.remote_id = data_in.readInt();
					data_in.close();
					
					p.addFreeInput(c);

					if(DEBUG) {
						System.err.println("Created new connection to " + receiver);
					}
				} else { 
					int remote_id = data_in.readInt();
					int local_id = data_in.readInt();

					data_in.close();
					obj_out.flush();
					obj_out.close();

					c = p.findFreeOutput(local_id, remote_id);
					p.addUsed(c);					
					if(DEBUG) {
						System.err.println("Reused connection to " + receiver);
					}
				} 

				if(DEBUG) {
					System.err.println("Found output " + c);
				}

				sp.connect(receiver, c.out, c.local_id);	

				if(DEBUG) {
					System.err.println("connection to " + receiver + " done");
				}

				if (!reuse_connection) { 
					sin.close();
					sout.close();
					s.close();
				}
					
				return true;
			} else { 
				obj_out.flush();
				obj_out.close();
				data_in.close();
				sin.close();
				sout.close();
				s.close();	
				return false;
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
			sout.write(FREE);
			sout.flush();
			sout.close();			
		} catch (Exception e) { 
			// Ignore
		}
	} 

	public void run() { 
		PrintStream print = System.err;

		/* this thread handles incoming connection request from the connect(TcpSendPort) call */

		if(DEBUG) {
			System.err.println("TcpPortHandler running");
		}
				
		while (true) {

			boolean reuse_connection = false;			
			TcpReceivePort rp = null;
			Socket s = null;
		
			try { 
				if (DEBUG) { 
					System.err.println("PortHandler on " + me + " doing new accept()");
				}

				s = IbisSocketFactory.accept(systemServer);

//				System.err.println("********************** Accepted Socket from " + s.getInetAddress() + "*****************************");

				if (DEBUG) { 
					System.err.println("portHandler on " + me + " got new connection from " + s.getInetAddress() + ":" + s.getPort() + " on local port " + s.getLocalPort());
				}


				if (DEBUG) {
					print.println("Getting streams"); 
				}
     				OutputStream out = s.getOutputStream();
				InputStream in   = s.getInputStream();

				if (DEBUG) {
					print.println("Getting streams DONE"); 
				}
				if (in.read() == FREE) { 
					if (DEBUG) {
						print.println("it is a free"); 
					}

					systemServer.close();
					s.close();
					if (DEBUG) {
						print.println("it is a free: RETURN"); 
					}

					return;
				} else { 
					if (DEBUG) {
						print.println("it isn't a free"); 
					}
				
					ObjectInputStream obj_in  = new ObjectInputStream(new DummyInputStream(in));
					DataOutputStream data_out = new DataOutputStream(new DummyOutputStream(out));

					if (DEBUG) {
						print.println("S Reading Data"); 
					}
					
					TcpReceivePortIdentifier receive = (TcpReceivePortIdentifier) obj_in.readObject();
					TcpSendPortIdentifier send       = (TcpSendPortIdentifier) obj_in.readObject();
					TcpIbisIdentifier ibis           = send.ibis;
					
					if (DEBUG) {
						print.println("S finding RP"); 
					}
					
				        /* First, try to find the receive port this message is for... */
					int i = 0;
					
					while (rp == null && i < receivePorts.size()) { 
						
						TcpReceivePort temp = (TcpReceivePort) receivePorts.get(i);
						
						if (receive.equals(temp.identifier())) {
							if (DEBUG) {
								print.println("TcpPortHandler found " + receive + " == " + 
									      temp.identifier());
							}
							rp = temp;
						}
						i++;
					}
					
					if (DEBUG) {
						print.println("S  RP = " + (rp == null ? "not found" : rp.identifier().toString() )); 
					}
					
//				TcpReceivePort rp = (TcpReceivePort) receivePorts.get(receive);
					
					if (rp == null) { 					
						/* If we cannot find it */
						data_out.writeByte(RECEIVER_DENIED);
						data_out.flush();
						data_out.close();
						obj_in.close();	
					} else { 
						if (DEBUG) {
							print.println("S testing RP.may_connect()");
						}
						
						if (rp.setupConnection(send)) { 							
							/* It accepts the connection, now we try to find an unused stream 
							   originating at the sending machine */ 
							
							if (DEBUG) {
								print.println("S getting peer");
							}
							
							Peer p = getPeer(ibis);
							
							Connection c = p.findFreeInput();
							
							if (DEBUG) {
								print.println("S found connection " + c);
							}
				
							if (c == null) { 
								/* no unused stream found, so reuse current one */
								c = new Connection(s, in, out);
								reuse_connection = true;
								c.local_id = p.getID();
								
								data_out.writeByte(RECEIVER_ACCEPTED);
								data_out.writeByte(NEW_CONNECTION);
								data_out.writeInt(c.local_id);
								data_out.flush();
								data_out.close();
								
								c.remote_id = obj_in.readInt();
								obj_in.close();
								
								p.addFreeOutput(c);
							} else { 
								data_out.writeByte(RECEIVER_ACCEPTED);
								data_out.writeByte(EXISTING_CONNECTION);
								data_out.writeInt(c.local_id);
								data_out.writeInt(c.remote_id);
								data_out.flush();
								data_out.close();
								obj_in.close();						
								
								p.addUsed(c);
							}
							
							if (DEBUG) {
								print.println("S connected " + c);
							}
							
							/* add the connection to the receiveport. */
							rp.connect(send, c.in, c.local_id);			
						
							if (DEBUG) {
								print.println("S connect done ");
								print.flush();
							}
						} else { 	
							if (DEBUG) {
								print.println("TcpPortHandler: receiveport denied the connection");
							}
							data_out.writeByte(RECEIVER_DENIED);
							data_out.flush();
							data_out.close();
							obj_in.close();	
						}
					}
					
					if (!reuse_connection) { 
						out.close();
						in.close();
						s.close();
					}
					
//				}						
				}
			} catch (Exception e ) { 
				try { 
					System.err.println("EEK: TcpPortHandler:run: got exception: " + e);
					systemServer.close();
					s.close();
				} catch (Exception e1) { 						
					e.printStackTrace();	
					e1.printStackTrace();	
				}
				return;

//				e.printStackTrace();
//				System.exit(1);
			}
		}			
	}			
} 
