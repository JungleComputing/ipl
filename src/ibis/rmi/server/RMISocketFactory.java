package ibis.rmi.server;

import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;

public abstract class RMISocketFactory {

    public RMISocketFactory() {
    }

    public abstract Socket createSocket(String host, int port)
	    throws IOException;

    public abstract ServerSocket createServerSocket(int port)
	    throws IOException;

    public static void setSocketFactory(RMISocketFactory fac)
	    throws IOException {
	throw new IllegalArgumentException("Ibis RMI does not support socket factories. "
		+ "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    public static RMISocketFactory getSocketFactory() {
	throw new IllegalArgumentException("Ibis RMI does not support socket factories. "
		+ "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    public static RMISocketFactory getDefaultSocketFactory() {
	throw new IllegalArgumentException("Ibis RMI does not support socket factories. "
		+ "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    public static void setFailureHandler(RMIFailureHandler fh) {
    }

    public static RMIFailureHandler getFailureHandler() {
	return null;
    }

}
