/* $Id: NioIbis.java 5175 2007-03-07 13:06:34Z ndrost $ */

package ibis.ipl.impl.nio;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.IbisIdentifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class NioIbis extends ibis.ipl.impl.Ibis {

    static final String prefix = "ibis.ipl.impl.nio.";

    static final String s_spi = prefix + "spi";

    static final String s_rpi = prefix + "rpi";

    static final String[] props = { s_spi, s_rpi };

    static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.WORLDMODEL_OPEN,
            IbisCapabilities.WORLDMODEL_CLOSED,
            IbisCapabilities.REGISTRY_DOWNCALLS,
            IbisCapabilities.REGISTRY_UPCALLS,
            "nickname.nio"
    );
    
    static final PortType portCapabilities = new PortType(
            PortType.SERIALIZATION_OBJECT,
            PortType.SERIALIZATION_DATA,
            PortType.SERIALIZATION_BYTE,
            PortType.SERIALIZATION_REPLACER + "=*",
            PortType.COMMUNICATION_FIFO,
            PortType.COMMUNICATION_NUMBERED,
            PortType.COMMUNICATION_RELIABLE,
            PortType.CONNECTION_DOWNCALLS,
            PortType.CONNECTION_UPCALLS,
            PortType.CONNECTION_TIMEOUT,
            PortType.CONNECTION_MANY_TO_MANY,
            PortType.CONNECTION_MANY_TO_ONE,
            PortType.CONNECTION_ONE_TO_MANY,
            PortType.CONNECTION_ONE_TO_ONE,
            PortType.RECEIVE_POLL,
            PortType.RECEIVE_AUTO_UPCALLS,
            PortType.RECEIVE_EXPLICIT,
            PortType.RECEIVE_POLL_UPCALLS,
            PortType.RECEIVE_TIMEOUT,
            "sendport.blocking",
            "sendport.nonblocking",
            "sendport.thread",
            "receiveport.blocking",
            "receivport.nonblocking",
            "receiveport.thread"
    );    
    
    private static final Logger logger
            = Logger.getLogger("ibis.ipl.impl.nio.NioIbis");

    ChannelFactory factory;

    private HashMap<ibis.ipl.IbisIdentifier, InetSocketAddress> addresses
        = new HashMap<ibis.ipl.IbisIdentifier, InetSocketAddress>();

    private SendReceiveThread sendReceiveThread = null;

    public NioIbis(RegistryEventHandler r, IbisCapabilities p, PortType[] types, Properties tp) {

        super(r, p, types, tp, null);
        properties.checkProperties(prefix, props, null, true);
    }

    protected PortType getPortCapabilities() {
        return portCapabilities;
    }
    
    protected IbisCapabilities getCapabilities() {
        return ibisCapabilities;
    }
    
    protected byte[] getData() throws IOException {

        factory = new TcpChannelFactory(this);

        InetSocketAddress myAddress = factory.getAddress();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(myAddress);
        out.close();

        return bos.toByteArray();
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

    protected void quit() {
        try {
            if (factory != null) {
                factory.quit();
            }

            if (sendReceiveThread != null) {
                factory.quit();
            }
        } catch(Throwable e) {
            // ignored
        }
        logger.info("NioIbis" + ident + " DE-initialized");
    }

    synchronized SendReceiveThread sendReceiveThread() throws IOException {
        if (sendReceiveThread == null) {
            sendReceiveThread = new SendReceiveThread();
        }
        return sendReceiveThread;
    }

    InetSocketAddress getAddress(IbisIdentifier id) throws IOException {
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
        return idAddr;
    }

    protected ibis.ipl.SendPort doCreateSendPort(PortType tp,
            String name, SendPortDisconnectUpcall cU) throws IOException {
        return new NioSendPort(this, tp, name, cU);
    }

    protected ibis.ipl.ReceivePort doCreateReceivePort(PortType tp,
            String name, MessageUpcall u, ReceivePortConnectUpcall cU)
            throws IOException {

        if (tp.hasCapability("receiveport.blocking")) {
            return new BlockingChannelNioReceivePort(this, tp, name, u, cU);
        }
        if (tp.hasCapability("receiveport.nonblocking")) {
            return new NonBlockingChannelNioReceivePort(this, tp, name, u, cU);
        }
        if (tp.hasCapability("receiveport.thread")) {
            return new ThreadNioReceivePort(this, tp, name, u, cU);
        }
        if (tp.hasCapability(PortType.CONNECTION_ONE_TO_ONE)
                || tp.hasCapability(PortType.CONNECTION_MANY_TO_ONE)) {
            return new BlockingChannelNioReceivePort(this, tp, name, u, cU);
        }
        return new NonBlockingChannelNioReceivePort(this, tp, name, u, cU);
    }
}
