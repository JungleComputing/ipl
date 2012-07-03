package ibis.ipl.impl.stacking.cache;

import ibis.io.DataInputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.*;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheReadMessage implements ReadMessage {

    /*
     * The port which will give me base ReadMessages.
     */
    final CacheReceivePort port;
    
    DataInputStream dis;
    SerializationInput in;

    protected boolean isFinished = false;

    public CacheReadMessage(CacheReceivePort port) throws IOException {
        this.port = port;
        
        PortType type = this.port.getPortType();
        String serialization;
        if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
            serialization = "sun";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
            serialization = "ibis";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }
        dis = new BufferedDataInputStream(port);
        in = SerializationFactory.createSerializationInput(serialization, dis);
    }

    @Override
    public ibis.ipl.ReceivePort localPort() {
        return this.port;
    }

    @Override
    public long bytesRead() {
	return dis.bytesRead();
    }

    @Override
    public int remaining() throws IOException {
        return -1;
    }

    @Override
    public int size() throws IOException {
        return -1;
    }
    
    protected final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    @Override
    public SendPortIdentifier origin() {
        return info.origin;
    }

    protected int available() throws IOException {
        checkNotFinished();
        return in.available();
    }
    
    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        checkNotFinished();
        return in.readByte();
    }

    public char readChar() throws IOException {
        checkNotFinished();
        return in.readChar();
    }

    public short readShort() throws IOException {
        checkNotFinished();
        return in.readShort();
    }

    public int readInt() throws IOException {
        checkNotFinished();
        return in.readInt();
    }

    public long readLong() throws IOException {
        checkNotFinished();
        return in.readLong();
    }

    public float readFloat() throws IOException {
        checkNotFinished();
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        checkNotFinished();
        return in.readDouble();
    }

    public String readString() throws IOException {
        checkNotFinished();
        return in.readString();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return in.readObject();
    }

    public void readArray(boolean[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(byte[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(char[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(short[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(int[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(long[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(float[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(double[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void setSequenceNumber(long s) {
        sequenceNr = s;
    }

    public long sequenceNumber() {
        if (! info.port.type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            throw new IbisConfigurationException("No COMMUNICATION_NUMBERED "
                    + "specified in port type");
        }
        return sequenceNr;
    }

    public long finish() throws IOException {
        checkNotFinished();
        in.clear();
        isFinished = true;
        if (inUpcall) {
            finishCalledFromUpcall = true;
            getInfo().upcallCalledFinish();
        }
        long after = info.bytesRead();
        long retval = after - before;
        before = after;
        info.port.finishMessage(this, retval);
        return retval;
    }

    public void finish(IOException e) {
        if (isFinished) {
            return;
        }
        if (inUpcall) {
            finishCalledFromUpcall = true;
        }
        info.port.finishMessage(this, e);
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean val) {
        isFinished = val;
        if (! isFinished) {
            before = info.bytesRead();
            finishCalledFromUpcall = false;
        }
    }

    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {
        checkNotFinished();
        in.readByteBuffer(value);
    }
}
