/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.nio;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.GatheringByteChannel;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.Conversion;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;

public final class NioSendPort extends SendPort implements Protocol {

    private static Logger logger = LoggerFactory.getLogger(NioSendPort.class);

    private final NioAccumulator accumulator;

    NioSendPort(Ibis ibis, PortType type, String name, SendPortDisconnectUpcall cU, Properties props) throws IOException {
        super(ibis, type, name, cU, props);

        if (type.hasCapability("sendport.blocking")) {
            accumulator = new BlockingChannelNioAccumulator(this);
        } else if (type.hasCapability("sendport.nonblocking")) {
            accumulator = new NonBlockingChannelNioAccumulator(this);
        } else if (type.hasCapability("sendport.thread")) {
            accumulator = new ThreadNioAccumulator(this, ((NioIbis) ibis).sendReceiveThread());
        } else if (type.hasCapability(PortType.CONNECTION_ONE_TO_ONE) || type.hasCapability(PortType.CONNECTION_ONE_TO_MANY)) {
            accumulator = new BlockingChannelNioAccumulator(this);
        } else {
            accumulator = new NonBlockingChannelNioAccumulator(this);
        }

        initStream(accumulator);
    }

    @Override
    protected void handleSendException(WriteMessage w, IOException e) {
        logger.debug("handleSendException", e);
    }

    @Override
    protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws IOException {

        // FIXME: Retry on "receiveport not ready"
        // FIXME: implement fillTimeout in the lower layers!

        // make the connection. Will throw an Exception if if failed
        Channel channel = ((NioIbis) ibis).factory.connect(this, receiver, timeoutMillis);

        if (!(channel instanceof GatheringByteChannel)) {
            logger.error("factory returned wrong type of channel");
            throw new IOException("factory returned wrong type of channel");
        }

        // close output stream (if it exist). The new receiver needs the
        // stream headers and such.
        if (out != null) {
            logger.info("letting all the other" + " receivers know there's a new connection");
            out.writeByte(NEW_RECEIVER);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("done connecting " + ident + " to " + receiver);
        }

        SendPortConnectionInfo c = accumulator.add((GatheringByteChannel) channel, receiver);

        initStream(accumulator);
        return c;
    }

    @Override
    protected void sendDisconnectMessage(ReceivePortIdentifier receiver, SendPortConnectionInfo c) throws IOException {

        // tell out peer someone is going to have to disconnect
        out.writeByte(CLOSE_ONE_CONNECTION);

        accumulator.removeConnection(receiver);

        byte[] receiverBytes = receiver.toBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length, receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
    }

    @Override
    protected void announceNewMessage() throws IOException {
        out.writeByte(NEW_MESSAGE);

        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
    }

    @Override
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
