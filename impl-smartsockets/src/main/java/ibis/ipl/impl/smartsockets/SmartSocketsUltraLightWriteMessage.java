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
package ibis.ipl.impl.smartsockets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.DataOutputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
// import ibis.io.SingleBufferArrayOutputStream;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

public class SmartSocketsUltraLightWriteMessage implements WriteMessage {

    private final SmartSocketsUltraLightSendPort port;
    private final SerializationOutput out;
    private final DataOutputStream bout;
    private final ByteArrayOutputStream b;

    SmartSocketsUltraLightWriteMessage(SmartSocketsUltraLightSendPort port) throws IOException {
        this.port = port;

        PortType type = port.getPortType();

        String serialization = null;

        if (type.hasCapability(PortType.SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
            serialization = "sun";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
            serialization = "ibis";
        } else if (type.hasCapability(PortType.SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }

        b = new ByteArrayOutputStream();
        bout = new BufferedArrayOutputStream(b);
        // bout = new SingleBufferArrayOutputStream(buffer);
        out = SerializationFactory.createSerializationOutput(serialization, bout, port.properties);
    }

    @Override
    public long bytesWritten() throws IOException {
        return bout.bytesWritten();
    }

    @Override
    public int capacity() throws IOException {
        // return bout.bufferSize();
        return -1;
    }

    @Override
    public int remaining() throws IOException {
        // return (int) (bout.bufferSize() - bout.bytesWritten());
        return -1;
    }

    @Override
    public long finish() throws IOException {
        out.flush();
        out.close();

        long bytes = bout.bytesWritten();

        // System.err.println("Written == " + bytes);

        port.finishedMessage(b.toByteArray());
        return bytes;
    }

    @Override
    public void finish(IOException exception) {
        try {
            port.finishedMessage(exception);
        } catch (Exception e) {
            // ignore ?
        }
    }

    @Override
    public void flush() throws IOException {
        // empty
    }

    @Override
    public SendPort localPort() {
        return port;
    }

    @Override
    public void reset() throws IOException {
        // bout.reset();
        out.reset(true);
    }

    @Override
    public int send() throws IOException {
        // empty -- excpetion ?
        return 0;
    }

    @Override
    public void sync(int ticket) throws IOException {
        // empty
    }

    @Override
    public void writeArray(boolean[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(byte[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(char[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(short[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(int[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(long[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(float[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(double[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(Object[] value) throws IOException {
        out.writeArray(value);
    }

    @Override
    public void writeArray(boolean[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(byte[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(char[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(short[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(int[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(long[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(float[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(double[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeArray(Object[] value, int offset, int length) throws IOException {
        out.writeArray(value, offset, length);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        out.writeBoolean(value);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        out.writeByte(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        out.writeChar(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        out.writeDouble(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        out.writeFloat(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        out.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        out.writeLong(value);
    }

    @Override
    public void writeObject(Object value) throws IOException {
        out.writeObject(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        out.writeShort(value);
    }

    @Override
    public void writeString(String value) throws IOException {
        out.writeString(value);
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        out.writeByteBuffer(value);
    }

}
