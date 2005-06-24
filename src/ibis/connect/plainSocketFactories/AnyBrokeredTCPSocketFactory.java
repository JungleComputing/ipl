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
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Socket type that attempts to set up a brokered connection in several ways.
 * First, an ordinary client/server connection is tried (taking open port ranges into account).
 * Even though a port range is defined, it might be incorrect :-( 
 * If that fails, a
 * reversed connection is tried. The idea is that one end may be behind a
 * firewall and the other not. Firewalls often allow for outgoing connections,
 * so acting as a client may succeed. If that fails as well, a TCPSplice
 * connection is tried.
 */
public class AnyBrokeredTCPSocketFactory extends BrokeredSocketFactory {

    static Logger logger = ibis.util.GetLogger
            .getLogger(AnyBrokeredTCPSocketFactory.class.getName());

    private static PlainTCPSocketFactory plainSocketType = new PortRangeSocketFactory();

    private static class ServerInfo implements Runnable {
        IbisServerSocket server;

        private IbisSocket accpt = null;

        boolean present = false;

        public ServerInfo(Map properties) throws IOException {
            server = plainSocketType.createServerSocket(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1, properties);
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
                AnyBrokeredTCPSocketFactory.logger.debug(
                        "AnyBrokeredTCPSocketFactory server accept " + "got exception",
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

    public AnyBrokeredTCPSocketFactory() {
    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hint, Map properties) throws IOException {
        DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(out));
        DataInputStream is = new DataInputStream(new BufferedInputStream(in));

        IbisSocket s = null;

        for (int i = 0; i < 2; i++) {
            if (hint) {
                logger.debug("AnyBrokeredTCPSocketFactory server side attempt");
                ServerInfo srv = getServerSocket(properties);
                String host = srv.server.getInetAddress()
                        .getCanonicalHostName();
                int port = srv.server.getLocalPort();
                logger.debug("AnyBrokeredTCPSocketFactory server side host = " + host
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
                    logger.debug("AnyBrokeredTCPSocketFactory server side succeeds");
                    tuneSocket(s);
                    return s;
                }
                logger.debug("AnyBrokeredTCPSocketFactory server side fails");
            } else {
                String host = is.readUTF();
                int port = is.readInt();
                logger.debug("AnyBrokeredTCPSocketFactory client got host = " + host
                        + ", port = " + port);
                InetSocketAddress target = new InetSocketAddress(host, port);
                
                try {
                    s = plainSocketType.createClientSocket(target.getAddress(), target.getPort(), IPUtils.getLocalHostAddress(), 0, 2000, properties);
                } catch (Exception e) {
                    logger.debug("AnyBrokeredTCPSocketFactory client got exception", e);
                    os.writeInt(0); // failure
                    s = null;
                }
                if (s != null) {
                    os.writeInt(1); // success!
                }
                os.flush();
                if (s != null) {
                    logger.debug("AnyBrokeredTCPSocketFactory client side attempt "
                            + "succeeds");
                    tuneSocket(s);
                    return s;
                }
                logger.debug("AnyBrokeredTCPSocketFactory client side attempt fails");
            }

            hint = !hint; // try the other way around
        }

        logger.debug("AnyBrokeredTCPSocketFactory TCPSplice attempt");

        TCPSpliceSocketFactory tp = new TCPSpliceSocketFactory();
        return tp.createBrokeredSocket(in, out, hint, properties);

        // @@@ maybe also try routed messages? --Rob
    }

    private ServerInfo getServerSocket(Map properties) throws IOException {
        ServerInfo s = new ServerInfo(properties);
        Thread thr = new Thread(s);
        thr.start();
        return s;
    }
}