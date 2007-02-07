/* $Id:$ */

package ibis.impl;

import ibis.io.DataInputStream;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;

import java.io.IOException;

/**
 * This class represents the information about a particular sendport/receiveport
 * connection, on the receiver side.
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
     * The underlying data stream for this connection.
     * The serialization steam lies on top of this.
     */
    protected DataInputStream dataIn;

    /**
     * Constructs a new <code>ReceivePortConnectionInfo</code> with the
     * specified parameters.
     * @param origin identifies the sendport of this connection.
     * @param port the receiveport.
     * @param dataIn the inputstream on which a serialization input stream
     * can be built.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortConnectionInfo(SendPortIdentifier origin,
            ReceivePort port, DataInputStream dataIn) throws IOException {
        this.origin = origin;
        this.port = port;
        this.dataIn = dataIn;
        newStream();
        port.addInfo(origin, this);
    }

    /**
     * Returns the number of bytes read from the data stream.
     * @return the number of bytes.
     */
    protected long bytesRead() {
        return dataIn.bytesRead();
    }

    /**
     * This method must be called each time a connected sendport adds a new
     * connection. This new connection may either be to the current receiveport,
     * or to another one. In both cases, the serialization stream must be
     * recreated.
     * @exception IOException is thrown in case of trouble.
     */
    protected void newStream() throws IOException {
        if (in != null) {
            in.close();
        }
        in = SerializationBase.createSerializationInput(port.serialization,
                dataIn);
        message = new ReadMessage(in, this);
    }
}
