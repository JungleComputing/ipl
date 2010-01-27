package ibis.ipl.impl.stacking.sns;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.sns.util.SNSEncryption;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.crypto.CipherInputStream;

public class SNSReadMessage implements ReadMessage {
    
    final ReadMessage base;
    final SNSReceivePort port;
    SNSEncryption encryption;
    
    ByteArrayInputStream baos;
    CipherInputStream cis;
    DataInputStream dis;
    byte[] data;
    
    
    public SNSReadMessage(ReadMessage base, SNSReceivePort port) {
        this.base = base;
        this.port = port;
        //this.encryption = encryption;
        /*
        if (encryption != null) {
		    this.baos = new ByteArrayInputStream(data);
		    this.cis = new CipherInputStream(baos, encryption.getDCipher());	   
		    this.dis = new DataInputStream (cis);
        }
        */
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
//    	if (encryption != null) {
//	    	base.readArray(data);
//
//	    	return dis.readBoolean();
//    	}
    	
        return base.readBoolean();
    }

    public byte readByte() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//
//	    	return dis.readByte();
//    	}
    	
        return base.readByte();
    }

    public char readChar() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//
//	    	return dis.readChar();
//    	}
    	
        return base.readChar();
    }

    public double readDouble() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//
//	    	return dis.readDouble();
//    	}
    	
        return base.readDouble();
    }

    public float readFloat() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//	    	
//		    return dis.readFloat();
//    	}
    	
        return base.readFloat();
    }

    public int readInt() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//	    	
//		    return dis.readInt();
//    	}
    	
        return base.readInt();
    }

    public long readLong() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//	    	
//		    return dis.readLong();
//    	}
    	
        return base.readLong();
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        return base.readObject();
    }

    public short readShort() throws IOException {
//    	if (encryption != null) {
//	    	base.readArray(data);
//	    	
//		    return dis.readShort();
//    	}
    	    	
        return base.readShort();
    }

    public String readString() throws IOException {    	   	
    	
//    	if (encryption != null) {
//	    	base.readArray(data);
//		    BufferedReader br = new BufferedReader(new InputStreamReader(dis));
//		    
//		    String result = br.readLine();
//		    //br.close();
//		    //dis.close();
//		    
//		    return result;
//    	}
    	
        return base.readString();
    }

    public long sequenceNumber() {
        return base.sequenceNumber();
    }

   

}
