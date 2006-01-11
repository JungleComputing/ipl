/* $Id$ */

package ibis.impl.net.tcp;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * The TCP output implementation.
 */
public final class TcpOutput extends NetOutput {

    /**
     * The communication socket.
     */
    private Socket tcpSocket = null;

    /**
     * The peer {@link ibis.impl.net.NetReceivePort NetReceivePort}
     * local number.
     */
    private Integer rpn = null;

    /**
     * The communication input stream.
     *
     * Note: this stream is not really needed but may be used for debugging
     *       purpose.
     */
    private DataInputStream tcpIs = null;

    /**
     * The communication output stream.
     */
    private DataOutputStream tcpOs = null;

    /*
     * Object stream for the internal fallback serialization.
     */
    private ObjectOutputStream _outputConvertStream = null;

    private boolean first = true;

    /**
     * Constructor.
     *
     * @param pt the properties of the output's 
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the TCP driver instance.
     */
    TcpOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
    }

    /**
     * Sets up an outgoing TCP connection.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();
        if (this.rpn != null) {
            throw new Error("connection already established");
        }

        this.rpn = cnx.getNum();

        InputStream brokering_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_brokering");
        OutputStream brokering_out = cnx.getServiceLink().getOutputSubStream(
                this, "tcp_brokering");

        final Map p = cnx.properties();

        tcpSocket = NetIbis.socketFactory.createBrokeredSocket(brokering_in,
                brokering_out, false, p);

        brokering_in.close();
        brokering_out.close();

        tcpOs = new DataOutputStream(tcpSocket.getOutputStream());
        tcpIs = new DataInputStream(tcpSocket.getInputStream());

        mtu = 0;
        log.out();
    }

    public long finish() throws IOException {
        log.in();
        super.finish();
        if (_outputConvertStream != null) {
            _outputConvertStream.close();
            _outputConvertStream = null;
        }

        tcpOs.flush();
        first = true;
        log.out();
        // TODO: return byte count of message
        return 0;
    }

    public void reset() throws IOException {
        log.in();
        super.reset();
        if (_outputConvertStream != null) {
            _outputConvertStream.close();
            _outputConvertStream = null;
        }

        tcpOs.flush();
        first = true;
        log.out();
    }

    public void sync(int ticket) throws IOException {
        log.in();
        super.sync(ticket);
        log.out();
    }

    public long getCount()
    {
        // TODO
        return 0;
    }

    public void resetCount()
    {
        // TODO
    }

    public void writeByteBuffer(NetSendBuffer b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }
        for (int i = 0; i < b.length; i++) {
            tcpOs.writeByte((int) b.data[i]);
        }

        if (!b.ownershipClaimed) {
            b.free();
        }
        log.out();
    }

    public void writeBoolean(boolean b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }
        tcpOs.writeBoolean(b);
        log.out();
    }

    public void writeByte(byte b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeByte((int) b);
        log.out();
    }

    public void writeChar(char b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeChar((int) b);
        log.out();
    }

    public void writeShort(short b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeShort((int) b);
        log.out();
    }

    public void writeInt(int b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeInt((int) b);
        log.out();
    }

    public void writeLong(long b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeLong(b);
        log.out();
    }

    public void writeFloat(float b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeFloat(b);
        log.out();
    }

    public void writeDouble(double b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeDouble(b);
        log.out();
    }

    public void writeString(String b) throws IOException {
        log.in();
        if (first) {
            tcpOs.write(1);
            first = false;
        }

        tcpOs.writeUTF(b);
        log.out();
    }

    public void writeObject(Object o) throws IOException {
        log.in();
        if (_outputConvertStream == null) {
            DummyOutputStream dos = new DummyOutputStream();
            _outputConvertStream = new ObjectOutputStream(dos);
            _outputConvertStream.flush();
        }
        _outputConvertStream.writeObject(o);
        _outputConvertStream.flush();
        log.out();
    }

    public void writeArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeBoolean(b[o + i]);
        }
        log.out();
    }

    public void writeArray(byte[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeByte(b[o + i]);
        }
        log.out();
    }

    public void writeArray(char[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeChar(b[o + i]);
        }
        log.out();
    }

    public void writeArray(short[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeShort(b[o + i]);
        }
        log.out();
    }

    public void writeArray(int[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeInt(b[o + i]);
        }
        log.out();
    }

    public void writeArray(long[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeLong(b[o + i]);
        }
        log.out();
    }

    public void writeArray(float[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeFloat(b[o + i]);
        }
        log.out();
    }

    public void writeArray(double[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeDouble(b[o + i]);
        }
        log.out();
    }

    public void writeArray(Object[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            writeObject(b[o + i]);
        }
        log.out();
    }

    /**
     * Reset the TCP connection if it exists.
     */
    public void doFree() throws IOException {
        log.in();
        if (tcpIs != null) {
            tcpIs.close();
        }

        if (tcpOs != null) {
            tcpOs.close();
        }

        if (tcpSocket != null) {
            tcpSocket.close();
        }

        rpn = null;
        log.out();
    }

    public void free() throws IOException {
        log.in();
        doFree();
        super.free();
        log.out();
    }

    private final class DummyOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            log.in();
            writeByte((byte) b);
            log.out();
        }

        /*
         * Note: the other write methods must _not_ be overloaded
         *       because the ObjectInput/OutputStream do not guaranty
         *       symmetrical transactions.
         */
    }

    public synchronized void close(Integer num) throws IOException {
        log.in();
        if (rpn == num) {
            doFree();
        }
        log.out();
    }
}
