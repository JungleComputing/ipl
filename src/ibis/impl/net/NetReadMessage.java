/* $Id$ */

package ibis.impl.net;

import java.io.IOException;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

/**
 * Provides an implementation of the {@link ReadMessage} interface of the IPL.
 */
public final class NetReadMessage implements ReadMessage {

    /** The receive port of this message. */
    NetReceivePort port;

    /** The topmost network input. */
    private NetInput input;

    /**
     * The empty message detection flag.
     *
     * The flag is set on each new {@link #fresh} call and should
     * be cleared as soon as at least a byte as been read.
     */
    boolean emptyMsg = true;

    /**
     * Seqno for numbered messages
     */
    private long messageSeqno = -1;

    /**
     * Optional (fine grained) logging object.
     *
     * This logging object should be used to display code-level information
     * like function calls, args and variable values.
     */
    protected NetLog log = null;

    /**
     * Optional (general purpose) logging object.
     *
     * This logging object should only be used temporarily for debugging
     * purpose.
     */
    protected NetLog disp = null;

    public NetReadMessage(NetReceivePort port) throws IOException {
        this.port = port;
        log = port.log;
        disp = port.disp;
        input = port.input;
    }

    /** Reinitializes for a new message. */
    void fresh() throws IOException {
        emptyMsg = true;
        if (port.type.numbered()) {
            messageSeqno = input.readSeqno();
            emptyMsg = false;
        }
    }

    public long sequenceNumber() {
        return messageSeqno;
    }

    public long finish() throws IOException {
        log.in();
        if (emptyMsg) {
            input.handleEmptyMsg();
            emptyMsg = false;
        }

        long l = port.finish();

        log.out();

        return l;
    }

    public void finish(IOException e) {
        // What to do here? Rutger?
        try {
            finish();
        } catch (IOException e2) {
            // Give up
        }
    }

    public SendPortIdentifier origin() {
        log.in();
        SendPortIdentifier spi = port.getActiveSendPortIdentifier();
        log.out();
        return spi;
    }

    public ReceivePort localPort() {
        return port;
    }

    public boolean readBoolean() throws IOException {
        log.in();
        emptyMsg = false;
        boolean v = input.readBoolean();
        log.out();
        return v;
    }

    public byte readByte() throws IOException {
        log.in();
        emptyMsg = false;
        byte v = input.readByte();
        log.out();
        return v;
    }

    public char readChar() throws IOException {
        log.in();
        emptyMsg = false;
        char v = input.readChar();
        log.out();
        return v;
    }

    public short readShort() throws IOException {
        log.in();
        emptyMsg = false;
        short v = input.readShort();
        log.out();
        return v;
    }

    public int readInt() throws IOException {
        log.in();
        emptyMsg = false;
        int v = input.readInt();
        log.out();
        return v;
    }

    public long readLong() throws IOException {
        log.in();
        emptyMsg = false;
        long v = input.readLong();
        log.out();
        return v;
    }

    public float readFloat() throws IOException {
        log.in();
        emptyMsg = false;
        float v = input.readFloat();
        log.out();
        return v;
    }

    public double readDouble() throws IOException {
        log.in();
        emptyMsg = false;
        double v = input.readDouble();
        log.out();
        return v;
    }

    public String readString() throws IOException {
        log.in();
        emptyMsg = false;
        String v = input.readString();
        log.out();
        return v;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        log.in();
        emptyMsg = false;
        Object v = input.readObject();
        log.out();
        return v;
    }

    public void readArray(boolean[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(byte[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(char[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(short[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(int[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(long[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(float[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(double[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(Object[] b) throws IOException,
            ClassNotFoundException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(byte[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(char[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(short[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(int[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(long[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(float[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(double[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(Object[] b, int o, int l) throws IOException,
            ClassNotFoundException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
        log.out();
    }

}
