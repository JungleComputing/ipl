/* $Id: NioIbis.java 5175 2007-03-07 13:06:34Z ndrost $ */

package ibis.impl.nio;

import ibis.impl.IbisIdentifier;
import ibis.ipl.CapabilitySet;
import ibis.ipl.RegistryEventHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class NioIbis extends ibis.impl.Ibis {

    static final String prefix = "ibis.impl.nio.";

    static final String s_spi = prefix + "spi";

    static final String s_rpi = prefix + "rpi";

    static final String[] props = { s_spi, s_rpi };

    private static final Logger logger
            = Logger.getLogger("ibis.impl.nio.NioIbis");

    ChannelFactory factory;

    private HashMap<IbisIdentifier, InetSocketAddress> addresses
        = new HashMap<IbisIdentifier, InetSocketAddress>();

    private SendReceiveThread sendReceiveThread = null;

    public NioIbis(RegistryEventHandler r, CapabilitySet p, Properties tp) {

        super(r, p, tp, null);
        properties.checkProperties(prefix, props, null, true);
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

    protected ibis.impl.PortType newPortType(CapabilitySet p, Properties tp) {
        return new NioPortType(this, p, tp);
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
}
