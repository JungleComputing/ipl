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
import java.nio.ReadOnlyBufferException;

import ibis.io.SerializationInput;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortType;

/**
 * Implementation of the {@link ibis.ipl.ReadMessage} interface. This is a
 * complete implementation, but may be extended by an implementation. In that
 * case, the
 * {@link ReceivePort#createReadMessage(SerializationInput, ReceivePortConnectionInfo)}
 * method must also be redefined.
 */
public class ReadMessage implements ibis.ipl.ReadMessage {

    protected SerializationInput in;

    protected ReceivePortConnectionInfo info;

    protected boolean isFinished = false;

    protected long before;

    protected long sequenceNr = -1;

    protected boolean inUpcall = false;

    protected boolean finishCalledFromUpcall = false;

    public ReadMessage(SerializationInput in, ReceivePortConnectionInfo info) {
        this.in = in;
        this.info = info;
        this.before = info.bytesRead();
    }

    public ibis.ipl.ReceivePort localPort() {
        return info.port;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     * 
     * @param val
     *            the value to set.
     */
    public void setInUpcall(boolean val) {
        inUpcall = val;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     * 
     * @return whether we are in an upcall
     */
    public boolean getInUpcall() {
        return inUpcall;
    }

    /**
     * May be called by an implementation to allow for detection of finish()
     * calls within an upcall.
     * 
     * @return whether finish was called from upcall
     */
    public boolean finishCalledInUpcall() {
        return finishCalledFromUpcall;
    }

    public long bytesRead() {
        long after = info.bytesRead();
        return after - before;
    }

    public int remaining() throws IOException {
        return -1;
    }

    public int size() throws IOException {
        return -1;
    }

    public ReceivePortConnectionInfo getInfo() {
        return info;
    }

    public void setInfo(ReceivePortConnectionInfo info) {
        this.info = info;
    }

    protected final void checkNotFinished() throws IOException {
        if (isFinished) {
            throw new IOException(
                    "Operating on a message that was already finished");
        }
    }

    public ibis.ipl.SendPortIdentifier origin() {
        return info.origin;
    }

    protected int available() throws IOException {
        checkNotFinished();
        return in.available();
    }

    public boolean readBoolean() throws IOException {
        checkNotFinished();
        return in.readBoolean();
    }

    public byte readByte() throws IOException {
        checkNotFinished();
        return in.readByte();
    }

    public char readChar() throws IOException {
        checkNotFinished();
        return in.readChar();
    }

    public short readShort() throws IOException {
        checkNotFinished();
        return in.readShort();
    }

    public int readInt() throws IOException {
        checkNotFinished();
        return in.readInt();
    }

    public long readLong() throws IOException {
        checkNotFinished();
        return in.readLong();
    }

    public float readFloat() throws IOException {
        checkNotFinished();
        return in.readFloat();
    }

    public double readDouble() throws IOException {
        checkNotFinished();
        return in.readDouble();
    }

    public String readString() throws IOException {
        checkNotFinished();
        return in.readString();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        checkNotFinished();
        return in.readObject();
    }

    public void readArray(boolean[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(byte[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(char[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(short[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(int[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(long[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(float[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(double[] destination) throws IOException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(Object[] destination)
            throws IOException, ClassNotFoundException {
        readArray(destination, 0, destination.length);
    }

    public void readArray(boolean[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(byte[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(char[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(short[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(int[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(long[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(float[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(double[] destination, int offset, int size)
            throws IOException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void readArray(Object[] destination, int offset, int size)
            throws IOException, ClassNotFoundException {
        checkNotFinished();
        in.readArray(destination, offset, size);
    }

    public void setSequenceNumber(long s) {
        sequenceNr = s;
    }

    public long sequenceNumber() {
        if (!info.port.type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            throw new IbisConfigurationException(
                    "No COMMUNICATION_NUMBERED " + "specified in port type");
        }
        return sequenceNr;
    }

    public long finish() throws IOException {
        checkNotFinished();
        in.clear();
        isFinished = true;
        if (inUpcall) {
            finishCalledFromUpcall = true;
            getInfo().upcallCalledFinish();
        }
        long after = info.bytesRead();
        long retval = after - before;
        before = after;
        info.port.finishMessage(this, retval);
        return retval;
    }

    public void finish(IOException e) {
        if (isFinished) {
            return;
        }
        if (inUpcall) {
            finishCalledFromUpcall = true;
        }
        info.port.finishMessage(this, e);
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean val) {
        isFinished = val;
        if (!isFinished) {
            before = info.bytesRead();
            finishCalledFromUpcall = false;
        }
    }

    public void readByteBuffer(ByteBuffer value)
            throws IOException, ReadOnlyBufferException {
        checkNotFinished();
        in.readByteBuffer(value);
    }
}
