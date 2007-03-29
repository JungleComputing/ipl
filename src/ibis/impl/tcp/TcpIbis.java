/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.IbisIdentifier;
import ibis.impl.ReceivePort;
import ibis.impl.SendPortIdentifier;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.RegistryEventHandler;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class TcpIbis extends ibis.impl.Ibis
        implements Runnable, TcpProtocol {

    static final Logger logger
            = Logger.getLogger("ibis.impl.tcp.TcpIbis");

    private IbisSocketFactory factory;

    private IbisServerSocket systemServer;

    private IbisSocketAddress myAddress;

    private boolean quiting = false;

    private HashMap<ibis.ipl.IbisIdentifier, IbisSocketAddress> addresses
        = new HashMap<ibis.ipl.IbisIdentifier, IbisSocketAddress>();

    public TcpIbis(RegistryEventHandler r, CapabilitySet p, Properties tp) {

        super(r, p, tp, null);

        factory.setIdent(ident);

        ThreadPool.createNew(this, "TcpIbis");
    }

    protected byte[] getData() throws IOException {

        factory = new IbisSocketFactory(properties);

        systemServer = factory.createServerSocket(0, 50, true, null);
        myAddress = systemServer.getLocalSocketAddress();

        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis: address = " + myAddress);
        }

        return myAddress.toBytes();
    }

    protected ibis.impl.PortType newPortType(CapabilitySet p, Properties tp) {
        return new TcpPortType(this, p, tp);
    }

    public void left(ibis.ipl.IbisIdentifier id) {
        super.left(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }

    public void died(ibis.ipl.IbisIdentifier id) {
        super.died(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }

    IbisSocket connect(TcpSendPort sp, ibis.impl.ReceivePortIdentifier rip,
            int timeout) throws IOException {
        IbisIdentifier id = (IbisIdentifier) rip.ibis();
        String name = rip.name();
        IbisSocketAddress idAddr;

        synchronized(addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                idAddr = new IbisSocketAddress(id.getImplementationData());
                addresses.put(id, idAddr);
            }
        }

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("--> Creating socket for connection to " + name
                    + " at " + idAddr);
        }

        do {
            DataOutputStream out = null;
            IbisSocket s = null;
            int result = -1;

            try {
                s = factory.createClientSocket(idAddr, timeout,
                        sp.dynamicProperties());
                out = new DataOutputStream(new BufferedArrayOutputStream(
                            s.getOutputStream()));

                out.writeUTF(name);
                sp.getIdent().writeTo(out);
                sp.getType().capabilities().writeTo(out);
                out.flush();

                result = s.getInputStream().read();

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
            factory.createClientSocket(myAddress, 0, null);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(IbisSocket s) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis got connection request from "
                    + s.getAddress() + ":" + s.getPort() + " on local port "
                    + s.getLocalPort());
        }

        BufferedArrayInputStream bais
                = new BufferedArrayInputStream(s.getInputStream());

        DataInputStream in = new DataInputStream(bais);
        OutputStream out = s.getOutputStream();

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
            rp.connect(send, s, bais);
            if (logger.isDebugEnabled()) {
                logger.debug("--> S connect done ");
            }
        } else {
            out.close();
            in.close();
            s.close();
        }
    }

    public void run() {
        // This thread handles incoming connection request from the
        // connect(TcpSendPort) call.
        while (true) {
            IbisSocket s = null;

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

    public void printStatistics() {
        factory.printStatistics(ident.toString());
    }

    private void cleanup() {
        try {
            systemServer.close();
        } catch (Throwable e) {
            // Ignore
        }
    }

    public void end() {
        super.end();
        printStatistics();
    }
}
