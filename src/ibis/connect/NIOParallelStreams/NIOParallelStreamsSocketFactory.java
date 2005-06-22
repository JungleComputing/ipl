/* $Id$ */

package ibis.connect.NIOParallelStreams;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.IbisServerSocket;
import ibis.connect.IbisSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public class NIOParallelStreamsSocketFactory extends BrokeredSocketFactory {
    public NIOParallelStreamsSocketFactory() {

    }

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

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hint, Map p) throws IOException {
        IbisSocket s = null;
        if (p == null) {
            throw new Error(
                    "Bad property given to ParallelStreams socket factory");
        }
        s = new NIOParallelStreamsSocket(in, out, hint, p);
        return s;
    }
}