package ibis.util;

import ibis.ipl.ConnectionTimedOutException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class IbisNormalSocketFactory extends IbisSocketFactory {
	boolean firewall = false;
	int portNr = 0;
	int startRange = 0;
	int endRange = 0;

	public IbisNormalSocketFactory() {
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

    /** Simple ServerSocket factory
     */
    public ServerSocket createServerSocket(int port, int backlog, InetAddress addr) 
	throws IOException {
	ServerSocket s = null;
	s = new ServerSocket(port, backlog, addr);
	return s;
    }

    /** Simple client Socket factory
     */
    public Socket createSocket(InetAddress rAddr, int rPort) 
	throws IOException {
	Socket s = null;
	s = new Socket(rAddr, rPort);
	return s;
    }

	public synchronized int allocLocalPort() {
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

        /** 
	    A host can have multiple local IPs (sierra)
	    if localIP is null, try to bind to the first of this machine's IP addresses.

	    timeoutMillis < 0  means do not retry, throw exception on failure.
	    timeoutMillis == 0 means retry until success.
	    timeoutMillis > 0  means block at most for timeoutMillis milliseconds, then return. 
	    An IOException is thrown when the socket was not properly created within this time.
	**/
	// timneout is not implemented correctly @@@@
	// this can only be done with 1.4 functions... --Rob
	public Socket createSocket(InetAddress dest, int port, InetAddress localIP, long timeoutMillis) throws IOException { 
		boolean connected = false;
		Socket s = null;
		long startTime = System.currentTimeMillis();
		long currentTime = 0;

		if (localIP != null && dest.getHostAddress().equals("127.0.0.1")) {
			/* Avoid ConnectionRefused exception */
			dest = InetAddress.getLocalHost();
		}

		while (!connected) {
			int localPort = allocLocalPort();
			if (DEBUG) {
				System.err.println("Trying to connect Socket (local:" + (localIP==null?"any" : localIP.toString()) + ":" + localPort + ") to " + dest + ":" + port);
			}

                        try {
				s = null;

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
//				s.setSoTimeout(1000);
//				System.err.println("created socket linger = " + s.getSoLinger());
                        } catch (IOException e1) { 
				if (DEBUG) { 
					System.err.println("Socket connect to " + dest + ":" + port + " failed (" + e1 + ")");
					e1.printStackTrace();
				}

				if (s != null && ! s.isClosed()) {
					s.close();
				}

//System.err.println("Socket connect hits " + e1);

				if(timeoutMillis < 0) {
					throw new ConnectionTimedOutException("" + e1);
				} else if (timeoutMillis == 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e2) { 
						// don't care
					}

				} else {
					currentTime = System.currentTimeMillis();
					if(currentTime - startTime < timeoutMillis) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e2) { 
							// don't care
						}
					} else {
						throw new ConnectionTimedOutException("" + e1);
					}
				}
                        }
                } 

		return s;
	} 

    
	
        /** A host can have multiple local IPs (sierra)
	    if localIP is null, try to bind to the first of this machine's IP addresses.
            port of 0 means choose a free port **/
	public ServerSocket createServerSocket(int port, InetAddress localAddress, boolean retry) throws IOException { 
		boolean connected = false;
		/*Ibis*/ServerSocket s = null;
		int localPort;

		if(localAddress == null) {
			localAddress = InetAddress.getLocalHost();
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
				
				s = createServerSocket(localPort, 50, localAddress);

				if (DEBUG) {
					System.err.println("DONE, with port = " + s.getLocalPort());
				}
                                connected = true;
                        } catch (IOException e1) {
				if (!retry) {
					throw new IOException("" + e1);
				} else {         
					if (DEBUG) { 
						System.err.println("ServerSocket connect to " + port + " failed: " + e1 + "; retrying");
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
}
