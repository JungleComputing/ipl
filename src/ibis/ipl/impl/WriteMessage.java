/* $Id$ */

package ibis.ipl.impl;

import ibis.io.SerializationOutput;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Implementation of the {@link ibis.ipl.WriteMessage} interface.
 * This is a complete implementation, but may be extended by an implementation.
 * In that case, the {@link SendPort#createWriteMessage()} method
 * must also be redefined.
 */
public class WriteMessage implements ibis.ipl.WriteMessage {

    protected SerializationOutput out;

    protected SendPort port;

    protected boolean isFinished = false;

    protected long before;

    protected WriteMessage(SendPort port) {
        this.port = port;
    }

    protected void initMessage(SerializationOutput out) {
        this.out = out;
        this.isFinished = false;
        this.before = port.bytesWritten();
    }

    private final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    public ibis.ipl.SendPort localPort() {
        return port;
    }

    public int send() throws IOException {
        checkNotFinished();
        return 0;
    }

    public void reset() throws IOException {
        try {
            out.reset();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void sync(int ticket) throws IOException {
        checkNotFinished();
        try {
            out.flush();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }
    
    public void flush() throws IOException {
        checkNotFinished();
        try {
            out.flush();
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        try {
            out.writeBoolean(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        try {
            out.writeByte(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeChar(char value) throws IOException {
        checkNotFinished();
        try {
            out.writeChar(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeShort(short value) throws IOException {
        checkNotFinished();
        try {
            out.writeShort(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeInt(int value) throws IOException {
        checkNotFinished();
        try {
            out.writeInt(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeLong(long value) throws IOException {
        checkNotFinished();
        try {
            out.writeLong(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        try {
            out.writeFloat(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        try {
            out.writeDouble(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeString(String value) throws IOException {
        checkNotFinished();
        try {
            out.writeString(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        try {
            out.writeObject(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }

    public long bytesWritten() {
        return port.bytesWritten() - before;
    }

    public int capacity() throws IOException {
        return -1;
    }

    public int remaining() throws IOException {
        return -1;
    }
    
    public long finish() throws IOException {
        checkNotFinished();
        try {
            out.reset();
        } catch(IOException e) {
            port.gotSendException(this, e);
        }
        try {
            out.flush();
        } catch(IOException e) {
            port.gotSendException(this, e);
        }
        isFinished = true;
        long retval = bytesWritten();
        port.finishMessage(this, retval);
        return retval;
    }

    public void finish(IOException e) {
        if (isFinished) {
            return;
        }

        try {
            out.reset();
        } catch (Throwable e2) {
            // ignored
        }

        try {
            out.flush();
        } catch (Throwable e2) {
            // ignored
        }

        isFinished = true;
        port.finishMessage(this, e);
    }

    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        try {
            out.writeByteBuffer(value);
        } catch (IOException e) {
            port.gotSendException(this, e);
        }
    }


}
