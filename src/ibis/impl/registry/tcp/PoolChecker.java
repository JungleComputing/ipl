/* $Id: PoolChecker.java 3799 2006-06-01 16:14:44Z ceriel $ */

package ibis.impl.registry.tcp;

import ibis.util.IPUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

public class PoolChecker implements Protocol {

    private static Logger logger = Logger.getLogger(PoolChecker.class);

    private String poolName;
    private String serverHost;
    private int port;
    private int sleep;

    public PoolChecker(String poolName, String serverHost, int port, int sleep) {
        this.poolName = poolName;
        this.serverHost = serverHost;
        this.port = port;
        this.sleep = sleep;
    }

    static void check(String poolName, String serverHost, int port, int sleep)
            throws IOException {
        PoolChecker ck = new PoolChecker(poolName, serverHost, port, sleep);
        ck.run();
    }

    public void run() {
        do {
            if (sleep != 0) {
                try {
                    Thread.sleep(sleep * 1000);
                } catch(Exception e) {
                    // ignored
                }
            }
            try {
                check(poolName, serverHost, port);
            } catch(Exception e) {
                // Ignored
            }
        } while (sleep != 0);
    }

    public static void check(String poolName, String serverHost, int port)
            throws IOException {
        InetAddress myAddress = IPUtils.getAlternateLocalHostAddress();

        if (serverHost.equals("localhost")) {
            serverHost = myAddress.getHostName();
        }

        InetAddress serverAddress = InetAddress.getByName(serverHost);
        // serverAddress.getHostName();

        logger.debug("Found nameServerInet " + serverAddress);

        Socket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            s = NameServerClient.nsConnect(new InetSocketAddress(serverAddress, port), myAddress,
                    false, 5);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            logger.debug("PoolChecker: contacting nameserver");
            if (poolName == null) {
                out.writeByte(IBIS_CHECKALL);
            } else {
                out.writeByte(IBIS_CHECK);
                out.writeUTF(poolName);
            }
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            int opcode = in.readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("PoolChecker: nameserver reply, opcode "
                        + opcode);
            }
        } finally {
            NameServer.closeConnection(in, out, s);
        }
    }
}
