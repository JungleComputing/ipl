package ibis.ipl.impl.stacking.cache;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CacheWriteMessage implements WriteMessage {
    
    final WriteMessage base;
    final CacheSendPort port;
    
    public CacheWriteMessage(WriteMessage base, CacheSendPort port) {
        this.base = base;
        this.port = port;        
    }

    public long bytesWritten() throws IOException {
        return base.bytesWritten();
    }
    
    public int capacity() throws IOException {
        return base.capacity();
    }

    public int remaining() throws IOException {
        return base.remaining();
    }

    @Override
    public long finish() throws IOException {
        port.currentMessage = null;
        return base.finish();
    }

    @Override
    public void finish(IOException e) {
        port.currentMessage = null;
        base.finish(e);
    }

    @Override
    public SendPort localPort() {
        return port;
    }

    @Override
    public void reset() throws IOException {
        base.reset();
    }

    @Override
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

    public void writeByteBuffer(ByteBuffer value) throws IOException {
	base.writeByteBuffer(value);
    }

    
}
