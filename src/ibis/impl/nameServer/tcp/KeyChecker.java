/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.virtual.VirtualSocket;
import ibis.connect.virtual.VirtualSocketAddress;
import ibis.connect.virtual.VirtualSocketFactory;
import ibis.impl.nameServer.NSProps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class KeyChecker implements Protocol {

    private static VirtualSocketFactory socketFactory 
        = VirtualSocketFactory.getSocketFactory();

    private static Logger logger
            = ibis.util.GetLogger.getLogger(KeyChecker.class.getName());

    private String poolName;
    private VirtualSocketAddress address;
    private int sleep;

    public KeyChecker(String poolName, VirtualSocketAddress address, int sleep) {
        this.poolName = poolName;
        this.address = address;
        this.sleep = sleep;
    }

    public static void main(String args[]) throws IOException {
        String serverHost = null;
        String poolName = null;
        String portString = null;
        int sleep = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-key")) {
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
            logger.fatal("KeyChecker: no nameserver host specified");
            System.exit(1);
        }

        if (poolName == null) {
            poolName = p.getProperty(NSProps.s_key);
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
        // TODO
        // FIX!! check(poolName, serverHost, port, sleep);
    }

    static void check(String poolName, VirtualSocketAddress address, int sleep)
            throws IOException {
        new KeyChecker(poolName, address, sleep).run();
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
                check(poolName, address);
            } catch(Exception e) {
                // Ignored
            }
        } while (sleep != 0);
    }

    public static void check(String poolName, VirtualSocketAddress address)
            throws IOException {
        
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            s = socketFactory.createClientSocket(address, 0, null);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));
            
            logger.debug("KeyChecker: contacting nameserver");
            
            if (poolName == null) {
                out.writeByte(IBIS_CHECKALL);
            } else {
                out.writeByte(IBIS_CHECK);
                out.writeUTF(poolName);
            }
            out.flush();

            in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));

            int opcode = in.readByte();

            if (logger.isDebugEnabled()) {
                logger.debug("KeyChecker: nameserver reply, opcode "
                        + opcode);
            }
        } finally {
            NameServer.closeConnection(in, out, s);
        }
    }
}
