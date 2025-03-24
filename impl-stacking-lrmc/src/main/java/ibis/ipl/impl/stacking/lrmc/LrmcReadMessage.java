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
package ibis.ipl.impl.stacking.lrmc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

import ibis.io.SerializationInput;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.lrmc.io.LrmcInputStream;
import ibis.util.ThreadPool;

public class LrmcReadMessage implements ReadMessage {

    SerializationInput in;
    boolean isFinished = false;
    Multicaster om;
    long count = 0;
    LrmcInputStream stream;
    private boolean inUpcall = false;

    public LrmcReadMessage(Multicaster om, LrmcInputStream stream) {
        this.in = om.sin;
        this.om = om;
        this.stream = stream;
    }

    void setInUpcall(boolean val) {
        inUpcall = val;
    }

    protected final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException("Operating on a message that was already finished");
        }
    }

    @Override
    public SendPortIdentifier origin() {
        int source = om.bin.getInputStream().getSource();
        IbisIdentifier id = om.lrmc.ibis.getId(source);
        return new LrmcSendPortIdentifier(id, om.name);
    }

    protected int available() throws IOException {
        checkNotFinished();
        return in.available();
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return in.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        checkNotFinished();
        return in.readByte();
    }

    @Override
    public char readChar() throws IOException {
        checkNotFinished();
        return in.readChar();
    }

    @Override
    public short readShort() throws IOException {
        checkNotFinished();
        return in.readShort();
    }

    @Override
    public int readInt() throws IOException {
        checkNotFinished();
        return in.readInt();
    }

    @Override
    public long readLong() throws IOException {
        checkNotFinished();
        return in.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        checkNotFinished();
        return in.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        checkNotFinished();
        return in.readDouble();
    }

    @Override
    public String readString() throws IOException {
        checkNotFinished();
        return in.readString();
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return in.readObject();
    }

    @Override
    public void readArray(boolean[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(byte[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(char[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(short[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(int[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(long[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(float[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(double[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(Object[] destination) throws IOException, ClassNotFoundException {
        readArray(destination, 0, destination.length);
    }

    @Override
    public void readArray(boolean[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(byte[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(char[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(short[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(int[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(long[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(float[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(double[] destination, int offset, int size) throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public void readArray(Object[] destination, int offset, int size) throws IOException, ClassNotFoundException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    @Override
    public long bytesRead() throws IOException {
        long cnt = om.bin.bytesRead();
        long retval = cnt - count;
        count = cnt;
        return retval;
    }

    @Override
    public int remaining() throws IOException {

        if (isFinished) {
            return 0;
        }

        return om.bin.available();
    }

    @Override
    public int size() throws IOException {
        return om.bin.bufferSize();
    }

    @Override
    public long finish() throws IOException {
        if (!isFinished) {
            long retval = om.finalizeRead(stream);
            om.receivePort.doFinish();
            if (inUpcall) {
                ThreadPool.createNew(om.receivePort, "ReceivePort");
            }
            isFinished = true;
            return retval;
        }
        throw new IOException("ReadMessage already finished");
    }

    @Override
    public void finish(IOException exception) {
        if (!isFinished) {
            isFinished = true;
            om.finalizeRead(stream);
            om.receivePort.doFinish();
            if (inUpcall) {
                ThreadPool.createNew(om.receivePort, "ReceivePort");
            }
        }
    }

    @Override
    public ReceivePort localPort() {
        return om.receivePort;
    }

    @Override
    public long sequenceNumber() {
        // Not supported.
        return 0;
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException, ReadOnlyBufferException {
        checkNotFinished();
        in.readByteBuffer(value);
    }

}
