package ibis.ipl.impl.stacking.sns;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.impl.stacking.sns.util.SNSEncryption;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.crypto.CipherOutputStream;

public class SNSWriteMessage implements WriteMessage {
    
    final WriteMessage base;
    final SNSSendPort port;
    SNSEncryption encryption;
    
    ByteArrayOutputStream baos;
    CipherOutputStream cos;
    DataOutputStream dos;
    
    public SNSWriteMessage(WriteMessage base, SNSSendPort port) {
        this.base = base;
        this.port = port;
        //this.encryption = encryption;
        
//        if(encryption != null){            
//    	    this.baos = new ByteArrayOutputStream();
//    	    this.cos = new CipherOutputStream(baos, encryption.getECipher());
//    	    this.dos = new DataOutputStream (cos);
//        }
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
//    	if(encryption != null) {
//    	    dos.writeDouble(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
    	
        base.writeDouble(val);
    }

    public void writeFloat(float val) throws IOException {
//    	if(encryption != null) {
//    	    dos.writeFloat(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
    	
        base.writeFloat(val);
    }

    public void writeInt(int val) throws IOException {
    	
//    	if(encryption != null) {
//    	    dos.writeInt(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
    	
        base.writeInt(val);
    }

    public void writeLong(long val) throws IOException {
    	
//    	if(encryption != null) {
//    	    dos.writeLong(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
    	
        base.writeLong(val);
    }

    public void writeObject(Object val) throws IOException {
        base.writeObject(val);
    }

    public void writeShort(short val) throws IOException {
    	
//    	if(encryption != null) {
//    	    dos.writeShort(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
    	
        base.writeShort(val);
    }

    public void writeString(String val) throws IOException {
    	
//    	if(encryption != null) {
//    	    dos.writeBytes(val);
//    	    dos.close();
//        	
//    	    byte[] data = baos.toByteArray();
//    	    base.writeArray(data);    		
//    	}
	    
        base.writeString(val);
    }

    
}
