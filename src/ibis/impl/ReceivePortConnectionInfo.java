/* $Id:$ */

package ibis.impl;

import ibis.io.DataInputStream;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;

import java.io.IOException;

/**
 * This class represents the information about a particular sendport/receiveport connection,
 * on the receiver side.
 */
public class ReceivePortConnectionInfo {

    /** Identifies the sendport side of the connection. */
    protected final SendPortIdentifier origin;

    /** The serialization input stream of the connection. */
    protected SerializationInput in;

    /** The receiveport of the connection. */
    protected ReceivePort port;

    /** The message that arrives on this connection. */
    protected ReadMessage message;

    /**
     * The underlying data stream for this connection. The serialization steam lies on
     * top of this.
     */
    protected DataInputStream dataIn;

    public ReceivePortConnectionInfo(SendPortIdentifier origin,
            ReceivePort port, DataInputStream dataIn) throws IOException {
        this.origin = origin;
        this.port = port;
        this.dataIn = dataIn;
        newStream();
        port.addInfo(origin, this);
    }

    public ReceivePortConnectionInfo(ReceivePortConnectionInfo orig) {
        origin = orig.origin;
        port = orig.port;
        dataIn = orig.dataIn;
        in = orig.in;
        message = orig.message;
        message.setFinished(false);
        message.setInfo(this);
    }

    protected long bytesRead() {
        return dataIn.bytesRead();
    }

    /**
     * This method must be called each time a connected sendport adds a new connection.
     * This new connection may either be to the current receiveport, or to another one.
     * In both cases, the serialization stream must be recreated.
     */
    protected void newStream() throws IOException {
        if (in != null) {
            in.close();
        }
        in = SerializationBase.createSerializationInput(port.serialization, dataIn);
        message = new ReadMessage(in, this, port);
    }

    public final SendPortIdentifier origin() {
        return origin;
    }

    public final ReadMessage message() {
        return message;
    }
}
