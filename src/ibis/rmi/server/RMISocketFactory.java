package ibis.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class RMISocketFactory {

    public RMISocketFactory() {
        // nothing
    }

    public abstract Socket createSocket(String host, int port)
	    throws IOException;

    public abstract ServerSocket createServerSocket(int port)
	    throws IOException;

    public static void setSocketFactory(RMISocketFactory fac) {
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
        // not implemented
    }

    public static RMIFailureHandler getFailureHandler() {
	return null;
    }

}
