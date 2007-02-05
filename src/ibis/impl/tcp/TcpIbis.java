/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.IbisSocketFactory;
import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePort;
import ibis.impl.SendPortIdentifier;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ResizeHandler;
import ibis.ipl.StaticProperties;
import ibis.util.GetLogger;
import ibis.util.ThreadPool;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.log4j.Logger;

public final class TcpIbis extends ibis.impl.Ibis
        implements Runnable, TcpProtocol {

    private static final Logger logger
            = GetLogger.getLogger("ibis.impl.tcp.TcpIbis");

    private ServerSocket systemServer;

    private InetSocketAddress myAddress;

    private boolean quiting = false;

    private IbisSocketFactory socketFactory;

    private HashMap<IbisIdentifier, InetSocketAddress> addresses
        = new HashMap<IbisIdentifier, InetSocketAddress>();

    public TcpIbis(ResizeHandler r, StaticProperties p1, StaticProperties p2)
        throws IOException {

        super(r, p1, p2);

        ThreadPool.createNew(this, "TcpIbis");
    }

    protected byte[] getData() throws IOException {
        InetAddress addr = IPUtils.getLocalHostAddress();
        if (addr == null) {
            logger.fatal("ERROR: could not get my own IP address, exiting.");
            System.exit(1);
        }

        socketFactory = IbisSocketFactory.getFactory();

        systemServer = socketFactory.createServerSocket(0, addr, true, null);

        myAddress = new InetSocketAddress(addr, systemServer.getLocalPort());

        logger.debug("--> TcpIbis: address = " + myAddress);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF(myAddress.getAddress().getHostAddress());
        out.writeInt(myAddress.getPort());
        out.close();

        return bos.toByteArray();
    }

    protected ibis.impl.PortType newPortType(StaticProperties p)
            throws PortMismatchException {
        return new TcpPortType(this, p);
    }

    Socket connect(TcpSendPort sp, ibis.impl.ReceivePortIdentifier rip,
            int timeout) throws IOException {
        IbisIdentifier id = (IbisIdentifier) rip.ibis();
        String name = rip.name();
        InetSocketAddress idAddr;

        synchronized(addresses) {
            idAddr = (InetSocketAddress) addresses.get(id);
            if (idAddr == null) {
                DataInputStream in = new DataInputStream(
                        new java.io.ByteArrayInputStream(id.getData()));
                String addr = in.readUTF();
                try {
                    idAddr = new InetSocketAddress(InetAddress.getByName(addr),
                            in.readInt());
                } catch(Exception e) {
                    throw new IOException("Could not get address from " + id);
                }
                in.close();
                addresses.put(id, idAddr);
            }
        }

        int port = idAddr.getPort();

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("--> Creating socket for connection to " + name
                    + " at " + id + ", port = " + port);
        }

        do {
            DataOutputStream data_out = null;
            DataInputStream data_in = null;
            Socket s = null;

            try {
                s = socketFactory.createClientSocket(idAddr.getAddress(), port,
                        myAddress.getAddress(), 0, timeout, sp.properties());

                data_out = new DataOutputStream(new BufferedOutputStream(
                            s.getOutputStream()));
                data_in = new DataInputStream(new BufferedInputStream(
                            s.getInputStream()));

                byte[] spIdent = sp.getIdent().getBytes();

                data_out.writeUTF(name);
                data_out.writeInt(spIdent.length);
                data_out.write(spIdent, 0, spIdent.length);
                data_out.flush();

                int result = data_in.readByte();

                switch(result) {
                case ReceivePort.ACCEPTED:
                    return socketFactory.createBrokeredSocket(data_in, data_out,
                            false, sp.properties());
                case ReceivePort.ALREADY_CONNECTED:
                    throw new AlreadyConnectedException(
                            "The sender was already connected to " + name
                            + " at " + id);
                case ReceivePort.TYPE_MISMATCH:
                    throw new PortMismatchException(
                            "Cannot connect ports of different PortTypes");
                case ReceivePort.DENIED:
                    throw new ConnectionRefusedException(
                            "Receiver denied connection");
                case ReceivePort.NO_MANYTOONE:
                    throw new ConnectionRefusedException(
                            "Receiver already has a connection and ManyToOne "
                            + "is not set");
                case ReceivePort.NOT_PRESENT:
                case ReceivePort.DISABLED:
                    // and try again if we did not reach the timeout...
                    if (timeout > 0 && System.currentTimeMillis()
                            > startTime + timeout) {
                        throw new ConnectionTimedOutException(
                                "Could not connect");
                    }
                    break;
                default:
                    throw new Error("Illegal opcode in TcpIbis.connect");
                }
            } finally {
                try {
                    data_in.close();
                } catch(Throwable e) {
                    // ignored
                }
                try {
                    data_out.close();
                } catch(Throwable e) {
                    // ignored
                }
                try {
                    s.close();
                } catch(Throwable e) {
                    // ignored
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        } while (true);
    }

    protected void quit() {
        try {
            quiting = true;
            // Connect so that the TcpIbis thread wakes up.
            InetAddress addr = myAddress.getAddress();
            int port = myAddress.getPort();
            socketFactory.createClientSocket(addr, port, addr, 0, 0, null);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(Socket s) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis got connection request from "
                    + s.getInetAddress() + ":" + s.getPort() + " on local port "
                    + s.getLocalPort());
        }

        DataInputStream data_in = new DataInputStream(
                new BufferedInputStream(s.getInputStream()));
        DataOutputStream data_out = new DataOutputStream(
                new BufferedOutputStream(s.getOutputStream()));

        String name = data_in.readUTF();
        int spLen = data_in.readInt();
        byte[] sp = new byte[spLen];
        data_in.readFully(sp, 0, sp.length);
        SendPortIdentifier send = new SendPortIdentifier(sp);

        // First, lookup receiveport.
        TcpReceivePort rp = (TcpReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
        } else {
            result = rp.connectionAllowed(send);
        }

        logger.debug("--> S RP = " + name + ": "
                + ReceivePort.getString(result));

        data_out.writeByte(result);
        if (result == ReceivePort.ACCEPTED) {
            data_out.flush();
            Socket s1 = socketFactory.createBrokeredSocket(data_in, data_out,
                    true, rp.properties());
            // add the connection to the receiveport.
            rp.connect(send, s1);
            logger.debug("--> S connect done ");
        }

        data_out.close();
        data_in.close();
    }

    public void run() {
        // This thread handles incoming connection request from the
        // connect(TcpSendPort) call.
        logger.debug("--> TcpIbis running");
        while (true) {
            Socket s = null;

            logger.debug("--> TcpIbis doing new accept()");
            try {
                s = systemServer.accept();
            } catch (Throwable e) {
                /* if the accept itself fails, we have a fatal problem. */
                logger.fatal("TcpIbis:run: got fatal exception in accept! ", e);
                cleanup();
                throw new Error("Fatal: TcpIbis could not do an accept", e);
            }

            logger.debug("--> TcpIbis through new accept()");
            try {
                if (quiting) {
                    logger.debug("--> it is a quit: RETURN");
                    cleanup();
                    return;
                }
                handleConnectionRequest(s);
            } catch (Throwable e) {
                logger.error("EEK: TcpIbis:run: got exception "
                        + "(closing this socket only: ", e);
            } finally {
                try {
                    s.close();
                } catch (Throwable e1) {
                    // ignored
                }
            }
        }
    }

    private void cleanup() {
        try {
            systemServer.close();
        } catch (Throwable e) {
            // Ignore
        }
    }
}
