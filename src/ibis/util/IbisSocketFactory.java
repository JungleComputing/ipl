package ibis.util;

import ibis.ipl.ConnectionTimedOutException;

import ibis.connect.socketFactory.ExtSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class IbisSocketFactory {

    static final boolean DEBUG = false;

    /** Simple ServerSocket factory
     */
    public abstract ServerSocket createServerSocket(int port,
						    int backlog,
						    InetAddress addr) 
	throws IOException;

    /** Simple client Socket factory
     */
    public abstract Socket createSocket(InetAddress rAddr, int rPort) 
	throws IOException;

    public abstract int allocLocalPort();

    /** 
	A host can have multiple local IPs (sierra)
	if localIP is null, try to bind to the first of this machine's IP addresses.

	timeoutMillis < 0  means do not retry, throw exception on failure.
	timeoutMillis == 0 means retry until success.
	timeoutMillis > 0  means block at most for timeoutMillis milliseconds, then return. 
	An IOException is thrown when the socket was not properly created within this time.
    **/
    public abstract Socket createSocket(InetAddress dest,
					int port,
					InetAddress localIP,
					long timeoutMillis)
	    throws IOException;
    
	
    /** A host can have multiple local IPs (sierra).
	If localIP is null, try to bind to the first of this machine's
	IP addresses. Port of 0 means choose a free port **/
    public abstract ServerSocket createServerSocket(int port,
						    InetAddress localAddress,
						    boolean retry)
	    throws IOException;

    /** Use this to accept, it sets the socket parameters. **/
    public Socket accept(ServerSocket a) throws IOException {
	Socket s;
	s = a.accept();
	s.setTcpNoDelay(true);
//		s.setSoTimeout(1000);
//		System.err.println("accepted socket linger = " + s.getSoLinger());

	if(DEBUG) {
	    System.out.println("accepted new connection from " + s.getInetAddress() + ":" + s.getPort() + ", local = " + s.getLocalAddress() + ":" + s.getLocalPort());
	}
	
	return s;
    }

    /** Use this to close sockets, it nicely shuts down the streams, etc. **/
    public void close(InputStream in, OutputStream out, Socket s) {
	if(out != null) {
	    try {
		out.flush();
	    } catch (Exception e) {
		// ignore
	    }
	    try {
		out.close();
	    } catch (Exception e) {
		// ignore
	    }
	}

	if(in != null) {
	    try {
		in.close();
	    } catch (Exception e) {
		// ignore
	    }
	}

	if(s != null) {
	    try {
		s.close();
	    } catch (Exception e) {
		// ignore
	    }
	}
    }

    public void shutdown() {
    }

    public static IbisSocketFactory createFactory(String name) {
	if (name.equals("ibis-connect")) {
	    return new IbisConnectSocketFactory();
	}
	return new IbisNormalSocketFactory();
    }
}
