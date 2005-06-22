/* $Id$ */

package ibis.connect.plainSocketFactories;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.IbisServerSocket;
import ibis.connect.IbisSocket;
import ibis.connect.tcpSplicing.TCPSpliceSocketFactory;
import ibis.util.IPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Socket type that attempts to set up a brokered connection in several ways.
 * First, an ordinary client/server connection is tried. If that fails, a
 * reversed connection is tried. The idea is that one end may be behind a
 * firewall and the other not. Firewalls often allow for outgoing connections,
 * so acting as a client may succeed. If that fails as well, a TCPSplice
 * connection is tried.
 */
public class AnyTCPSocketFactory extends BrokeredSocketFactory {

    static Logger logger = Logger
            .getLogger(AnyTCPSocketFactory.class.getName());

    private static class ServerInfo implements Runnable {
        IbisServerSocket server;

        private IbisSocket accpt = null;

        boolean present = false;

        public ServerInfo(Map properties) throws IOException {
            server = new PlainTCPServerSocket(properties);
            server.setReceiveBufferSize(0x10000);
            server.bind(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1);
            server.setSoTimeout(60000); // one minute
        }

        public void run() {
            synchronized (this) {
                present = true;
                notifyAll();
            }
            try {
                accpt = (IbisSocket) server.accept();
            } catch (Exception e) {
                AnyTCPSocketFactory.logger.debug(
                        "AnyTCPSocketFactory server accept " + "got exception",
                        e);
            }
            if (accpt != null) {
                synchronized (this) {
                    notifyAll();
                }
            }
        }

        synchronized IbisSocket waitForAccpt() {
            while (accpt == null) {
                try {
                    wait();
                } catch (Exception e) {
                    // ignored
                }
            }
            return accpt;
        }

    }

    public AnyTCPSocketFactory() {

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
            boolean hint, Map properties) throws IOException {
        DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(out));
        DataInputStream is = new DataInputStream(new BufferedInputStream(in));

        IbisSocket s = null;

        for (int i = 0; i < 2; i++) {
            if (hint) {
                logger.debug("AnyTCPSocketFactory server side attempt");
                ServerInfo srv = getServerSocket(properties);
                String host = srv.server.getInetAddress()
                        .getCanonicalHostName();
                int port = srv.server.getLocalPort();
                logger.debug("AnyTCPSocketFactory server side host = " + host
                        + ", port = " + port);
                os.writeUTF(host);
                os.writeInt(port);
                os.flush();
                int success = is.readInt();
                if (success != 0) {
                    s = srv.waitForAccpt();
                }

                srv.server.close(); // will cause exception in accept
                // when it is still running.
                if (success != 0) {
                    logger.debug("AnyTCPSocketFactory server side succeeds");
                    tuneSocket(s);
                    return s;
                }
                logger.debug("AnyTCPSocketFactory server side fails");
            } else {
                String host = is.readUTF();
                int port = is.readInt();
                logger.debug("AnyTCPSocketFactory client got host = " + host
                        + ", port = " + port);
                InetSocketAddress target = new InetSocketAddress(host, port);
                s = new PlainTCPSocket(properties);
                try {
                    s.connect(target, 2000);
                    // s.connect(target);
                    // No, a connect without timeout sometimes just hangs.
                } catch (Exception e) {
                    logger.debug("AnyTCPSocketFactory client got exception", e);
                    os.writeInt(0); // failure
                    s = null;
                }
                if (s != null) {
                    os.writeInt(1); // success!
                }
                os.flush();
                if (s != null) {
                    logger.debug("AnyTCPSocketFactory client side attempt "
                            + "succeeds");
                    tuneSocket(s);
                    return s;
                }
                logger.debug("AnyTCPSocketFactory client side attempt fails");
            }

            hint = !hint; // try the other way around
        }

        logger.debug("AnyTCPSocketFactory TCPSplice attempt");

        TCPSpliceSocketFactory tp = new TCPSpliceSocketFactory();
        return tp.createBrokeredSocket(in, out, hint, properties);

        // @@@ first try port range sockets
        // @@@ maybe also try routed messages? --Rob
    }

    private ServerInfo getServerSocket(Map properties) throws IOException {
        ServerInfo s = new ServerInfo(properties);
        Thread thr = new Thread(s);
        thr.start();
        return s;
    }
}