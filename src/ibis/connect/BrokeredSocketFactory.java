/* $Id$ */

package ibis.connect;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public abstract class BrokeredSocketFactory extends ClientServerSocketFactory {
    public abstract IbisSocket createBrokeredSocket(InputStream in,
            OutputStream out, boolean hintIsServer,
            Map properties) throws IOException;
    
    public IbisSocket createClientSocket(InetAddress destAddr, int destPort,
            InetAddress localAddr, int localPort, int timeout, Map properties)
            throws IOException {
        throw new Error("createClientSocket not implemented by "
                + this.getClass().getName());
    }

    public IbisSocket createClientSocket(InetAddress addr, int port,
            Map properties) throws IOException {
        throw new Error("createClientSocket not implemented by "
                + this.getClass().getName());
    }

    public IbisServerSocket createServerSocket(InetSocketAddress addr,
            int backlog, Map properties) throws IOException {
        throw new Error("createServerSocket not implemented by "
                + this.getClass().getName());
    }

}