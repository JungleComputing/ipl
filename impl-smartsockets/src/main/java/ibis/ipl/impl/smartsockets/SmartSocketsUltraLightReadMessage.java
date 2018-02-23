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

import ibis.io.BufferedArrayInputStream;
import ibis.io.DataInputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public final class SmartSocketsUltraLightReadMessage implements ReadMessage {

    private final SerializationInput in;

    private final DataInputStream bin;

    private boolean isFinished = false;

    private boolean inUpcall = false;

    private boolean finishCalledFromUpcall = false;

    private final SendPortIdentifier origin;

    private final SmartSocketsUltraLightReceivePort port;

    private final int len;
    
    SmartSocketsUltraLightReadMessage(SmartSocketsUltraLightReceivePort port, 
            SendPortIdentifier origin, byte [] data) throws IOException {

        this.origin = origin;
        this.port = port;
        this.len = data.length;

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

        // bin = new SingleBufferArrayInputStream(data);
        bin = new BufferedArrayInputStream(
                new ByteArrayInputStream(data));
        in = SerializationFactory.createSerializationInput(serialization, bin, port.properties);		
    }

    public long bytesRead() throws IOException {
        return bin.bytesRead();
    }

    public int remaining() throws IOException {
        return bin.available();
    }

    public int size() throws IOException {
        return len;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     * @param val the value to set.
     */
    public void setInUpcall(boolean val) {
        inUpcall = val;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     */
    public boolean getInUpcall() {
        return inUpcall;
    }

    public boolean finishCalledInUpcall() {
        return finishCalledFromUpcall;
    }

    public boolean isFinished() { 
        return isFinished;
    }

    public long finish() throws IOException {

        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }

        isFinished = true;

        if (inUpcall) {
            finishCalledFromUpcall = true;
            port.newUpcallThread();
        }
        
        long retval = bin.bytesRead();
        try {
            in.close();
        } catch(Throwable e) {
            // ignore
        }
        return retval;
    }

    public void finish(IOException e) {

        if (isFinished) {
            return;
        }
        
        try {
            in.close();
        } catch(Throwable ex) {
            // ignore
        }
        
        isFinished = true;

        if (inUpcall) {
            finishCalledFromUpcall = true;
            port.newUpcallThread();
        }
    }


    public ReceivePort localPort() {
        return port;
    }

    public SendPortIdentifier origin() {
        return origin;
    }

    public void readArray(boolean[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(byte[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(char[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(short[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(int[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(long[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(float[] destination) throws IOException {
        in.readArray(destination);
    }

    public void readArray(double[] destination) throws IOException {
        in.readArray(destination);		
    }

    public void readArray(Object[] destination) throws IOException, ClassNotFoundException {
        in.readArray(destination);		
    }

    public void readArray(boolean[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(byte[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(char[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(short[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(int[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(long[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(float[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(double[] destination, int offset, int size) throws IOException {
        in.readArray(destination, offset, size);		
    }

    public void readArray(Object[] destination, int offset, int size) throws IOException, ClassNotFoundException {
        in.readArray(destination, offset, size);		
    }

    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public char readChar() throws IOException {
        return in.readChar();
    }

    public double readDouble() throws IOException {
        return in.readDouble();
    }

    public float readFloat() throws IOException {
        return in.readFloat();
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    public short readShort() throws IOException {
        return in.readShort();
    }

    public String readString() throws IOException {
        return in.readString();
    }

    public long sequenceNumber() {
        return 0;
    }

    @Override
    public void readByteBuffer(ByteBuffer value) throws IOException,
	    ReadOnlyBufferException {
	in.readByteBuffer(value);
    }


}
