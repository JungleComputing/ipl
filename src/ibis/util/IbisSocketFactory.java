package ibis.ipl.impl.generic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import ibis.ipl.IbisIOException;


public class IbisSocketFactory { 

	static final boolean DEBUG = false;

	public static Socket createSocket(InetAddress dest, int port, boolean retry) throws IbisIOException { 
		
		boolean connected = false;
		Socket s = null;

		if (DEBUG) { 
			System.out.println("Trying to connect Socket connect to " + dest + ":" + port);
		}

		while (!connected) { 
                        try { 
                                s = new Socket(dest, port);

				if (DEBUG) { 
					System.out.println("DONE");
				}
                                connected = true;
                                s.setTcpNoDelay(true);
//				System.out.println("created socket linger = " + s.getSoLinger());
                        } catch (IOException e1) { 
				if (!retry) { 
					throw new IbisIOException("" + e1);
				} else {      
					if (DEBUG) { 
						System.out.println("Socket connect to " + dest + ":" + port + " failed, retrying");
					}
                              
					try { 
						Thread.sleep(500);
					} catch (InterruptedException e2) { 
						// don't care
					}
				}
                        }                       
                } 

		return s;
	} 
	
        /** a port of 0 means choose a free port **/
	public static ServerSocket createServerSocket(int port, boolean retry) throws IbisIOException { 

		boolean connected = false;
		ServerSocket s = null;

		while (!connected) { 
                        try {
				if (DEBUG) {
					System.out.println("Creating new ServerSocket on port " + port);
				}
                                s = new ServerSocket(port);
				if (DEBUG) {
					System.out.println("DONE");
				}
                                connected = true;
                        } catch (IOException e1) {   
				if (!retry) { 
					throw new IbisIOException("" + e1);
				} else {         
					if (DEBUG) { 
						System.out.println("ServerSocket connect to " + port + " failed, retrying");
					}
                                                         
					try { 
						Thread.sleep(500);
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
//			System.out.println("accepted socket linger = " + s.getSoLinger());
		} catch (IOException e) {
			throw new IbisIOException("Accept got an error ", e);
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
				s.shutdownInput();
				s.shutdownOutput();
				s.close();
			}
		} catch (IOException e) {
			throw new IbisIOException("Accept got an error ", e);
		}
	}
}
