package ibis.ipl.impl.generic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import ibis.ipl.IbisIOException;
import java.util.*;

public class IbisSocketFactory {
	private static final boolean DEBUG = false;

	private static boolean firewall = false;
	private static int portNr = 0;
	private static int startRange = 0;
	private static int endRange = 0;

	static {
		Properties p = System.getProperties();
		String range = p.getProperty("firewall_range");
		if(range != null) {
			int pos = range.indexOf('-');
			if(pos < 0) {
				System.err.println("Specify a firewall range in this format: 3000-4000.");
				System.exit(1);
			} else {
				String from = range.substring(0, pos);
				String to = range.substring(pos+1, range.length());

				try {
					startRange = Integer.parseInt(from);
					endRange = Integer.parseInt(to);
					firewall = true;
					portNr = startRange;
				} catch (Exception e) {
					System.err.println("Specify a firewall range in this format: 3000-4000.");
					System.exit(1);
				}
			}
		}
	}

	private synchronized static int allocLocalPort() {
		if(firewall) {
			int res = portNr++;
			if(portNr >= endRange) {
				portNr = startRange;
				System.err.println("WARNING, used more ports than available within firewall range. Wrapping around");
			}
			return res;
		} else {
			return 0; /* any free port */
		}
	}

        /** A host can have multiple local IPs (sierra)
	    if localIP is null, try to bind to the first of this machine's IP addresses. **/
	public static Socket createSocket(InetAddress dest, int port, InetAddress localIP, boolean retry) throws IbisIOException { 
		boolean connected = false;
		Socket s = null;

		if (localIP != null && dest.getHostAddress().equals("127.0.0.1")) {
		    /* Avoid ConnectionRefused exception */
		    try {
			    dest = InetAddress.getLocalHost();
		    } catch (IOException e1) { 
			    throw new IbisIOException("" + e1);
		    }
		}

		while (!connected) {
			int localPort = allocLocalPort();
			if (DEBUG) {
				System.err.println("Trying to connect Socket (local:" + (localIP==null?"any" : localIP.toString()) + ":" + localPort + ") to " + dest + ":" + port);
			}
                        try {
				if(localIP == null) {
					s = new Socket(dest, port);
				} else {
					s = new Socket(dest, port, localIP, localPort);
				}

				if (DEBUG) {
					System.err.println("DONE, local port: " + s.getLocalPort());
				}
                                connected = true;
                                s.setTcpNoDelay(true);
//				System.err.println("created socket linger = " + s.getSoLinger());
                        } catch (IOException e1) { 
				if (!retry) { 
					throw new IbisIOException("" + e1);
				} else {      
					if (DEBUG) { 
						System.err.println("Socket connect to " + dest + ":" + port + " failed (" + e1 + "), retrying");
						e1.printStackTrace();
					}
                              
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e2) { 
						// don't care
					}
				}
                        }
                } 

		return s;
	} 
	
        /** A host can have multiple local IPs (sierra)
	    if localIP is null, try to bind to the first of this machine's IP addresses.
            port of 0 means choose a free port **/
	public static ServerSocket createServerSocket(int port, InetAddress localAddress, boolean retry) throws IbisIOException { 
		boolean connected = false;
		/*Ibis*/ServerSocket s = null;
		int localPort;

		if(localAddress == null) {
			try {
				localAddress = InetAddress.getLocalHost();
			} catch (IOException e1) { 
				throw new IbisIOException("" + e1);
			}
		}

		while (!connected) { 
                        try {
				if(port == 0) {
					localPort = allocLocalPort();
				} else {
					localPort = port;
				}

				if (DEBUG) {
					System.err.println("Creating new ServerSocket on " + localAddress + ":" + localPort);
				}

				s = new /*Ibis*/ServerSocket(localPort, 50, localAddress);

				if (DEBUG) {
					System.err.println("DONE, with port = " + s.getLocalPort());
				}
                                connected = true;
                        } catch (IOException e1) {
				if (!retry) {
					throw new IbisIOException("" + e1);
				} else {         
					if (DEBUG) { 
						System.err.println("ServerSocket connect to " + port + " failed, retrying");
					}
                                                         
					try { 
						Thread.sleep(1000);
					} catch (InterruptedException e2) { 
						// don't care
					}
				}
                        }                       
                } 

		return s;
	} 

	/** Use this to accept, it sets the socket parameters. **/
	public static Socket accept(ServerSocket a) throws IbisIOException {
		Socket s;
		try {
			s = a.accept();
			s.setTcpNoDelay(true);
//			System.err.println("accepted socket linger = " + s.getSoLinger());
		} catch (IOException e) {
			throw new IbisIOException("Accept got an error ", e);
		}

		if(DEBUG) {
			System.out.println("accepted new connection from " + s.getInetAddress() + ":" + s.getPort() + ", local = " + s.getLocalAddress() + ":" + s.getLocalPort());
		}
		
		return s;
	}

	/** Use this to close sockets, it nicely shuts down the streams, etc. **/
	public static void close(InputStream in, OutputStream out, Socket s) throws IbisIOException {
		try {
			if(out != null) {
				out.flush();
				out.close();
			}

			if(in != null) {
				in.close();
			}

			if(s != null) {
				s.close();
			}
		} catch (IOException e) {
			throw new IbisIOException("Accept got an error ", e);
		}
	}
}
