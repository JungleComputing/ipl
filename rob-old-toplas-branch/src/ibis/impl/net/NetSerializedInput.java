/* $Id$ */

package ibis.impl.net;

import ibis.io.SerializationInput;

import java.io.IOException;
import java.util.Hashtable;

//2     8      16      24      32      40      48      56      64      72      80      88      96     104     112
//......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......!.......
//      |       |       |       |       |       |       |       |       |       |       |       |       |       |

/**
 * The ID input implementation.
 */
public abstract class NetSerializedInput extends NetInput {

    /**
     * The driver used for the 'real' input.
     */
    protected NetDriver subDriver = null;

    /**
     * The 'real' input.
     */
    public NetInput subInput = null;

    /**
     * The currently active {@linkplain SerializationInput serialization
     * input stream}, or <code>null</code>.
     */
    protected SerializationInput iss = null;

    /**
     * The table containing each {@linkplain SerializationInput
     * serialization input stream}.
     *
     * The table is indexed by connection numbers.
     */
    private Hashtable streamTable = null;

    /**
     * The most recently activated upcall thread if it is still alive, or
     * <code>null</code>.
     */
    protected Thread activeUpcallThread = null;

    private Integer active = null;

    private int waiters;

    /**
     * Some serialization protocols (like Sun ObjectStreams) require a full
     * kill/recreate if another connection is added. This is the default
     * behaviour of this class. Some protocols (like Ibis ObjectStreams)
     * don't require any precautions. In that case, set this field to
     * false in the constructor.
     */
    protected boolean requiresStreamReinit = true;

    public NetSerializedInput(NetPortType pt, NetDriver driver, String context,
            NetInputUpcall inputUpcall) throws IOException {
        super(pt, driver, context, inputUpcall);
        streamTable = new Hashtable();
    }

    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        log.in();
        NetInput subInput = this.subInput;

        if (subInput == null) {
            if (subDriver == null) {
                String subDriverName = getMandatoryProperty("Driver");
                subDriver = driver.getIbis().getDriver(subDriverName);
            }

            subInput = newSubInput(subDriver, upcallFunc == null ? null : this);
            this.subInput = subInput;
        }

        subInput.setupConnection(cnx);
        log.out();
    }

    public void startReceive() throws IOException {
        subInput.startReceive();
    }

    public void switchToUpcallMode(NetInputUpcall inputUpcall)
            throws IOException {
        installUpcallFunc(inputUpcall);
        subInput.switchToUpcallMode(this);
    }

    public boolean pollIsInterruptible() throws IOException {
        return subInput.pollIsInterruptible();
    }

    public void setInterruptible(boolean interruptible) throws IOException {
        subInput.setInterruptible(interruptible);
    }

    public abstract SerializationInput newSerializationInputStream()
            throws IOException;

    public void initReceive(Integer num) throws IOException {
        log.in();
        if (active != null) {
            throw new Error("Revise your calls to initReceive");
        }
        active = num;
        mtu = subInput.getMaximumTransfertUnit();
        headerOffset = subInput.getHeadersLength();

        boolean makeNewStream;
        iss = (SerializationInput) streamTable.get(active);
        if (requiresStreamReinit) {
            byte b = subInput.readByte();
            makeNewStream = (b != 0);
        } else {
            makeNewStream = (iss == null);
        }

        if (makeNewStream) {
            iss = newSerializationInputStream();
            requiresStreamReinit = iss.reInitOnNewConnection();
            if (active == null) {
                throw new Error("invalid state: activeNum is null");
            }

            streamTable.put(active, iss);
        }

        if (iss == null) {
            throw new Error("invalid state: stream not found num " + num);
        }

        log.out();
    }

    public void inputUpcall(NetInput input, Integer spn) throws IOException {
        log.in();
        Thread me = Thread.currentThread();
        synchronized (this) {
            if (spn == null) {
                throw new Error("invalid connection num");
            }

            while (active != null) {
                waiters++;
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException(e);
                } finally {
                    waiters--;
                }
            }

            initReceive(spn);
            activeUpcallThread = me;
        }

        upcallFunc.inputUpcall(this, spn);
        synchronized (this) {
            if (active == spn && activeUpcallThread == me) {
                active = null;
                activeUpcallThread = null;
                iss = null;
                if (waiters > 0) {
                    notify();
                    // notifyAll();
                }
            }
        }

        log.out();
    }

    public Integer doPoll(boolean block) throws IOException {
        log.in();
        if (subInput == null) {
            log.out();
            return null;
        }

        Integer result = subInput.poll(block);

        log.out();
        return result;
    }

    public void doFinish() throws IOException {
        log.in();
        //iss.close();
        subInput.finish();
        synchronized (this) {
            iss = null;
            active = null;
            activeUpcallThread = null;
            if (waiters > 0) {
                notify();
                // notifyAll();
            }
            // System.err.println("NetSerializedInput: finish - activeNum = "+activeNum);
        }

        log.out();
    }

    public synchronized void doClose(Integer num) throws IOException {
        log.in();
        if (subInput != null) {
            subInput.close(num);
        }
        log.out();
    }

    public void doFree() throws IOException {
        log.in();
        if (subInput != null) {
            subInput.free();
        }
        log.out();
    }

    public NetReceiveBuffer readByteBuffer(int expectedLength)
            throws IOException {
        log.in();
        NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
        log.out();
        return b;
    }

    public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
        log.in();
        subInput.readByteBuffer(buffer);
        log.out();
    }

    public boolean readBoolean() throws IOException {
        boolean b = false;

        log.in();
        b = iss.readBoolean();

        log.out();
        return b;
    }

    public byte readByte() throws IOException {
        byte b = 0;

        log.in();
        b = iss.readByte();
        log.out();

        return b;
    }

    public char readChar() throws IOException {
        char c = 0;

        log.in();
        c = iss.readChar();
        log.out();

        return c;
    }

    public short readShort() throws IOException {
        short s = 0;

        log.in();
        s = iss.readShort();
        log.out();

        return s;
    }

    public int readInt() throws IOException {
        int i = 0;

        log.in();
        i = iss.readInt();
        log.out();

        return i;
    }

    protected long readSeqno() throws IOException {
        return readLong();
    }

    public long readLong() throws IOException {
        long l = 0;

        log.in();
        l = iss.readLong();
        log.out();

        return l;
    }

    public float readFloat() throws IOException {
        float f = 0.0f;

        log.in();
        f = iss.readFloat();
        log.out();

        return f;
    }

    public double readDouble() throws IOException {
        double d = 0.0;

        log.in();
        d = iss.readDouble();
        log.out();

        return d;
    }

    public String readString() throws IOException {
        String s = null;

        log.in();
        try {
            s = (String) iss.readObject();
        } catch (ClassNotFoundException e2) {
            throw new Error("Cannot find class String", e2);
        }
        log.out();

        return s;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        Object o = null;

        log.in();
        o = iss.readObject();
        log.out();

        return o;
    }

    public void readArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(byte[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(char[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(short[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(int[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(long[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(float[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(double[] b, int o, int l) throws IOException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }

    public void readArray(Object[] b, int o, int l) throws IOException,
            ClassNotFoundException {
        log.in();
        iss.readArray(b, o, l);
        log.out();
    }
}
