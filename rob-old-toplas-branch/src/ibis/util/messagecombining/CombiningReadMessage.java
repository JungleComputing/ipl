/* $Id$ */

/*
 * Created on Jun 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ibis.util.messagecombining;

import java.io.IOException;

import ibis.io.SerializationBase;
import ibis.io.SerializationInput;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

public class CombiningReadMessage implements ReadMessage {

    private SerializationInput in;
    private StoreArrayInputStream storeIn;
    private MessageSplitter rp;
    private String ser;
    
    protected CombiningReadMessage(MessageSplitter rp,
            StoreArrayInputStream storeIn, String ser) {
        this.storeIn = storeIn;
        this.rp = rp;
        this.ser = ser;
    }

    protected void clear() {
        if (in == null) {
            in = SerializationBase.createSerializationInput(ser, storeIn);
        }
        in.clear();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#finish()
     */
    public long finish() throws IOException {
        return rp.messageIsFinished();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#finish(java.io.IOException)
     */
    public void finish(IOException arg0) {
        // TODO Auto-generated method stub
    }

    public long bytesRead() throws IOException {
	throw new IOException("Bytes Read not supported");
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#localPort()
     */
    public ReceivePort localPort() {    
        return rp.getReceivePort();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#sequenceNumber()
     */
    public long sequenceNumber() {
        return 0;
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#origin()
     */
    public SendPortIdentifier origin() {        
        return rp.origin();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readBoolean()
     */
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readByte()
     */
    public byte readByte() throws IOException {
        return in.readByte();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readChar()
     */
    public char readChar() throws IOException {
        return in.readChar();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readShort()
     */
    public short readShort() throws IOException {
        return in.readShort();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readInt()
     */
    public int readInt() throws IOException {
        return in.readInt();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readLong()
     */
    public long readLong() throws IOException {
        return in.readLong();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readFloat()
     */
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readDouble()
     */
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readString()
     */
    public String readString() throws IOException {
        return in.readString();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readObject()
     */
    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(boolean[])
     */
    public void readArray(boolean[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(byte[])
     */
    public void readArray(byte[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(char[])
     */
    public void readArray(char[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(short[])
     */
    public void readArray(short[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(int[])
     */
    public void readArray(int[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(long[])
     */
    public void readArray(long[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(float[])
     */
    public void readArray(float[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(double[])
     */
    public void readArray(double[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(java.lang.Object[])
     */
    public void readArray(Object[] arg0) throws IOException,
            ClassNotFoundException {
        in.readArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(boolean[], int, int)
     */
    public void readArray(boolean[] arg0, int arg1, int arg2)
            throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(byte[], int, int)
     */
    public void readArray(byte[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);    
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(char[], int, int)
     */
    public void readArray(char[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(short[], int, int)
     */
    public void readArray(short[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(int[], int, int)
     */
    public void readArray(int[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(long[], int, int)
     */
    public void readArray(long[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(float[], int, int)
     */
    public void readArray(float[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(double[], int, int)
     */
    public void readArray(double[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.ReadMessage#readArray(java.lang.Object[], int, int)
     */
    public void readArray(Object[] arg0, int arg1, int arg2)
            throws IOException, ClassNotFoundException {
        in.readArray(arg0, arg1, arg2);
    }
}
