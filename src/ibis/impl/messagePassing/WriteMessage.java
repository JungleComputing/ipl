package ibis.impl.messagePassing;

import java.io.IOException;

/**
 * messagePassing WriteMessage that performs Byte serialization
 */
class WriteMessage implements ibis.ipl.WriteMessage {

    protected SendPort sPort;

    protected ByteOutputStream out;

    long before;

    WriteMessage(SendPort sPort) {
        if (Ibis.DEBUG) {
            System.err
                    .println("**************************************************Creating new SendPort port = "
                            + sPort);
        }
        this.sPort = sPort;
        out = sPort.out;
        before = out.getCount();
    }

    public ibis.ipl.SendPort localPort() {
        return sPort;
    }

    public int send() throws IOException {
        out.flush();
        return out.getSentFrags();
    }

    public void reset() throws IOException {
        // No-op, reserve for subclasses
    }

    public long finish() throws IOException {
        Ibis.myIbis.lock();
        try {
            // We must do a send here.
            out.send(true);
            sPort.registerSend();
            out.reset(true);
            sPort.finishMessage();
        } finally {
            Ibis.myIbis.unlock();
        }
        long after = out.getCount();
        long retval = after - before;
        before = after;
        sPort.count += retval;
        return retval;
    }

    public void finish(IOException e) {
        // What to do here? Rutger?
        try {
            finish();
        } catch (IOException e2) {
            // Give up
        }
    }

    public void sync(int ticketno) throws IOException {
        Ibis.myIbis.lock();
        try {
            out.waitForFragno(ticketno);
        } finally {
            Ibis.myIbis.unlock();
        }
    }

    public void writeBoolean(boolean value) throws IOException {
        throw new IOException("Write Boolean not supported");
    }

    public void writeByte(byte value) throws IOException {
        out.write(value);
    }

    public void writeChar(char value) throws IOException {
        throw new IOException("Write Char not supported");
    }

    public void writeShort(short value) throws IOException {
        throw new IOException("Write Short not supported");
    }

    public void writeInt(int value) throws IOException {
        throw new IOException("Write Int not supported");
    }

    public void writeLong(long value) throws IOException {
        throw new IOException("Write Long not supported");
    }

    public void writeFloat(float value) throws IOException {
        throw new IOException("Write Float not supported");
    }

    public void writeDouble(double value) throws IOException {
        throw new IOException("Write Double not supported");
    }

    public void writeString(String value) throws IOException {
        throw new IOException("Write String not supported");
    }

    public void writeObject(Object value) throws IOException {
        throw new IOException("Write Object not supported");
    }

    public void writeArray(boolean[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(byte[] value) throws IOException {
        out.write(value);
    }

    public void writeArray(char[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(short[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(int[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(long[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(float[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(double[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(Object[] value) throws IOException {
        writeObject(value);
    }

    public void writeArray(boolean[] value, int offset, int size)
            throws IOException {
        boolean[] temp = new boolean[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(byte[] value, int offset, int size)
            throws IOException {
        byte[] temp = new byte[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(char[] value, int offset, int size)
            throws IOException {
        char[] temp = new char[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(short[] value, int offset, int size)
            throws IOException {
        short[] temp = new short[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(int[] value, int offset, int size)
            throws IOException {
        int[] temp = new int[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(long[] value, int offset, int size)
            throws IOException {
        long[] temp = new long[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(float[] value, int offset, int size)
            throws IOException {
        float[] temp = new float[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(double[] value, int offset, int size)
            throws IOException {
        double[] temp = new double[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }

    public void writeArray(Object[] value, int offset, int size)
            throws IOException {
        Object[] temp = new Object[size];
        System.arraycopy(value, offset, temp, 0, size);
        writeObject(temp);
    }
}