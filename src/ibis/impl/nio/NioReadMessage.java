package ibis.impl.nio;

import ibis.io.SerializationInputStream;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

final class NioReadMessage implements ReadMessage, Config { 
    SerializationInputStream in;
    NioInputStream nis;
    private NioReceivePort port;
    private NioSendPortIdentifier origin;
    boolean isFinished = false;

    NioReadMessage(NioReceivePort port, SerializationInputStream in, 
	    NioInputStream nis, NioSendPortIdentifier origin) {
	this.port = port;
	this.in = in;
	this.nis = nis;
	this.origin = origin;
    }

    public ReceivePort localPort() {
	return port;
    }

    protected int available() throws IOException {
	return in.available();
    }

    public long finish() throws IOException {
	in.clear();
	return port.finish(this);
    }

    public SendPortIdentifier origin() {
	return origin;
    }

    public long sequenceNumber() { 
	return -1;
    }

    public boolean readBoolean() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean");
	}
	return in.readBoolean();
    } 

    public byte readByte() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte");
	}
	return in.readByte();
    } 

    public char readChar() throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char");
	}
	return in.readChar();
    }

    public short readShort() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short");
	}
	return in.readShort();
    } 

    public int readInt() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int");
	}
	return in.readInt();
    }

    public long readLong() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long");
	}
	return in.readLong();
    } 

    public float readFloat() throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float");
	}
	return in.readFloat();
    } 

    public double readDouble() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double");
	}
	return in.readDouble();
    }

    public String readString() throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading string");
	}
	return (String) in.readUTF();
    } 

    public Object readObject() throws IOException, ClassNotFoundException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object");
	}
	return in.readObject();
    } 

    public void readArray(boolean [] destination) throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(byte [] destination) throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(char [] destination) throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(short [] destination) throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(int [] destination) throws IOException {  
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(long [] destination) throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(float [] destination) throws IOException { 
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(double [] destination) throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(Object [] destination) throws IOException, 
						    ClassNotFoundException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(boolean [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(byte [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(char [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }


    public void readArray(short [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(int [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(long [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(float [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(double [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }	

    public void readArray(Object [] destination, int offset, int size) 
				throws IOException, ClassNotFoundException {
	if(DEBUG_LEVEL >= VERY_HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }	
}  
