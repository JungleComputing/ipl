/* $Id: NioSendPort.java 4360 2006-09-29 12:00:16Z ceriel $ */

package ibis.impl.nio;


import ibis.impl.Ibis;
import ibis.impl.ReceivePortIdentifier;
import ibis.impl.SendPort;
import ibis.impl.SendPortConnectionInfo;
import ibis.impl.WriteMessage;
import ibis.io.Conversion;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;

import org.apache.log4j.Logger;

public final class NioSendPort extends SendPort implements Config, Protocol {

    private static Logger logger = Logger.getLogger(NioSendPort.class);

    private final NioAccumulator accumulator;

    NioSendPort(Ibis ibis, NioPortType type, String name,
            boolean connectionDowncalls, SendPortDisconnectUpcall cU)
            throws IOException {
        super(ibis, type, name, cU, connectionDowncalls);

        switch (type.sendPortImplementation) {
        case NioPortType.IMPLEMENTATION_BLOCKING:
            accumulator = new BlockingChannelNioAccumulator(this);
            break;
        case NioPortType.IMPLEMENTATION_NON_BLOCKING:
            accumulator = new NonBlockingChannelNioAccumulator(this);
            break;
        case NioPortType.IMPLEMENTATION_THREAD:
            accumulator = new ThreadNioAccumulator(this,
                    ((NioIbis)ibis).sendReceiveThread());
            break;
        default:
            throw new Error("unknown send port implementation type");
        }
        initStream(accumulator);
    }

    protected void handleSendException(WriteMessage w, IOException e) {
        // nothing.
    }

    protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
            long timeoutMillis) throws IOException {

        // FIXME: Retry on "receiveport not ready"

        // make the connection. Will throw an Exception if if failed
        Channel channel = ((NioIbis)ibis).factory.connect(this, receiver,
                timeoutMillis);

        if (ASSERT) {
            if (!(channel instanceof GatheringByteChannel)) {
                logger.error("factory returned wrong type of channel");
                throw new Error("factory returned wrong type of channel");
            }
        }

        // close output stream (if it exist). The new receiver needs the
        // stream headers and such.
        if (out != null) {
            logger.info("letting all the other"
                    + " receivers know there's a new connection");
            out.writeByte(NEW_RECEIVER);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("done connecting " + ident + " to " + receiver);
        }

        SendPortConnectionInfo c = accumulator.newConnection(
                (GatheringByteChannel) channel, receiver);

        initStream(accumulator);
        return c;
    }

    protected void disconnectPort(ReceivePortIdentifier receiver,
            SendPortConnectionInfo c) throws IOException {

        // tell out peer someone is going to have to disconnect
        out.writeByte(CLOSE_ONE_CONNECTION);

        accumulator.removeConnection(c);

        byte[] receiverBytes = receiver.toBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length,
            receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
    }

    protected void announceNewMessage() throws IOException {
        out.writeByte(NEW_MESSAGE);

        if (type.numbered) {
            out.writeLong(ibis.registry().getSeqno(name));
        }
    }

    protected void closePort() {
        try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
            out.close();
        } catch (Throwable e) {
            // IGNORE
        }
        out = null;
    }
}
