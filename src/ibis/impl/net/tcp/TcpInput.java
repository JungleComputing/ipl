/* $Id$ */

package ibis.impl.net.tcp;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIO;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPort;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetReceiveBuffer;
import ibis.impl.net.NetReceivePort;
import ibis.impl.net.NetSendPort;
import ibis.ipl.ConnectionClosedException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;

public final class TcpInput extends NetInput {
    private Socket tcpSocket = null;

    private Integer spn = null;

    private DataInputStream tcpIs = null;

    private DataOutputStream tcpOs = null;

    private ObjectInputStream _inputConvertStream = null;

    private static final int INTERRUPT_TIMEOUT = 100; // ms

    private boolean interrupted = false;

    private boolean interruptible = false;

    TcpInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
    }

    public void initReceive(Integer num) throws IOException {
        //
    }

    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();
        if (this.spn != null) {
            throw new Error("connection already established");
        }

        InputStream brokered_in = cnx.getServiceLink().getInputSubStream(this,
                "tcp_brokering");
        OutputStream brokered_out = cnx.getServiceLink().getOutputSubStream(
                this, "tcp_brokering");
        NetPort port = cnx.getPort();

        final Map p;
        if (port != null) {
            if (port instanceof NetReceivePort) {
                p = ((NetReceivePort) port).properties();
            } else if (port instanceof NetSendPort) {
                p = ((NetSendPort) port).properties();
            } else {
                p = null;
            }
        } else {
            p = null;
        }

        final NetIO nn = this;

        tcpSocket = NetIbis.socketFactory.createBrokeredSocket(brokered_in,
                brokered_out, true, p);

        brokered_in.close();
        brokered_out.close();

        if (interruptible) {
            tcpSocket.setSoTimeout(INTERRUPT_TIMEOUT);
        }

        tcpIs = new DataInputStream(tcpSocket.getInputStream());
        tcpOs = new DataOutputStream(tcpSocket.getOutputStream());
        this.spn = cnx.getNum();

        log.out();
    }

    /**
     * {@inheritDoc}
     *
     * <BR><B>Note</B>: This TCP polling implementation uses the {@link
     * java.io.InputStream#available()} function to test whether at least one
     * data byte may be extracted without blocking.
     */
    protected Integer doPoll(boolean block) throws IOException {
        log.in();
        if (spn == null) {
            log.out("not connected");
            return null;
        }

        if (block || tcpIs.available() > 0) {
            /* Read the first byte that signifies arrival of a
             * message (or an empty message).
             */
            try {
                tcpIs.read();
            } catch (SocketTimeoutException e) {
                if (interrupted) {
                    interrupted = false;
                    // throw Ibis.createInterruptedIOException(e);
                    return null;
                }
            } catch (SocketException e) {
                String msg = e.getMessage();
                if (tcpSocket.isClosed()
                        || msg.equalsIgnoreCase("socket closed")
                        || msg.equalsIgnoreCase("null fd object")) {
                    throw new ConnectionClosedException(e);
                } else {
                    throw e;
                }
            }
            return spn;
        }

        log.out();
        return null;
    }

    public boolean pollIsInterruptible() throws IOException {
        return true;
    }

    public void interruptPoll() throws IOException {
        synchronized (new Object()) {
            // Make this JMM hard
        }
        interrupted = true;
    }

    public void setInterruptible(boolean interruptible) throws IOException {
        this.interruptible = interruptible;
        if (tcpSocket != null) {
            if (interruptible) {
                tcpSocket.setSoTimeout(INTERRUPT_TIMEOUT);
            } else {
                tcpSocket.setSoTimeout(0);
            }
        }
    }

    /*
    public void switchToDowncallMode() throws IOException {
        tcpSocket.setSoTimeout(INTERRUPT_TIMEOUT);
        installUpcallFunc(null);
        System.err.println(this + ": interruptiblePoll support is INCOMPLETE."
                + " Please implement!");
    }
    */

    public void switchToUpcallMode(NetInputUpcall upcallFunc)
            throws IOException {
        tcpSocket.setSoTimeout(0);
        installUpcallFunc(upcallFunc);
    }

    protected void doFinish() throws IOException {
        log.in();
        if (_inputConvertStream != null) {
            _inputConvertStream.close();
            _inputConvertStream = null;
        }
        log.out();
    }

    public NetReceiveBuffer readByteBuffer(int expectedLength)
            throws IOException {
        log.in();
        NetReceiveBuffer b = createReceiveBuffer(expectedLength);

        for (int i = 0; i < expectedLength; i++) {
            b.data[i] = readByte();
            b.length++;
        }
        log.out();
        return b;
    }

    public void readByteBuffer(NetReceiveBuffer b) throws IOException {
        log.in();
        for (int i = 0; i < b.data.length; i++) {
            b.data[i] = readByte();
            b.length++;
        }
        log.out();
    }

    public boolean readBoolean() throws IOException {
        try {
            log.in();
            boolean result = tcpIs.readBoolean();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public byte readByte() throws IOException {
        try {
            log.in();
            byte result = tcpIs.readByte();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public char readChar() throws IOException {
        try {
            log.in();
            char result = tcpIs.readChar();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public short readShort() throws IOException {
        try {
            log.in();
            short result = tcpIs.readShort();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public int readInt() throws IOException {
        try {
            log.in();
            int result = tcpIs.readInt();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public long readLong() throws IOException {
        try {
            log.in();
            long result = tcpIs.readLong();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public float readFloat() throws IOException {
        try {
            log.in();
            float result = tcpIs.readFloat();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public double readDouble() throws IOException {
        try {
            log.in();
            double result = tcpIs.readDouble();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public String readString() throws IOException {
        try {
            log.in();
            String result = tcpIs.readUTF();
            log.out();

            return result;
        } catch (SocketException e) {
            String msg = e.getMessage();
            if (tcpSocket.isClosed() || msg.equalsIgnoreCase("socket closed")
                    || msg.equalsIgnoreCase("null fd object")) {
                throw new ConnectionClosedException(e);
            } else {
                throw e;
            }
        }
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        log.in();
        Object o = null;
        if (_inputConvertStream == null) {
            DummyInputStream dis = new DummyInputStream();
            _inputConvertStream = new ObjectInputStream(dis);
        }

        o = _inputConvertStream.readObject();
        log.out();

        return o;
    }

    public void readArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readBoolean();
        }
        log.out();
    }

    public void readArray(byte[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readByte();
        }
        log.out();
    }

    public void readArray(char[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readChar();
        }
        log.out();
    }

    public void readArray(short[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readShort();
        }
        log.out();
    }

    public void readArray(int[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readInt();
        }
        log.out();
    }

    public void readArray(long[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readLong();
        }
        log.out();
    }

    public void readArray(float[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readFloat();
        }
        log.out();
    }

    public void readArray(double[] b, int o, int l) throws IOException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readDouble();
        }
        log.out();
    }

    public void readArray(Object[] b, int o, int l) throws IOException,
            ClassNotFoundException {
        log.in();
        for (int i = 0; i < l; i++) {
            b[o + i] = readObject();
        }
        log.out();
    }

    /**
     * Reset the TCP connection if it exists.
     */
    protected void doFree() throws IOException {
        log.in();
        if (tcpOs != null) {
            tcpOs.close();
        }

        if (tcpIs != null) {
            tcpIs.close();
        }

        if (tcpSocket != null) {
            tcpSocket.close();
        }

        spn = null;
        log.out();
    }

    private final class DummyInputStream extends InputStream {

        public int read() throws IOException {
            log.in();
            int result = 0;

            result = readByte();

            log.out();
            return (result & 255);
        }

        /*
         * Note: the other write methods must _not_ be overloaded
         *       because the ObjectInput/OutputStream do not guaranty
         *       symmetrical transactions.
         */
    }

    protected synchronized void doClose(Integer num) throws IOException {
        log.in();
        if (spn == num) {
            doFree();
        }
        log.out();
    }

}
