package ibis.ipl.impl.stacking.cache;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CacheWriteMessage implements WriteMessage {

    final WriteMessage base;
    final CacheSendPort sendPort;

    public CacheWriteMessage(WriteMessage base, CacheSendPort port) {
        this.base = base;
        this.sendPort = port;
    }

    @Override
    public long bytesWritten() throws IOException {
        return base.bytesWritten();
    }

    @Override
    public int capacity() throws IOException {
        return base.capacity();
    }

    @Override
    public int remaining() throws IOException {
        return base.remaining();
    }

    @Override
    public long finish() throws IOException {
        try {
            return base.finish();
        } finally {
            sendPort.currentMessage = null;
        }
    }

    @Override
    public void finish(IOException e) {
        try {
            base.finish(e);
        } finally {
            sendPort.currentMessage = null;
        }
    }

    @Override
    public SendPort localPort() {
        return sendPort;
    }

    @Override
    public void reset() throws IOException {
        base.reset();
    }

    @Override
    public int send() throws IOException {
        return base.send();
    }

    @Override
    public void flush() throws IOException {
        base.flush();
    }

    @Override
    public void sync(int ticket) throws IOException {
        base.sync(ticket);
    }

    @Override
    public void writeArray(boolean[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(boolean[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(byte[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(byte[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(char[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(char[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(double[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(double[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(float[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(float[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(int[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(int[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(long[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(long[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(Object[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(Object[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeArray(short[] val, int off, int len) throws IOException {
        base.writeArray(val, off, len);
    }

    @Override
    public void writeArray(short[] val) throws IOException {
        base.writeArray(val);
    }

    @Override
    public void writeBoolean(boolean val) throws IOException {
        base.writeBoolean(val);
    }

    @Override
    public void writeByte(byte val) throws IOException {
        base.writeByte(val);
    }

    @Override
    public void writeChar(char val) throws IOException {
        base.writeChar(val);
    }

    @Override
    public void writeDouble(double val) throws IOException {
        base.writeDouble(val);
    }

    @Override
    public void writeFloat(float val) throws IOException {
        base.writeFloat(val);
    }

    @Override
    public void writeInt(int val) throws IOException {
        base.writeInt(val);
    }

    @Override
    public void writeLong(long val) throws IOException {
        base.writeLong(val);
    }

    @Override
    public void writeObject(Object val) throws IOException {
        base.writeObject(val);
    }

    @Override
    public void writeShort(short val) throws IOException {
        base.writeShort(val);
    }

    @Override
    public void writeString(String val) throws IOException {
        base.writeString(val);
    }

    @Override
    public void writeByteBuffer(ByteBuffer value) throws IOException {
        base.writeByteBuffer(value);
    }
}
