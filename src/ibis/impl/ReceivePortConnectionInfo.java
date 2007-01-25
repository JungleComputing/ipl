/* $Id:$ */

package ibis.impl;

import ibis.io.DataInputStream;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

/**
 * Abstract class for implementation-dependent connection info for
 * receiveports.
 */
public abstract class ReceivePortConnectionInfo implements Config {

    public final SendPortIdentifier origin;

    protected SerializationInput in;

    public ReceivePortConnectionInfo(SendPortIdentifier origin) {
        this.origin = origin;
    }

    protected abstract long bytesRead();

    protected void initStream(String serialization, DataInputStream dataIn)
            throws IOException {
        if (in != null) {
            in.close();
        }
        in = SerializationBase.createSerializationInput(serialization,
                dataIn);
    }
}
