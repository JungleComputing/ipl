/* $Id$ */

package ibis.impl.net.id;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSendBuffer;

import java.io.IOException;

/**
 * The ID output implementation.
 */
public final class IdOutput extends NetOutput {

    /**
     * The driver used for the 'real' output.
     */
    private NetDriver subDriver = null;

    /**
     * The 'real' output.
     */
    private NetOutput subOutput = null;

    /**
     * Constructor.
     *
     * @param pt the properties of the output's 
     * {@link ibis.impl.net.NetSendPort NetSendPort}.
     * @param driver the ID driver instance.
     */
    IdOutput(NetPortType pt, NetDriver driver, String context)
            throws IOException {
        super(pt, driver, context);
    }

    public synchronized void setupConnection(NetConnection cnx)
            throws IOException {
        NetOutput subOutput = this.subOutput;

        if (subOutput == null) {
            if (subDriver == null) {
                String subDriverName = getProperty("Driver");
                subDriver = driver.getIbis().getDriver(subDriverName);
            }

            subOutput = newSubOutput(subDriver);
            this.subOutput = subOutput;
        }

        subOutput.setupConnection(cnx);

        int _mtu = subOutput.getMaximumTransfertUnit();

        if ((mtu == 0) || (mtu > _mtu)) {
            mtu = _mtu;
        }

        int _headersLength = subOutput.getHeadersLength();

        if (headerOffset < _headersLength) {
            headerOffset = _headersLength;
        }
    }

    public void initSend() throws IOException {
        super.initSend();
        subOutput.initSend();
    }

    public long finish() throws IOException {
        long retval = subOutput.finish();
        super.finish();
        return retval;
    }

    public void free() throws IOException {
        if (subOutput != null) {
            subOutput.free();
        }

        super.free();
    }

    public synchronized void close(Integer num) throws IOException {
        if (subOutput != null) {
            subOutput.close(num);
            subOutput = null;
        }
    }

    public long getCount() {
        if (subOutput != null) {
            return subOutput.getCount();
        } else {
            return 0;
        }
    }

    public void resetCount() {
        if (subOutput != null) {
            subOutput.resetCount();
        }
    }

    public void writeByteBuffer(NetSendBuffer buffer) throws IOException {
        subOutput.writeByteBuffer(buffer);
    }

    /**
     * Writes a boolean v to the message.
     * @param     v             The boolean v to write.
     */
    public void writeBoolean(boolean v) throws IOException {
        subOutput.writeBoolean(v);
    }

    /**
     * Writes a byte v to the message.
     * @param     v             The byte v to write.
     */
    public void writeByte(byte v) throws IOException {
        subOutput.writeByte(v);
    }

    /**
     * Writes a char v to the message.
     * @param     v             The char v to write.
     */
    public void writeChar(char v) throws IOException {
        subOutput.writeChar(v);
    }

    /**
     * Writes a short v to the message.
     * @param     v             The short v to write.
     */
    public void writeShort(short v) throws IOException {
        subOutput.writeShort(v);
    }

    /**
     * Writes a int v to the message.
     * @param     v             The int v to write.
     */
    public void writeInt(int v) throws IOException {
        subOutput.writeInt(v);
    }

    /**
     * Writes a long v to the message.
     * @param     v             The long v to write.
     */
    public void writeLong(long v) throws IOException {
        subOutput.writeLong(v);
    }

    /**
     * Writes a float v to the message.
     * @param     v             The float v to write.
     */
    public void writeFloat(float v) throws IOException {
        subOutput.writeFloat(v);
    }

    /**
     * Writes a double v to the message.
     * @param     v             The double v to write.
     */
    public void writeDouble(double v) throws IOException {
        subOutput.writeDouble(v);
    }

    /**
     * Writes a Serializable object to the message.
     * @param     v             The object v to write.
     */
    public void writeString(String v) throws IOException {
        subOutput.writeString(v);
    }

    /**
     * Writes a Serializable object to the message.
     * @param     v             The object v to write.
     */
    public void writeObject(Object v) throws IOException {
        subOutput.writeObject(v);
    }

    public void writeArray(boolean[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(byte[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(char[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(short[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(int[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(long[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(float[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(double[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }

    public void writeArray(Object[] b, int o, int l) throws IOException {
        subOutput.writeArray(b, o, l);
    }
}
