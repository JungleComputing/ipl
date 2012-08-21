package ibis.ipl.impl.stacking.cc;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CCReadMessage implements ReadMessage {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(CCReadMessage.class);

    /*
     * The port which will give me base ReadMessages.
     */
    final CCReceivePort recvPort;
    
    boolean isFinished = false;
    private final SendPortIdentifier origin;

    public CCReadMessage(ReadMessage m, CCReceivePort port)
            throws IOException {
        this.recvPort = port;
        this.recvPort.serIn.clear();
        this.origin = m.origin();
        
        port.dataIn.currentBaseMsg = m;
        
        logger.debug("{}: Read message created.", port);
        logger.debug("isLastPart={}, bufSize={}",
                new Object[] {port.dataIn.isLastPart, port.dataIn.remainingBytes});
    }

    /*
     * Glues the upcall in the CCReceivePort to the BufferedrecvPort.dataInputStream.
     */
    protected void offerToBuffer(boolean isLastPart, int remaining,ReadMessage msg) {
        recvPort.dataIn.offerToBuffer(isLastPart, remaining, msg);
    }

    @Override
    public long finish() throws IOException {
        long retVal = bytesRead();
        if(this.isFinished) {
            return retVal;
        }
        logger.debug("Finishing read message from {}.",
                this.origin());
        recvPort.dataIn.finish();
        isFinished = true;        
        synchronized (recvPort) {
            recvPort.currentLogicalReadMsg = null;
            recvPort.readMsgRequested = false;
            
            logger.debug("\n\tRead message from {} finished.",
                    this.origin());

            /*
             * Need to send signal to the next in line send port who wishes to
             * send us something only if he trully is the next in line.
             */
            for (Iterator<CCReceivePort.SequencedSpi> it = 
                    recvPort.toHaveMyFutureAttention.iterator(); it.hasNext();) {
                CCReceivePort.SequencedSpi seqSpi = it.next();
                
                if (recvPort.isNextSeqNo(seqSpi.seqNo)) {
                    /*
                     * This is set to false when we actually receive the
                     * message.
                     */
                    recvPort.readMsgRequested = true;
                    recvPort.incSeqNo(seqSpi.seqNo);
                    recvPort.ccManager.sideChannelHandler.newThreadSendProtocol(
                            recvPort.identifier(), seqSpi.spi,
                            SideChannelProtocol.GIVE_ME_YOUR_MESSAGE);
                    /*
                     * got my next message. get out.
                     */
                    it.remove();
                    break;
                }
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
            recvPort.dataIn.finish();
        } catch (IOException ignoreMe) {
        }

        isFinished = true;
        synchronized (recvPort) {
            recvPort.currentLogicalReadMsg = null;
            recvPort.readMsgRequested = false;
            logger.debug("Read message finished:\t{}",e.toString());

            /*
             * Need to send signal to the next in line send port who wishes to
             * send us something only if he trully is the next in line.
             */
            for (Iterator<CCReceivePort.SequencedSpi> it = 
                    recvPort.toHaveMyFutureAttention.iterator(); it.hasNext();) {
                CCReceivePort.SequencedSpi seqSpi = it.next();
                
                if (recvPort.isNextSeqNo(seqSpi.seqNo)) {
                    /*
                     * This is set to false when we actually receive the
                     * message.
                     */
                    recvPort.readMsgRequested = true;
                    recvPort.incSeqNo(seqSpi.seqNo);
                    recvPort.ccManager.sideChannelHandler.newThreadSendProtocol(
                            recvPort.identifier(), seqSpi.spi,
                            SideChannelProtocol.GIVE_ME_YOUR_MESSAGE);
                    /*
                     * got my next message. get out.
                     */
                    it.remove();
                    break;
                }
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
        return recvPort.dataIn.bytesRead();
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
        throw new IbisConfigurationException("COMMUNICATION_NUMBERED "
                + "not supported.");
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readByte();
    }

    @Override
    public char readChar() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readChar();
    }

    @Override
    public short readShort() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readShort();
    }

    @Override
    public int readInt() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readInt();
    }

    @Override
    public long readLong() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readDouble();
    }

    @Override
    public String readString() throws IOException {
        checkNotFinished();
        return recvPort.serIn.readString();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return recvPort.serIn.readObject();
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
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        checkNotFinished();
        recvPort.serIn.readArray(destination, offset, size);
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException,
            ReadOnlyBufferException {
        checkNotFinished();
        recvPort.serIn.readByteBuffer(value);
    }
    
    public static int readIntFromBytes(ReadMessage m) throws IOException {
        return ((int) m.readByte() << 24) + ((m.readByte() & 0xFF) << 16) + 
                ((m.readByte() & 0xFF) << 8) + (m.readByte() & 0xFF);
    }
}
