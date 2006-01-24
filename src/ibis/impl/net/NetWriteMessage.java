/* $Id$ */

package ibis.impl.net;

import java.io.IOException;

import ibis.ipl.WriteMessage;
import ibis.ipl.SendPort;

/**
 * Provides an implementation of the {@link WriteMessage} interface of the IPL.
 */
public final class NetWriteMessage implements WriteMessage {

    /** The send port of this message. */
    NetSendPort port;

    /** The topmost network output. */
    private NetOutput output;

    /**
     * The empty message detection flag.
     *
     * The flag is set on each new {@link #fresh} call and should
     * be cleared as soon as at least a byte as been added to the living
     * message.
     */
    private boolean emptyMsg = true;

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

    /**
     * Optional statistic object.
     */
    protected NetMessageStat stat = null;

    public NetWriteMessage(NetSendPort port) throws IOException {
        this.port = port;
        log = port.log;
        disp = port.disp;
        stat = port.stat;
        output = port.output;
    }

    /** Reinitializes for a new message. */
    void fresh() throws IOException {
        emptyMsg = true;
        output.initSend();
        if (port.type.numbered()) {
            long seqno = NetIbis.globalIbis.getSeqno(port.type.name());
            emptyMsg = false;
            output.writeSeqno(seqno);
        }
    }

    /**
     * Sends what remains to be sent.
     */
    public int send() throws IOException {
        log.in();
        int retval = output.send();
        log.out();
        return retval;
    }

    /**
     * Completes the message transmission and releases the send port.
     */
    public long finish() throws IOException {
        log.in();
        long l = 0;
        try {
            if (emptyMsg) {
                output.handleEmptyMsg();
            }
        } finally {
            l = port.finish();
        }

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

    public long bytesWritten() throws IOException {
	throw new IOException("Bytes Written not supported");
    }

    /**
     * Unconditionnaly completes the message transmission and
     * releases the send port. The writeMessage is kept by
     * the application for the next send.
     */
    public void reset() throws IOException {
        log.in();
        output.reset();
        log.out();
    }

    public void sync(int ticket) throws IOException {
        log.in();
        output.sync(ticket);
        emptyMsg = true;
        log.out();
    }

    public void writeBoolean(boolean v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addBoolean();
        output.writeBoolean(v);
        log.out();
    }

    public void writeByte(byte v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addByte();
        output.writeByte(v);
        log.out();
    }

    public void writeChar(char v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addChar();
        output.writeChar(v);
        log.out();
    }

    public void writeShort(short v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addShort();
        output.writeShort(v);
        log.out();
    }

    public void writeInt(int v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addInt();
        output.writeInt(v);
        log.out();
    }

    public void writeLong(long v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addLong();
        output.writeLong(v);
        log.out();
    }

    public void writeFloat(float v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addFloat();
        output.writeFloat(v);
        log.out();
    }

    public void writeDouble(double v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addDouble();
        output.writeDouble(v);
        log.out();
    }

    public void writeString(String v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addString();
        output.writeString(v);
        log.out();
    }

    public void writeObject(Object v) throws IOException {
        log.in();
        emptyMsg = false;
        stat.addObject();
        output.writeObject(v);
        log.out();
    }

    public void writeArray(boolean[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(byte[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(char[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(short[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(int[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(long[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(float[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(double[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(Object[] b) throws IOException {
        log.in();
        writeArray(b, 0, b.length);
        log.out();
    }

    public void writeArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        stat.addBooleanArray(l);
        emptyMsg = false;
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(byte[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addByteArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(char[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addCharArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(short[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addShortArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(int[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addIntArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(long[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addLongArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(float[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addFloatArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(double[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addDoubleArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public void writeArray(Object[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        stat.addObjectArray(l);
        output.writeArray(b, o, l);
        log.out();
    }

    public SendPort localPort() {
        return port;
    }

}
