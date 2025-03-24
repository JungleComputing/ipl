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

package ibis.ipl.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import ibis.io.SerializationOutput;

/**
 * Implementation of the {@link ibis.ipl.WriteMessage} interface. This is a
 * complete implementation, but may be extended by an implementation. In that
 * case, the {@link SendPort#createWriteMessage()} method must also be
 * redefined.
 */
public class WriteMessage implements ibis.ipl.WriteMessage {

    protected SerializationOutput out;

    protected SendPort port;

    protected boolean isFinished = false;

    protected long before;

    protected WriteMessage(SendPort port) {
        this.port = port;
    }

    protected void initMessage(SerializationOutput out) {
        this.out = out;
        this.isFinished = false;
        this.before = port.bytesWritten();
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

    private void throwException(Throwable e) throws IOException {
        IOException ex;
        if (e instanceof IOException) {
            ex = (IOException) e;
        } else {
            ex = new IOException("Unexpected exception", e);
        }
        port.gotSendException(this, ex);
    }

    @Override
    public void reset() throws IOException {
        try {
            out.reset();
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void sync(int ticket) throws IOException {
        checkNotFinished();
        try {
            out.flush();
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        checkNotFinished();
        try {
            out.flush();
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        checkNotFinished();
        try {
            out.writeBoolean(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeByte(byte value) throws IOException {
        checkNotFinished();
        try {
            out.writeByte(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeChar(char value) throws IOException {
        checkNotFinished();
        try {
            out.writeChar(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeShort(short value) throws IOException {
        checkNotFinished();
        try {
            out.writeShort(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        checkNotFinished();
        try {
            out.writeInt(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeLong(long value) throws IOException {
        checkNotFinished();
        try {
            out.writeLong(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeFloat(float value) throws IOException {
        checkNotFinished();
        try {
            out.writeFloat(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeDouble(double value) throws IOException {
        checkNotFinished();
        try {
            out.writeDouble(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeString(String value) throws IOException {
        checkNotFinished();
        try {
            out.writeString(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeObject(Object value) throws IOException {
        checkNotFinished();
        try {
            out.writeObject(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(boolean[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(byte[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(char[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(short[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(int[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(long[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(float[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(double[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public void writeArray(Object[] value, int offset, int size) throws IOException {
        checkNotFinished();
        try {
            out.writeArray(value, offset, size);
        } catch (Throwable e) {
            throwException(e);
        }
    }

    @Override
    public long bytesWritten() {
        return port.bytesWritten() - before;
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
    public long finish() throws IOException {
        checkNotFinished();
        try {
            out.reset();
        } catch (Throwable e) {
            throwException(e);
        }
        try {
            out.flush();
        } catch (Throwable e) {
            throwException(e);
        }
        isFinished = true;
        long retval = bytesWritten();
        port.finishMessage(this, retval);
        return retval;
    }

    @Override
    public void finish(IOException e) {
        if (isFinished) {
            return;
        }

        try {
            out.reset();
        } catch (Throwable e2) {
            // ignored
        }

        try {
            out.flush();
        } catch (Throwable e2) {
            // ignored
        }

        isFinished = true;
        port.finishMessage(this, e);
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        checkNotFinished();
        try {
            out.writeByteBuffer(value);
        } catch (Throwable e) {
            throwException(e);
        }
    }

}
