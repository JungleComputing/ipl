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
import ibis.util.ThreadPool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import smartsockets.hub.servicelink.ServiceLink;
import smartsockets.virtual.InitializationException;
import smartsockets.virtual.VirtualServerSocket;
import smartsockets.virtual.VirtualSocket;
import smartsockets.virtual.VirtualSocketAddress;
import smartsockets.virtual.VirtualSocketFactory;

import org.apache.log4j.Logger;

public final class TcpIbis extends ibis.impl.Ibis
        implements Runnable, TcpProtocol {

    private static final Logger logger
            = Logger.getLogger("ibis.impl.tcp.TcpIbis");

    private VirtualSocketFactory socketFactory;

    private VirtualServerSocket systemServer;

    private VirtualSocketAddress myAddress;

    private boolean quiting = false;

    private HashMap<IbisIdentifier, VirtualSocketAddress> addresses
        = new HashMap<IbisIdentifier, VirtualSocketAddress>();

    public TcpIbis(ResizeHandler r, CapabilitySet p, Properties tp)
        throws Throwable {

        super(r, p, tp);
        // Bit of a hack to improve the visualization
        try { 
            ServiceLink sl = socketFactory.getServiceLink();
        
            if (sl != null) {
                sl.registerProperty("ibis", ident.toString());
            }
        } catch (Exception e) {
            logger.debug("Failed to register ibis property with " +
                        "registry (not very important...)");
        }
        ThreadPool.createNew(this, "TcpIbis");
    }

    protected byte[] getData() throws IOException {
        // NOTE: moved here from the static initializer, since we may want to 
        //       configure the thing differently for every TcpIbis instance in 
        //       this process. Having a single -static- socketfactory doesn't 
        //       work then....        
        
        try {
            socketFactory = VirtualSocketFactory.createSocketFactory(properties,
                true);
        } catch (InitializationException e1) {
            throw new IOException("Failed to create socket factory");
        }
        // TODO: fix for more than one Ibis instance in a jvm
        VirtualSocketFactory.registerSocketFactory("Factory for Ibis",
                socketFactory);

        // We don't pass properties, since this is not a socket that is used
        // for an ibis port.
        systemServer = socketFactory.createServerSocket(0, 0, true, null);

        myAddress = systemServer.getLocalSocketAddress();

        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis: address = " + myAddress);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF(myAddress.toString());
        out.flush();
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

    VirtualSocket connect(TcpSendPort sp, ibis.impl.ReceivePortIdentifier rip,
            int timeout) throws IOException {
        IbisIdentifier id = (IbisIdentifier) rip.ibis();
        String name = rip.name();
        VirtualSocketAddress idAddr;

        synchronized(addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                DataInputStream in = new DataInputStream(
                        new java.io.ByteArrayInputStream(
                                id.getImplementationData()));
                String addr = in.readUTF();
                in.close();
                try {
                    idAddr = new VirtualSocketAddress(addr);
                } catch(Exception e) {
                    throw new IOException("Could not get address from " + id);
                }
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
            InputStream in = null;
            VirtualSocket s = null;
            int result = -1;

            try {
                s = socketFactory.createClientSocket(idAddr, timeout,
                        sp.properties());
                s.setTcpNoDelay(true);
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
            socketFactory.createClientSocket(myAddress, 0, null);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(VirtualSocket s) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("--> TcpIbis got connection request from "
                    + s.getLocalSocketAddress() + ":" + s.getPort()
                    + " on local port " + s.getLocalPort());
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

    public void run() {
        // This thread handles incoming connection request from the
        // connect(TcpSendPort) call.
        while (true) {
            VirtualSocket s = null;

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
        socketFactory.printStatistics(ident.toString());
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
        socketFactory.printStatistics("Factory for Ibis");
    }
}
