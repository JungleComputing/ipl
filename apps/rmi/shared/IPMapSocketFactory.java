import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.rmi.server.RMISocketFactory;

import ibis.util.IPUtils;

public class IPMapSocketFactory extends RMISocketFactory {

    private final static boolean DEBUG = true;

    private InetAddress myAddr = IPUtils.getLocalHostAddress();

    public IPMapSocketFactory() {
	if (DEBUG) {
	    System.err.println("My local hostaddr " + myAddr);
	}
    }

    public ServerSocket createServerSocket(int port) throws IOException {
	ServerSocket s = new ServerSocket(port, 0, myAddr);
	if (DEBUG) {
	    System.err.println("Created new ServerSocket " + s);
	}
	// s.setTcpNoDelay(true);
	return s;
    }

    public Socket createSocket(String host, int port) throws IOException {
	Socket s = null;
	try {
	    s = new Socket(host, port, myAddr, 0);
	} catch (IOException e) {
	    s = new Socket(host, port);
	}
	if (DEBUG) {
	    System.err.println("Created new Socket " + s);
	}
	s.setTcpNoDelay(true);
	return s;
    }

}
