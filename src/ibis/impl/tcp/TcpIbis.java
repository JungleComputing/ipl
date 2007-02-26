/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePort;
import ibis.impl.SendPortIdentifier;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ResizeHandler;
import ibis.util.IPUtils;
import ibis.util.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class TcpIbis extends ibis.impl.Ibis
        implements Runnable, TcpProtocol {

    private static final Logger logger
            = Logger.getLogger("ibis.impl.tcp.TcpIbis");

    private ServerSocket systemServer;

    private InetSocketAddress myAddress;

    private InetSocketAddress local;

    private boolean quiting = false;

    private HashMap<IbisIdentifier, InetSocketAddress> addresses
        = new HashMap<IbisIdentifier, InetSocketAddress>();

    public TcpIbis(ResizeHandler r, CapabilitySet p, Properties tp)
        throws Throwable {

        super(r, p, tp);

        ThreadPool.createNew(this, "TcpIbis");
    }

    protected byte[] getData() throws IOException {
        InetAddress addr = IPUtils.getLocalHostAddress();
        if (addr == null) {
            logger.fatal("ERROR: could not get my own IP address, exiting.");
            System.exit(1);
        }

        systemServer = new ServerSocket();
        local = new InetSocketAddress(addr, 0);
        systemServer.bind(local, 50);
        myAddress = new InetSocketAddress(addr, systemServer.getLocalPort());

        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis: address = " + myAddress);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(myAddress);
        out.close();

        return bos.toByteArray();
    }

    protected ibis.impl.PortType newPortType(CapabilitySet p, Properties tp) {
        return new TcpPortType(this, p, tp);
    }

    public void left(IbisIdentifier[] ids) {
        super.left(ids);
        synchronized(addresses) {
            for (int i = 0; i < ids.length; i++) {
                addresses.remove(ids[i]);
            }
        }
    }

    public void died(IbisIdentifier[] ids) {
        super.died(ids);
        synchronized(addresses) {
            for (int i = 0; i < ids.length; i++) {
                addresses.remove(ids[i]);
            }
        }
    }

    Socket connect(TcpSendPort sp, ibis.impl.ReceivePortIdentifier rip,
            int timeout) throws IOException {
        IbisIdentifier id = (IbisIdentifier) rip.ibis();
        String name = rip.name();
        InetSocketAddress idAddr;

        synchronized(addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                ObjectInputStream in = new ObjectInputStream(
                        new java.io.ByteArrayInputStream(
                                id.getImplementationData()));
                try {
                    idAddr = (InetSocketAddress) in.readObject();
                } catch(ClassNotFoundException e) {
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
            DataOutputStream out = null;
            InputStream in = null;
            Socket s = null;
            int result = -1;

            try {
                s = createClientSocket(local, idAddr, timeout);
                out = new DataOutputStream(new BufferedOutputStream(
                            s.getOutputStream()));
                in = new BufferedInputStream(s.getInputStream());

                out.writeUTF(name);
                sp.getIdent().writeTo(out);
                sp.getType().capabilities().writeTo(out);
                out.flush();

                result = in.read();

                switch(result) {
                case ReceivePort.ACCEPTED:
                    return s;
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
            } catch(SocketTimeoutException e) {
                throw new ConnectionTimedOutException("Could not connect");
            } finally {
                if (result != ReceivePort.ACCEPTED) {
                    try {
                        in.close();
                    } catch(Throwable e) {
                        // ignored
                    }
                    try {
                        out.close();
                    } catch(Throwable e) {
                        // ignored
                    }
                    try {
                        s.close();
                    } catch(Throwable e) {
                        // ignored
                    }
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
            createClientSocket(local, myAddress, 0);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(Socket s) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis got connection request from "
                    + s.getInetAddress() + ":" + s.getPort() + " on local port "
                    + s.getLocalPort());
        }

        DataInputStream in = new DataInputStream(
                new BufferedInputStream(s.getInputStream()));
        OutputStream out = new BufferedOutputStream(s.getOutputStream());

        String name = in.readUTF();
        SendPortIdentifier send = new SendPortIdentifier(in);
        CapabilitySet sp = new CapabilitySet(in);

        // First, lookup receiveport.
        TcpReceivePort rp = (TcpReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
        } else {
            result = rp.connectionAllowed(send, sp);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("--> S RP = " + name + ": "
                    + ReceivePort.getString(result));
        }

        out.write(result);
        out.flush();
        if (result == ReceivePort.ACCEPTED) {
            // add the connection to the receiveport.
            rp.connect(send, s);
            if (logger.isDebugEnabled()) {
                logger.debug("--> S connect done ");
            }
        } else {
            out.close();
            in.close();
            s.close();
        }
    }

    private Socket createClientSocket(InetSocketAddress local,
            InetSocketAddress remote, int timeout) throws IOException {
        Socket s = new Socket();
        s.bind(local);
        s.connect(remote, timeout);
        s.setTcpNoDelay(true);
        return s;
    }

    public void run() {
        // This thread handles incoming connection request from the
        // connect(TcpSendPort) call.
        while (true) {
            Socket s = null;

            if (logger.isDebugEnabled()) {
                logger.debug("--> TcpIbis doing new accept()");
            }
            try {
                s = systemServer.accept();
                s.setTcpNoDelay(true);
            } catch (Throwable e) {
                /* if the accept itself fails, we have a fatal problem. */
                logger.fatal("TcpIbis:run: got fatal exception in accept! ", e);
                cleanup();
                throw new Error("Fatal: TcpIbis could not do an accept", e);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("--> TcpIbis through new accept()");
            }
            try {
                if (quiting) {
                    s.close();
                    if (logger.isDebugEnabled()) {
                        logger.debug("--> it is a quit: RETURN");
                    }
                    cleanup();
                    return;
                }
                handleConnectionRequest(s);
            } catch (Throwable e) {
                try {
                    s.close();
                } catch(Throwable e2) {
                    // ignored
                }
                logger.error("EEK: TcpIbis:run: got exception "
                        + "(closing this socket only: ", e);
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
