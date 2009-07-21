package ibis.ipl.impl.stacking.lrmc;

import ibis.io.SerializationOutput;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public class LrmcWriteMessage implements WriteMessage {

    boolean isFinished = false;
    Multicaster om;
    long count = 0;
    SerializationOutput out;
    LrmcSendPort port;
    IbisIdentifier[] destinations;

    public LrmcWriteMessage(LrmcSendPort port, Multicaster om,
            IbisIdentifier[] dests) throws IOException {
        this.om = om;
        this.out = om.sout;
        this.port = port;
        this.destinations = dests;
        om.initializeSend(dests);
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
        out.reset();
    }

    public void sync(int ticket) throws IOException {
        checkNotFinished();
        out.flush();
    }

    public void flush() throws IOException {
        checkNotFinished();
        out.flush();
    }

    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        out.writeBoolean(value);
    }

    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        out.writeByte(value);
    }

    public void writeChar(char value) throws IOException {
        checkNotFinished();
        out.writeChar(value);
    }

    public void writeShort(short value) throws IOException {
        checkNotFinished();
        out.writeShort(value);
    }

    public void writeInt(int value) throws IOException {
        checkNotFinished();
        out.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
        checkNotFinished();
        out.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        out.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        out.writeDouble(value);
    }

    public void writeString(String value) throws IOException {
        checkNotFinished();
        out.writeString(value);
    }

    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        out.writeObject(value);
    }

    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);        
        checkNotFinished();

    }

    public long bytesWritten() {
        long cnt = om.bout.bytesWritten();
        long retval = cnt - count;
        count = cnt;
        return retval;
    }

    public long finish() throws IOException {
        try {
            if (!isFinished) {
                long retval = om.finalizeSend();
                isFinished = true;
                return retval;
            }
            throw new IOException("Already finished");
        } finally {
            synchronized (port) {
                port.message = null;
                port.notifyAll();
            }
        }
    }

    public void finish(IOException exception) {
        try {
            if (!isFinished) {
                om.finalizeSend();
            }
        } catch (Throwable e) {
            // ignored
        } finally {
            synchronized (port) {
                isFinished = true;
                port.message = null;
                port.notifyAll();
            }
        }
        // TODO Auto-generated method stub

    }

    public int capacity() throws IOException {
        return om.bout.bufferSize();
    }

    public int remaining() throws IOException {
        return (int) (om.bout.bufferSize() - om.bout.bytesWritten());
    }

}
