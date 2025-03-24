/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.util.messagecombining;

import java.io.IOException;
import java.nio.ByteBuffer;

import ibis.io.Replacer;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class CombiningWriteMessage implements WriteMessage {

    private SerializationOutput out;
    private MessageCombiner sp;
    private StoreArrayOutputStream storeOut;
    private String ser;
    private Replacer replacer;

    protected CombiningWriteMessage(MessageCombiner sp, StoreArrayOutputStream storeOut, String ser) {

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

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#send()
     */
    @Override
    public int send() throws IOException {
        // no-op
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#sync(int)
     */
    @Override
    public void sync(int arg0) throws IOException {
        // no-op
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#reset()
     */
    @Override
    public void reset() throws IOException {
        out.reset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#finish()
     */
    @Override
    public long finish() throws IOException {
        out.reset();
        out.flush();

        return sp.messageIsFinished();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#finish(java.io.IOException)
     */
    @Override
    public void finish(IOException arg0) {
        try {
            finish();
        } catch (Exception e) {
            // TODO: Handle the exception here ??
        }
        // TODO: Handle the exception here ??
    }

    @Override
    public long bytesWritten() throws IOException {
        throw new IOException("Bytes Written not supported");
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#localPort()
     */
    @Override
    public SendPort localPort() {
        return sp.getSendPort();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeBoolean(boolean)
     */
    @Override
    public void writeBoolean(boolean arg0) throws IOException {
        out.writeBoolean(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeByte(byte)
     */
    @Override
    public void writeByte(byte arg0) throws IOException {
        out.writeByte(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeChar(char)
     */
    @Override
    public void writeChar(char arg0) throws IOException {
        out.writeChar(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeShort(short)
     */
    @Override
    public void writeShort(short arg0) throws IOException {
        out.writeShort(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeInt(int)
     */
    @Override
    public void writeInt(int arg0) throws IOException {
        out.writeInt(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeLong(long)
     */
    @Override
    public void writeLong(long arg0) throws IOException {
        out.writeLong(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeFloat(float)
     */
    @Override
    public void writeFloat(float arg0) throws IOException {
        out.writeFloat(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeDouble(double)
     */
    @Override
    public void writeDouble(double arg0) throws IOException {
        out.writeDouble(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeString(java.lang.String)
     */
    @Override
    public void writeString(String arg0) throws IOException {
        out.writeString(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeObject(java.lang.Object)
     */
    @Override
    public void writeObject(Object arg0) throws IOException {
        out.writeObject(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(boolean[])
     */
    @Override
    public void writeArray(boolean[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(byte[])
     */
    @Override
    public void writeArray(byte[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(char[])
     */
    @Override
    public void writeArray(char[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(short[])
     */
    @Override
    public void writeArray(short[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(int[])
     */
    @Override
    public void writeArray(int[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(long[])
     */
    @Override
    public void writeArray(long[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(float[])
     */
    @Override
    public void writeArray(float[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(double[])
     */
    @Override
    public void writeArray(double[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(java.lang.Object[])
     */
    @Override
    public void writeArray(Object[] arg0) throws IOException {
        out.writeArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(boolean[], int, int)
     */
    @Override
    public void writeArray(boolean[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(byte[], int, int)
     */
    @Override
    public void writeArray(byte[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(char[], int, int)
     */
    @Override
    public void writeArray(char[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(short[], int, int)
     */
    @Override
    public void writeArray(short[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(int[], int, int)
     */
    @Override
    public void writeArray(int[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(long[], int, int)
     */
    @Override
    public void writeArray(long[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(float[], int, int)
     */
    @Override
    public void writeArray(float[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(double[], int, int)
     */
    @Override
    public void writeArray(double[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.WriteMessage#writeArray(java.lang.Object[], int, int)
     */
    @Override
    public void writeArray(Object[] arg0, int arg1, int arg2) throws IOException {
        out.writeArray(arg0, arg1, arg2);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public int capacity() throws IOException {
        return -1;
    }

    @Override
    public int remaining() throws IOException {
        return -1;
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        out.writeByteBuffer(value);
    }
}
