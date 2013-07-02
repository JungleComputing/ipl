/* $Id$ */

package ibis.ipl.util.messagecombining;

import ibis.io.Replacer;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CombiningWriteMessage implements WriteMessage {

    private SerializationOutput out;
    private MessageCombiner sp;
    private StoreArrayOutputStream storeOut;
    private String ser;
    private Replacer replacer;
            
    protected CombiningWriteMessage(MessageCombiner sp, 
            StoreArrayOutputStream storeOut, String ser) {
        
        this.storeOut = storeOut;
        this.sp = sp;        
        this.ser = ser;
    }

    protected void clear() throws IOException {
        if (out == null) {
            out = SerializationFactory.createSerializationOutput(ser, storeOut, null);
            if (replacer != null) {
                out.setReplacer(replacer);
            }
        }
        out.reset(true);
    }

    protected void setReplacer(Replacer r) throws IOException {
        replacer = r;
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#send()
     */
    public int send() throws IOException {
        // no-op
        return 0;
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#sync(int)
     */
    public void sync(int arg0) throws IOException {
        // no-op
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#reset()
     */
    public void reset() throws IOException {
        out.reset();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#finish()
     */
    public long finish() throws IOException {
        out.reset();
        out.flush();
     
        return sp.messageIsFinished();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#finish(java.io.IOException)
     */
    public void finish(IOException arg0) {
        try { 
            finish();
        } catch (Exception e) { 
            // TODO: Handle the exception here ??
        }
        // TODO: Handle the exception here ??
    }

    public long bytesWritten() throws IOException {
	throw new IOException("Bytes Written not supported");
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#localPort()
     */
    public SendPort localPort() {
        return sp.getSendPort();
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeBoolean(boolean)
     */
    public void writeBoolean(boolean arg0) throws IOException {
        out.writeBoolean(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeByte(byte)
     */
    public void writeByte(byte arg0) throws IOException {
        out.writeByte(arg0);        
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeChar(char)
     */
    public void writeChar(char arg0) throws IOException {
        out.writeChar(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeShort(short)
     */
    public void writeShort(short arg0) throws IOException {
        out.writeShort(arg0);     
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeInt(int)
     */
    public void writeInt(int arg0) throws IOException {
        out.writeInt(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeLong(long)
     */
    public void writeLong(long arg0) throws IOException {
        out.writeLong(arg0);        
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeFloat(float)
     */
    public void writeFloat(float arg0) throws IOException {
        out.writeFloat(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeDouble(double)
     */
    public void writeDouble(double arg0) throws IOException {
        out.writeDouble(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeString(java.lang.String)
     */
    public void writeString(String arg0) throws IOException {
        out.writeString(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeObject(java.lang.Object)
     */
    public void writeObject(Object arg0) throws IOException {
        out.writeObject(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(boolean[])
     */
    public void writeArray(boolean[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(byte[])
     */
    public void writeArray(byte[] arg0) throws IOException {
        out.writeArray(arg0);        
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(char[])
     */
    public void writeArray(char[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(short[])
     */
    public void writeArray(short[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(int[])
     */
    public void writeArray(int[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(long[])
     */
    public void writeArray(long[] arg0) throws IOException {
        out.writeArray(arg0);   
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(float[])
     */
    public void writeArray(float[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(double[])
     */
    public void writeArray(double[] arg0) throws IOException {
        out.writeArray(arg0);   
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(java.lang.Object[])
     */
    public void writeArray(Object[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(boolean[], int, int)
     */
    public void writeArray(boolean[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(byte[], int, int)
     */
    public void writeArray(byte[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(char[], int, int)
     */
    public void writeArray(char[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(short[], int, int)
     */
    public void writeArray(short[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(int[], int, int)
     */
    public void writeArray(int[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(long[], int, int)
     */
    public void writeArray(long[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(float[], int, int)
     */
    public void writeArray(float[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(double[], int, int)
     */
    public void writeArray(double[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see ibis.ipl.WriteMessage#writeArray(java.lang.Object[], int, int)
     */
    public void writeArray(Object[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public int capacity() throws IOException {
        return -1;
    }

    public int remaining() throws IOException {
        return -1;
    }

    public void writeByteBuffer(ByteBuffer value) throws IOException {
	out.writeByteBuffer(value);	
    }
}
