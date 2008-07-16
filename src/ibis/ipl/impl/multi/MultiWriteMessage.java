/* $Id: StackingIbisStarter.java 6500 2007-10-02 18:28:50Z ceriel $ */

package ibis.ipl.impl.multi;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public class MultiWriteMessage implements WriteMessage {

    final WriteMessage base;
    final MultiSendPort port;

    public MultiWriteMessage(WriteMessage base, MultiSendPort port) {
        this.base = base;
        this.port = port;
    }

    public long bytesWritten() throws IOException {
        return base.bytesWritten();
    }

    public long finish() throws IOException {
        return base.finish();
    }

    public void finish(IOException e) {
        base.finish(e);
    }

    public SendPort localPort() {
        return port;
    }

    public void reset() throws IOException {
        base.reset();
    }

    public int send() throws IOException {
        return base.send();
    }

    public void flush() throws IOException {
        base.flush();
    }

    public void sync(int ticket) throws IOException {
        base.sync(ticket);
    }

    public void writeArray(boolean[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(boolean[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(byte[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(byte[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(char[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(char[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(double[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(double[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(float[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(float[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(int[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(int[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(long[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(long[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(Object[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(Object[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeArray(short[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    public void writeArray(short[] val) throws IOException {
        base.writeArray(val);
    }

    public void writeBoolean(boolean val) throws IOException {
        base.writeBoolean(val);
    }

    public void writeByte(byte val) throws IOException {
        base.writeByte(val);
    }

    public void writeChar(char val) throws IOException {
        base.writeChar(val);
    }

    public void writeDouble(double val) throws IOException {
        base.writeDouble(val);
    }

    public void writeFloat(float val) throws IOException {
        base.writeFloat(val);
    }

    public void writeInt(int val) throws IOException {
        base.writeInt(val);
    }

    public void writeLong(long val) throws IOException {
        base.writeLong(val);
    }

    public void writeObject(Object val) throws IOException {
        base.writeObject(val);
    }

    public void writeShort(short val) throws IOException {
        base.writeShort(val);
    }

    public void writeString(String val) throws IOException {
        base.writeString(val);
    }

}
