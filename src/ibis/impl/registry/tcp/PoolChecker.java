/* $Id: PoolChecker.java 3799 2006-06-01 16:14:44Z ceriel $ */

package ibis.impl.registry.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.registry.NSProps;
import ibis.util.IPUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PoolChecker implements Protocol {

    private static IbisSocketFactory socketFactory 
        = IbisSocketFactory.getFactory();

    private static Logger logger
            = ibis.util.GetLogger.getLogger(PoolChecker.class.getName());

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

    public static void main(String args[]) throws IOException {
        String serverHost = null;
        String poolName = null;
        String portString = null;
        int sleep = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-pool")) {
                poolName = args[++i];
            } else if (args[i].equals("-host") || args[i].equals("-ns")) {
                serverHost = args[++i];
            } else if (args[i].equals("-port")) {
                portString = args[++i];
            } else if (args[i].equals("-sleep")) {
                sleep = Integer.parseInt(args[++i]);
            }
        }

        check(poolName, serverHost, portString, sleep);
    }

    private static void check(String poolName, String serverHost,
            String portString, int sleep) throws IOException {
        Properties p = System.getProperties();

        if (serverHost == null) {
            serverHost = p.getProperty(NSProps.s_host);
        }
        if (serverHost == null) {
            logger.fatal("PoolChecker: no nameserver host specified");
            System.exit(1);
        }

        if (poolName == null) {
            poolName = p.getProperty(NSProps.s_pool);
        }

        if (portString == null) {
            portString = p.getProperty(NSProps.s_port);
        }

        int port = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;

        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
                logger.debug("Using nameserver port: " + port);
            } catch (Exception e) {
                logger.error("illegal nameserver port: "
                        + portString + ", using default");
            }
        }
        check(poolName, serverHost, port, sleep);
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
            s = socketFactory.createClientSocket(serverAddress, port, 
                    myAddress, 0, -1, null);

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
