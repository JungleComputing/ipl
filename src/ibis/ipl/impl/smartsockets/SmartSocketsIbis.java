/* $Id$ */

package ibis.ipl.impl.smartsockets;

import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisProperties;
import ibis.ipl.IbisStarter;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.support.Client;
import ibis.smartsockets.hub.servicelink.ServiceLink;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmartSocketsIbis extends ibis.ipl.impl.Ibis implements
        Runnable, SmartSocketsProtocol {

    private static final String PORT_PROPERTY = "ibis.local.port";
    
    private static final String KEEP_ALIVE_PROPERTY = "ibis.ipl.impl.smartsockets.keepalive";
    
    private static final String SOCKET_TIMEOUT_PROPERTY = "ibis.ipl.impl.smartsockets.sotimeout";

    static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.smartsockets.SmartSocketsIbis");

    private VirtualSocketFactory factory;

    private VirtualServerSocket systemServer;

    private VirtualSocketAddress myAddress;

    private boolean quiting = false;
    
    private boolean keepAlive = false;
    
    private int soTimeout = -1;

    private HashMap<ibis.ipl.IbisIdentifier, VirtualSocketAddress> addresses = new HashMap<ibis.ipl.IbisIdentifier, VirtualSocketAddress>();

    private final HashMap<String, Object> lightConnection = new HashMap<String, Object>();

    private final HashMap<String, Object> directConnection = new HashMap<String, Object>();

    public SmartSocketsIbis(RegistryEventHandler registryEventHandler,
            IbisCapabilities capabilities, Credentials credentials,
            byte[] applicationTag, PortType[] types, Properties userProperties,
            IbisStarter starter) throws IbisCreationFailedException {
        super(registryEventHandler, capabilities, credentials, applicationTag,
                types, userProperties, starter);

        lightConnection.put("connect.module.allow", "ConnectModule(HubRouted)");

        // directConnection.put("connect.module.skip",
        // "ConnectModule(HubRouted)");

        // NOTE: this is too restrictive, since reverse connection setup also
        // result in a direct connection...
        directConnection.put("connect.module.allow", "ConnectModule(Direct)");

        this.properties.checkProperties("ibis.ipl.impl.smartsockets.",
                new String[] { KEEP_ALIVE_PROPERTY, SOCKET_TIMEOUT_PROPERTY }, null, true);

        keepAlive = this.properties.getBooleanProperty(KEEP_ALIVE_PROPERTY, false);
        soTimeout = this.properties.getIntProperty(SOCKET_TIMEOUT_PROPERTY, -1);
        Runnable r = new Runnable() {
            public void run() {
                try {
                    ServiceLink sl = factory.getServiceLink();
                    if (sl != null) {
                        String colorString = "";
                        if (properties.getProperty(IbisProperties.LOCATION_COLOR) != null) {
                            colorString = "^"
                                    + properties
                                    .getProperty(IbisProperties.LOCATION_COLOR);
                        }

                        sl.registerProperty("smartsockets.viz", "I^" + ident.name() + "^" + ident.name()
                                + "," + ident.location().toString() + colorString);

                    }
                } catch (Throwable e) {
                    // ignored
                }
            }
        };
        
        ThreadPool.createNew(r, "Color setter");

        // Create a new accept thread
        ThreadPool.createNew(this, "SmartSocketsIbis Accept Thread");
    }

    protected byte[] getData() throws IOException {
        String clientID = this.properties.getProperty(ID_PROPERTY);

        int port = this.properties.getIntProperty(PORT_PROPERTY, 0);
        
        Client client = Client.getOrCreateClient(clientID, properties, port);
        factory = client.getFactory();

        // systemServer = factory.createServerSocket(port, 50, true, null);
        systemServer = factory.createServerSocket(0, 50, true, null);
        myAddress = systemServer.getLocalSocketAddress();

        if (logger.isInfoEnabled()) {
            logger.info("--> SmartSocketIbis: address = " + myAddress);
        }

        return myAddress.toBytes();
    }

    /*
     * // NOTE: this is wrong ? Even though the ibis has left, the
     * IbisIdentifier may still be floating around in the system... We should
     * just have some timeout on the cache entries instead...
     * 
     * public void left(ibis.ipl.IbisIdentifier id) { super.left(id);
     * synchronized(addresses) { addresses.remove(id); } }
     * 
     * public void died(ibis.ipl.IbisIdentifier id) { super.died(id);
     * synchronized(addresses) { addresses.remove(id); } }
     */

    ServiceLink getServiceLink() {
        return factory.getServiceLink();
    }

    VirtualSocket connect(SmartSocketsSendPort sp,
            ibis.ipl.impl.ReceivePortIdentifier rip, int timeout,
            boolean fillTimeout) throws IOException {

        IbisIdentifier id = (IbisIdentifier) rip.ibisIdentifier();
        String name = rip.name();
        VirtualSocketAddress idAddr;

        synchronized (addresses) {
            idAddr = addresses.get(id);
            if (idAddr == null) {
                idAddr = VirtualSocketAddress.fromBytes(id
                        .getImplementationData(), 0);
                addresses.put(id, idAddr);
            }
        }

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("--> Creating socket for connection to " + name
                    + " at " + idAddr);
        }

        PortType sendPortType = sp.getPortType();

        do {
            DataOutputStream out = null;
            VirtualSocket s = null;
            int result = -1;

            try {
                HashMap<String, Object> h = null;

                if (sp.getPortType().hasCapability(PortType.CONNECTION_LIGHT)) {
                    h = lightConnection;
                } else if (sp.getPortType().hasCapability(
                        PortType.CONNECTION_DIRECT)) {
                    h = directConnection;
                }

                /*
                 * Map<String, String> properties = sp.managementProperties();
                 * 
                 * if (properties != null) {
                 * 
                 * if (h == null) { h = new HashMap<String, Object>(); }
                 * 
                 * h.putAll(properties); }
                 * 
                 * if (logger.isDebugEnabled()) {
                 * logger.debug("Creating connection with properties " + h); }
                 */

                s = factory.createClientSocket(idAddr, timeout, fillTimeout, h);

                s.setTcpNoDelay(true);
                boolean keepAlive = this.keepAlive;
                int soTimeout = this.soTimeout;
                Properties props = sp.getProperties();
                if (props != null) {
                    TypedProperties p = new TypedProperties(props);
                    keepAlive = p.getBooleanProperty(KEEP_ALIVE_PROPERTY, keepAlive);
                    soTimeout = p.getIntProperty(SOCKET_TIMEOUT_PROPERTY, soTimeout);
                }
                if (keepAlive) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Setting KeepAlive");
                        }
                	s.setKeepAlive(true);
                    } catch(Throwable e) {
                	logger.info("Could not set KeepAlive", e);
                    }
                }
                if (soTimeout > 0) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Setting soTimeout");
                        }
                        s.setSoTimeout(soTimeout);
                    } catch(Throwable e) {
                        logger.info("Could not set SoTimeout", e);
                    }
                }

                out = new DataOutputStream(
                        new BufferedArrayOutputStream(s.getOutputStream()));

                out.writeUTF(name);
                sp.getIdent().writeTo(out);
                sendPortType.writeTo(out);
                out.flush();

                result = s.getInputStream().read();

                switch (result) {
                case ReceivePort.ACCEPTED:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection accepted");
                    }
                    return s;
                case ReceivePort.ALREADY_CONNECTED:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection refused: already connected");
                    }
                    throw new AlreadyConnectedException("Already connected",
                            rip);
                case ReceivePort.TYPE_MISMATCH:
                    // Read receiveport type from input, to produce a
                    // better error message.
                    DataInputStream in = new DataInputStream(s.getInputStream());
                    PortType rtp = new PortType(in);
                    CapabilitySet s1 = rtp.unmatchedCapabilities(sendPortType);
                    CapabilitySet s2 = sendPortType.unmatchedCapabilities(rtp);
                    String message = "";
                    if (s1.size() != 0) {
                        message = message
                                + "\nUnmatched receiveport capabilities: "
                                + s1.toString() + ".";
                    }
                    if (s2.size() != 0) {
                        message = message
                                + "\nUnmatched sendport capabilities: "
                                + s2.toString() + ".";
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection refused: " + message);
                    }
                    throw new PortMismatchException(
                            "Cannot connect ports of different port types."
                                    + message, rip);
                case ReceivePort.DENIED:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection denied");
                    }
                    throw new ConnectionRefusedException(
                            "Receiver denied connection", rip);
                case ReceivePort.NO_MANY_TO_X:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection refused: already connected and no ManyToXXX");
                    }
                    throw new ConnectionRefusedException(
                            "Receiver already has a connection and neither ManyToOne not ManyToMany "
                                    + "is set", rip);
                case ReceivePort.NOT_PRESENT:
                case ReceivePort.DISABLED:
                    // and try again if we did not reach the timeout...
                    if (timeout > 0
                            && System.currentTimeMillis() > startTime + timeout) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Connection timed out");
                        }
                        throw new ConnectionTimedOutException(
                                "Could not connect", rip);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection retrying ...");
                    }
                    break;
                case -1:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection got EOF");
                    }
                    throw new IOException("Encountered EOF in SmartSocketsIbis.connect");
                default:
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection got illegal opcode");
                    }
                    throw new IOException("Illegal opcode in SmartSocketsIbis.connect");
                }
            } catch (SocketTimeoutException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SocketTimeoutException", e);
                }
                throw new ConnectionTimedOutException("Could not connect", rip);
            } finally {
                if (result != ReceivePort.ACCEPTED) {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Throwable e) {
                        // ignored
                    }
                    try {
                        s.close();
                    } catch (Throwable e) {
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
            // Connect so that the SmartSocketsIbis thread wakes up.
            factory.createClientSocket(myAddress, 0, false, null);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private void handleConnectionRequest(VirtualSocket s) throws IOException {

        boolean keepAlive = this.keepAlive;
        int soTimeout = this.soTimeout;
        
        if (logger.isDebugEnabled()) {
            logger.debug("--> SmartSocketsIbis got connection request from " + s);
        }
 
        BufferedArrayInputStream bais = 
            new BufferedArrayInputStream(s.getInputStream());

        DataInputStream in = new DataInputStream(bais);
        OutputStream out = s.getOutputStream();

        String name = in.readUTF();
        SendPortIdentifier send = new SendPortIdentifier(in);
        PortType sp = new PortType(in);

        // First, lookup receiveport.
        SmartSocketsReceivePort rp = (SmartSocketsReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
            if (logger.isDebugEnabled()) {
                logger.debug("ReceivePort " + name + " not present");
            }
            out.write(result);
            out.close();
            in.close();
            s.close();
            return;
            
        }

        synchronized(rp) {
            result = rp.connectionAllowed(send, sp);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("--> S RP = " + name + ": "
                    + ReceivePort.getString(result));
        }

        try {
            out.write(result);
            if (result == ReceivePort.TYPE_MISMATCH) {
                DataOutputStream dout = new DataOutputStream(out);
                rp.getPortType().writeTo(dout);
                dout.flush();
            }
            out.flush();
        } catch(IOException e) {
            // If we cannot write the result back, we must notify the system that
            // the connection is to be removed.
            if (logger.isDebugEnabled()) {
                logger.debug("Got exception while writing connection request result", e);
            }
            if (result == ReceivePort.ACCEPTED) {
                rp.cancelConnection(send);
            }
            throw e;
        }
        if (result == ReceivePort.ACCEPTED) {
            // add the connection to the receiveport.
            Properties props = rp.getProperties();
            if (props != null) {
                TypedProperties p = new TypedProperties(props);
                soTimeout = p.getIntProperty(SOCKET_TIMEOUT_PROPERTY, soTimeout);
                keepAlive = p.getBooleanProperty(KEEP_ALIVE_PROPERTY, keepAlive);
            }

            if (soTimeout > 0) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting soTimeout");
                    }
                    s.setSoTimeout(soTimeout);
                } catch(Throwable e) {
                    logger.info("Could not set SoTimeout", e);
                }
            }
            if (keepAlive) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting KeepAlive");
                    }
                    s.setKeepAlive(true);
                } catch(Throwable e) {
                    logger.info("Could not set KeepAlive", e);
                }
            }
            
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
        // connect(SmartSocketsSendPort) call.

        boolean stop = false;

        while (!stop) {
            VirtualSocket s = null;

            if (logger.isDebugEnabled()) {
                logger.debug("--> SmartSocketsIbis doing new accept()");
            }

            try {
                s = systemServer.accept();
                s.setTcpNoDelay(true);
            } catch (Throwable e) {
                /* if the accept itself fails, we have a fatal problem. */
                logger.error("SmartSocketsIbis:run: got fatal exception in accept! ", e);
                cleanup();
                throw new Error("Fatal: SmartSocketsIbis could not do an accept", e);
                // This error is thrown in the SmartSocketsIbis thread, not in a user
                // thread. It kills the thread.
            }

            if (logger.isDebugEnabled()) {
                logger.debug("--> SmartSocketsIbis through new accept()");
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

                // This thread will now live on as a connection handler. Start
                // a new accept thread here, and make sure that this thread does
                // not do an accept again, if it ever returns to this loop.
                stop = true;

                try {
                    Thread.currentThread().setName("Connection Handler");
                } catch (Exception e) {
                    // ignore
                }

                ThreadPool.createNew(this, "SmartSocketsIbis Accept Thread");
                handleConnectionRequest(s);
            } catch (Throwable e) {
                try {
                    s.close();
                } catch (Throwable e2) {
                    // ignored
                }
                logger.error("EEK: SmartSocketsIbis:run: got exception "
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

    protected ibis.ipl.SendPort doCreateSendPort(PortType tp, String nm,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {

        if (tp.hasCapability(PortType.CONNECTION_ULTRALIGHT)) {
            return new SmartSocketsUltraLightSendPort(this, tp, nm, props);
        }

        if (tp.hasCapability(PortType.CONNECTION_LIGHT)) {

            if (props == null) {
                props = new Properties();
            }

            props.put("connect.module.type.skip", "direct");

        } else if (tp.hasCapability(PortType.CONNECTION_DIRECT)) {

            if (props == null) {
                props = new Properties();
            }

            props.put("connect.module.skip", "ConnectModule(HubRouted)");
        }

        return new SmartSocketsSendPort(this, tp, nm, cU, props);
    }

    protected ibis.ipl.ReceivePort doCreateReceivePort(PortType tp, String nm,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {

        if (tp.hasCapability(PortType.CONNECTION_ULTRALIGHT)) {
            return new SmartSocketsUltraLightReceivePort(this, tp, nm, u, props);
        }

        if (tp.hasCapability(PortType.CONNECTION_LIGHT)) {

            if (props == null) {
                props = new Properties();
            }

            props.put("connect.module.type.skip", "direct");

        } else if (tp.hasCapability(PortType.CONNECTION_DIRECT)) {

            if (props == null) {
                props = new Properties();
            }

            props.put("connect.module.skip", "ConnectModule(HubRouted)");
        }

        return new SmartSocketsReceivePort(this, tp, nm, u, cU, props);
    }
}
