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

import ibis.io.SerializationOutput;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.WriteMessage;

public class LrmcWriteMessage implements WriteMessage {

    boolean isFinished = false;
    Multicaster om;
    long count = 0;
    SerializationOutput out;
    LrmcSendPort port;
    IbisIdentifier[] destinations;

    public LrmcWriteMessage(LrmcSendPort port, Multicaster om, IbisIdentifier[] dests) throws IOException {
        this.om = om;
        this.out = om.sout;
        this.port = port;
        this.destinations = dests;
        om.initializeSend(dests);
    }

    private final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException("Operating on a message that was already finished");
        }
    }

    @Override
    public ibis.ipl.SendPort localPort() {
        return port;
    }

    @Override
    public int send() throws IOException {
        checkNotFinished();
        return 0;
    }

    @Override
    public void reset() throws IOException {
        out.reset();
    }

    @Override
    public void sync(int ticket) throws IOException {
        checkNotFinished();
        out.flush();
    }

    @Override
    public void flush() throws IOException {
        checkNotFinished();
        out.flush();
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        out.writeBoolean(value);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        out.writeByte(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkNotFinished();
        out.writeChar(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkNotFinished();
        out.writeShort(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkNotFinished();
        out.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkNotFinished();
        out.writeLong(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        out.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        out.writeDouble(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        checkNotFinished();
        out.writeString(value);
    }

    @Override
    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        out.writeObject(value);
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        out.writeArray(value);
    }

    @Override
    public void writeArray(boolean[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(byte[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(char[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(short[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(int[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(long[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(float[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(double[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
    }

    @Override
    public void writeArray(Object[] value, int offset, int size) throws IOException {
        checkNotFinished();
        out.writeArray(value, offset, size);
        checkNotFinished();

    }

    @Override
    public long bytesWritten() {
        long cnt = om.bout.bytesWritten();
        long retval = cnt - count;
        count = cnt;
        return retval;
    }

    @Override
    public long finish() throws IOException {
        try {
            if (!isFinished) {
                long retval = om.finalizeSend();
                isFinished = true;
                return retval;
            }
            throw new IOException("Already finished");
        } finally {
            synchronized (port) {
                port.message = null;
                port.notifyAll();
            }
        }
    }

    @Override
    public void finish(IOException exception) {
        try {
            if (!isFinished) {
                om.finalizeSend();
            }
        } catch (Throwable e) {
            // ignored
        } finally {
            synchronized (port) {
                isFinished = true;
                port.message = null;
                port.notifyAll();
            }
        }
    }

    @Override
    public int capacity() throws IOException {
        return om.bout.bufferSize();
    }

    @Override
    public int remaining() throws IOException {
        return (int) (om.bout.bufferSize() - om.bout.bytesWritten());
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        out.writeByteBuffer(value);
    }
}
