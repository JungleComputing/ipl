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

/*
 * Created on Jun 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ibis.ipl.util.messagecombining;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

public class CombiningReadMessage implements ReadMessage {

    private SerializationInput in;
    private StoreArrayInputStream storeIn;
    private MessageSplitter rp;
    private String ser;

    protected CombiningReadMessage(MessageSplitter rp, StoreArrayInputStream storeIn, String ser) {
        this.storeIn = storeIn;
        this.rp = rp;
        this.ser = ser;
    }

    protected void clear() throws IOException {
        if (in == null) {
            in = SerializationFactory.createSerializationInput(ser, storeIn, null);
        }
        in.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#finish()
     */
    @Override
    public long finish() throws IOException {
        return rp.messageIsFinished();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#finish(java.io.IOException)
     */
    @Override
    public void finish(IOException arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public long bytesRead() throws IOException {
        throw new IOException("Bytes Read not supported");
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#localPort()
     */
    @Override
    public ReceivePort localPort() {
        return rp.getReceivePort();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#sequenceNumber()
     */
    @Override
    public long sequenceNumber() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#origin()
     */
    @Override
    public SendPortIdentifier origin() {
        return rp.origin();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readBoolean()
     */
    @Override
    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readByte()
     */
    @Override
    public byte readByte() throws IOException {
        return in.readByte();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readChar()
     */
    @Override
    public char readChar() throws IOException {
        return in.readChar();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readShort()
     */
    @Override
    public short readShort() throws IOException {
        return in.readShort();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readInt()
     */
    @Override
    public int readInt() throws IOException {
        return in.readInt();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readLong()
     */
    @Override
    public long readLong() throws IOException {
        return in.readLong();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readFloat()
     */
    @Override
    public float readFloat() throws IOException {
        return in.readFloat();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readDouble()
     */
    @Override
    public double readDouble() throws IOException {
        return in.readDouble();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readString()
     */
    @Override
    public String readString() throws IOException {
        return in.readString();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readObject()
     */
    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(boolean[])
     */
    @Override
    public void readArray(boolean[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(byte[])
     */
    @Override
    public void readArray(byte[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(char[])
     */
    @Override
    public void readArray(char[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(short[])
     */
    @Override
    public void readArray(short[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(int[])
     */
    @Override
    public void readArray(int[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(long[])
     */
    @Override
    public void readArray(long[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(float[])
     */
    @Override
    public void readArray(float[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(double[])
     */
    @Override
    public void readArray(double[] arg0) throws IOException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(java.lang.Object[])
     */
    @Override
    public void readArray(Object[] arg0) throws IOException, ClassNotFoundException {
        in.readArray(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(boolean[], int, int)
     */
    @Override
    public void readArray(boolean[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(byte[], int, int)
     */
    @Override
    public void readArray(byte[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(char[], int, int)
     */
    @Override
    public void readArray(char[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(short[], int, int)
     */
    @Override
    public void readArray(short[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(int[], int, int)
     */
    @Override
    public void readArray(int[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(long[], int, int)
     */
    @Override
    public void readArray(long[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(float[], int, int)
     */
    @Override
    public void readArray(float[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(double[], int, int)
     */
    @Override
    public void readArray(double[] arg0, int arg1, int arg2) throws IOException {
        in.readArray(arg0, arg1, arg2);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.ReadMessage#readArray(java.lang.Object[], int, int)
     */
    @Override
    public void readArray(Object[] arg0, int arg1, int arg2) throws IOException, ClassNotFoundException {
        in.readArray(arg0, arg1, arg2);
    }

    @Override
    public int remaining() throws IOException {
        return -1;
    }

    @Override
    public int size() throws IOException {
        return -1;
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException, ReadOnlyBufferException {
        in.readByteBuffer(value);
    }
}
