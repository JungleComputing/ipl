package ibis.ipl.impl.stacking.cache;

import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cache.io.BufferedDataInputStream;
import ibis.ipl.impl.stacking.cache.io.DowncallBufferedDataInputStream;
import ibis.ipl.impl.stacking.cache.io.UpcallBufferedDataInputStream;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cache.util.Loggers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.logging.Level;

public abstract class CacheReadMessage implements ReadMessage {

    public static class CacheReadDowncallMessage extends CacheReadMessage {

        public CacheReadDowncallMessage(ReadMessage m, CacheReceivePort port)
                throws IOException {
            super(m, port, new DowncallBufferedDataInputStream(m, port));
        }
    }

    /*
     * In this class, the boolean isLastPart has already been read.
     */
    public static class CacheReadUpcallMessage extends CacheReadMessage {

        CacheReadUpcallMessage(ReadMessage m, CacheReceivePort port)
                throws IOException {
            super(m, port, new UpcallBufferedDataInputStream(m, port));
        }
    }

    /*
     * The port which will give me base ReadMessages.
     */
    final CacheReceivePort recvPort;
    BufferedDataInputStream dataIn;
    SerializationInput in;
    boolean isFinished = false;
    long sequenceNr = -1;
    private final SendPortIdentifier origin;

    protected CacheReadMessage(ReadMessage m, CacheReceivePort port,
            BufferedDataInputStream dataIn)
            throws IOException {
        Loggers.readMsgLog.log(Level.INFO, "{0}: Read message created.", port);
        this.recvPort = port;
        this.origin = m.origin();

        PortType type = this.recvPort.getPortType();
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
        this.dataIn = dataIn;
        this.in = SerializationFactory.createSerializationInput(serialization, dataIn);
    }

    /*
     * Glues the upcall in the CacheReceivePort to the BufferedDataInpustStream.
     */
    protected void offerToBuffer(boolean isLastPart, ReadMessage msg) {
        dataIn.offerToBuffer(isLastPart, msg);
    }

    @Override
    public long finish() throws IOException {
        checkNotFinished();
        Loggers.readMsgLog.log(Level.INFO, "Finishing read message from {0}.",
                this.origin());
        dataIn.close();
//            in.clear();
        in.close();
        isFinished = true;
        long retVal = bytesRead();
        synchronized (recvPort) {
            recvPort.currentReadMsg = null;
            recvPort.readMsgRequested = false;
            
            Loggers.readMsgLog.log(Level.INFO, "\n\tRead message from {0} finished.",
                    this.origin());

            /*
             * Need to send signal to the next in line send port who wishes to
             * send us something.
             */
            if (!recvPort.toHaveMyFutureAttention.isEmpty()) {
                /*
                 * Set to false when we actually receive the message.
                 */
                recvPort.readMsgRequested = true;
                recvPort.cacheManager.sideChannelHandler.newThreadSendProtocol(
                        recvPort.identifier(), recvPort.toHaveMyFutureAttention.remove(),
                        SideChannelProtocol.GIVE_ME_YOUR_MESSAGE);
            }

            recvPort.notifyAll();
        }
        return retVal;
    }

    @Override
    public void finish(IOException e) {
        if (isFinished) {
            return;
        }
        try {
            dataIn.close();
        } catch (IOException ignoreMe) {
        }
        in.clear();
        try {
            in.close();
        } catch (IOException ingoreMe) {
        }

        isFinished = true;
        synchronized (recvPort) {
            recvPort.currentReadMsg = null;
            Loggers.readMsgLog.log(Level.INFO, "Read message finished:\t{0}",e.toString());

            /*
             * Need to send signal to the next in line send port who wishes to
             * send us something.
             */
            if (!recvPort.toHaveMyFutureAttention.isEmpty()) {
                /*
                 * Set to false when we actually receive the message.
                 */
                recvPort.readMsgRequested = true;
                recvPort.cacheManager.sideChannelHandler.newThreadSendProtocol(
                        recvPort.identifier(), recvPort.toHaveMyFutureAttention.remove(),
                        SideChannelProtocol.GIVE_ME_YOUR_MESSAGE);
            }

            recvPort.notifyAll();
        }
    }

    @Override
    public ibis.ipl.ReceivePort localPort() {
        return this.recvPort;
    }

    @Override
    public long bytesRead() {
        return dataIn.bytesRead();
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
        return origin;
    }

    @Override
    public long sequenceNumber() {
        if (!recvPort.getPortType().hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            throw new IbisConfigurationException("No COMMUNICATION_NUMBERED "
                    + "specified in port type");
        }
        return sequenceNr;
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        checkNotFinished();
        return in.readByte();
    }

    @Override
    public char readChar() throws IOException {
        checkNotFinished();
        return in.readChar();
    }

    @Override
    public short readShort() throws IOException {
        checkNotFinished();
        return in.readShort();
    }

    @Override
    public int readInt() throws IOException {
        checkNotFinished();
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        checkNotFinished();
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        checkNotFinished();
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        checkNotFinished();
        return in.readDouble();
    }

    @Override
    public String readString() throws IOException {
        checkNotFinished();
        return in.readString();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return in.readObject();
    }

    @Override
    public void readArray(boolean[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(byte[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(char[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(short[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(int[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(long[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(float[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(double[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(Object[] destination) throws IOException,
            ClassNotFoundException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException,
            ReadOnlyBufferException {
        checkNotFinished();
        in.readByteBuffer(value);
    }
}
