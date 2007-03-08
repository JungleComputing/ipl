/* $Id: PoolChecker.java 3799 2006-06-01 16:14:44Z ceriel $ */

package ibis.impl.registry.smartsockets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

public class PoolChecker implements Protocol {

    private static Logger logger = Logger.getLogger(PoolChecker.class);

    private String poolName;
    private VirtualSocketAddress address;
    private int sleep;
    private VirtualSocketFactory socketFactory;

    public PoolChecker(VirtualSocketFactory factory, String poolName,
            VirtualSocketAddress address, int sleep) {
        this.socketFactory = factory;
        this.poolName = poolName;
        this.address = address;
        this.sleep = sleep;
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

    private void check(String poolName, VirtualSocketAddress address)
            throws IOException {

        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            s = socketFactory.createClientSocket(address, 0, null);

            out = new DataOutputStream(
                    new BufferedOutputStream(s.getOutputStream()));

            logger.debug("PoolChecker: contacting nameserver");

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
                logger.debug("PoolChecker: nameserver reply, opcode "
                        + opcode);
            }
        } finally {
            VirtualSocketFactory.close(s, out, in);
        }
    }
}
