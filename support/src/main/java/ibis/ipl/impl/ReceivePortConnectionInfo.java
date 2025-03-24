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

package ibis.ipl.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.DataInputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;

/**
 * This class represents the information about a particular sendport/receiveport
 * connection, on the receiver side.
 */
public class ReceivePortConnectionInfo {

    /** Debugging. */
    protected static final Logger logger = LoggerFactory.getLogger(ReceivePortConnectionInfo.class);

    /** Identifies the sendport side of the connection. */
    public final SendPortIdentifier origin;

    /** The serialization input stream of the connection. */
    public SerializationInput in;

    /** The receiveport of the connection. */
    public final ReceivePort port;

    /** The message that arrives on this connection. */
    public ReadMessage message;

    /**
     * The underlying data stream for this connection. The serialization stream lies
     * on top of this.
     */
    public DataInputStream dataIn;

    protected boolean closed = false;

    private long cnt = 0;

    /**
     * Constructs a new <code>ReceivePortConnectionInfo</code> with the specified
     * parameters.
     *
     * @param origin identifies the sendport of this connection.
     * @param port   the receiveport.
     * @param dataIn the inputstream on which a serialization input stream can be
     *               built.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortConnectionInfo(SendPortIdentifier origin, ReceivePort port, DataInputStream dataIn) throws IOException {
        this.origin = origin;
        this.port = port;
        this.dataIn = dataIn;
        // newStream();
        // Moved to subtypes. Calling it here may cause deadlocks!
        port.addInfo(origin, this);
    }

    /**
     * Returns the number of bytes read from the data stream.
     *
     * @return the number of bytes.
     */
    public long bytesRead() {
        long rd = dataIn.bytesRead();
        if (rd != 0) {
            dataIn.resetBytesRead();
            port.addDataIn(rd);
            cnt += rd;
        }
        return cnt;
    }

    /**
     * This method must be called each time a connected sendport adds a new
     * connection. This new connection may either be to the current receiveport, or
     * to another one. In both cases, the serialization stream must be recreated.
     *
     * @exception IOException is thrown in case of trouble.
     */
    public void newStream() throws IOException {
        bytesRead();
        if (in != null) {
            in.close();
        }
        in = SerializationFactory.createSerializationInput(port.serialization, dataIn, port.properties);
        message = port.createReadMessage(in, this);
    }

    /**
     * This method closes the connection, as the result of the specified exception.
     * Implementations may need to redefine this method
     *
     * @param e the exception.
     */
    public void close(Throwable e) {
        try {
            in.close();
        } catch (Throwable z) {
            // ignore
        }
        try {
            dataIn.close();
        } catch (Throwable z) {
            // ignore
        }
        closed = true;
        in = null;
        if (logger.isDebugEnabled()) {
            logger.debug(port.name + ": connection with " + origin + " closing", e);
        }
        port.lostConnection(origin, e);
    }

    /**
     * This method gets called when the upcall for the message explicitly called
     * {@link ReadMessage#finish()}. The default implementation just allocates a new
     * message.
     */
    protected void upcallCalledFinish() {
        message = port.createReadMessage(in, this);
        if (logger.isDebugEnabled()) {
            logger.debug(port.name + ": new connection handler for " + origin + ", finish called from upcall");
        }
    }

    public String connectionType() {
        return "unknown";
    }
}
