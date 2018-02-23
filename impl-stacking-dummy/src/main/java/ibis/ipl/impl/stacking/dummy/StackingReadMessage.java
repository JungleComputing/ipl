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
package ibis.ipl.impl.stacking.dummy;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public class StackingReadMessage implements ReadMessage {
    
    final ReadMessage base;
    final StackingReceivePort port;
    
    public StackingReadMessage(ReadMessage base, StackingReceivePort port) {
        this.base = base;
        this.port = port;
    }

    public long bytesRead() throws IOException {
        return base.bytesRead();
    }

    public int remaining() throws IOException {
        return base.remaining();
    }

    public int size() throws IOException {
        return base.size();
    }
    
    public long finish() throws IOException {
        return base.finish();
    }

    public void finish(IOException e) {
        base.finish(e);
    }

    public ReceivePort localPort() {
        // This method is the only reason why we need a forwarder message.
        return port;
    }

    public SendPortIdentifier origin() {
        return base.origin();
    }

    public void readArray(boolean[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(boolean[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(byte[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(char[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(char[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(double[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(double[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(float[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(float[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(int[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(int[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(long[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(long[] destination) throws IOException {
        base.readArray(destination);
    }

    public void readArray(Object[] destination, int offset, int size) throws IOException, ClassNotFoundException {
        base.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination) throws IOException, ClassNotFoundException {
        base.readArray(destination);
    }

    public void readArray(short[] destination, int offset, int size) throws IOException {
        base.readArray(destination, offset, size);
    }

    public void readArray(short[] destination) throws IOException {
        base.readArray(destination);
    }

    public boolean readBoolean() throws IOException {
        return base.readBoolean();
    }

    public byte readByte() throws IOException {
        return base.readByte();
    }

    public char readChar() throws IOException {
        return base.readChar();
    }

    public double readDouble() throws IOException {
        return base.readDouble();
    }

    public float readFloat() throws IOException {
        return base.readFloat();
    }

    public int readInt() throws IOException {
        return base.readInt();
    }

    public long readLong() throws IOException {
        return base.readLong();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return base.readObject();
    }

    public short readShort() throws IOException {
        return base.readShort();
    }

    public String readString() throws IOException {
        return base.readString();
    }

    public long sequenceNumber() {
        return base.sequenceNumber();
    }

    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {
	base.readByteBuffer(value);
    }

   

}
