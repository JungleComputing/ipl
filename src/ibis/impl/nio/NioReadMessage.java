package ibis.impl.nio;

import ibis.io.SerializationInputStream;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

final class NioReadMessage implements ReadMessage, Config { 
    SerializationInputStream in;
    NioDissipator dissipator;
    NioReceivePort port;
    boolean isFinished = false;
    private long sequencenr;

    NioReadMessage(NioReceivePort port, NioDissipator dissipator,
		   long sequencenr) throws IOException {
	this.port = port;
	this.dissipator = dissipator;
	this.sequencenr = sequencenr;

	if(dissipator != null) {
	    in = dissipator.sis;
	}

    }

    public ReceivePort localPort() {
	return port;
    }

    public long finish() throws IOException {
	long messageCount;
	
	in.clear();

	messageCount = dissipator.bytesRead();
	dissipator.resetBytesRead();

	port.finish(this, messageCount);

	return messageCount;
    }

    public void finish(IOException e) {
	port.finish(this, e);
    }

    public SendPortIdentifier origin() {
	return dissipator.peer;
    }

    public long sequenceNumber() { 
	return sequencenr;
    }

    public boolean readBoolean() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean");
	}
	return in.readBoolean();
    } 

    public byte readByte() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte");
	}
	return in.readByte();
    } 

    public char readChar() throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char");
	}
	return in.readChar();
    }

    public short readShort() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short");
	}
	return in.readShort();
    } 

    public int readInt() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int");
	}
	return in.readInt();
    }

    public long readLong() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long");
	}
	return in.readLong();
    } 

    public float readFloat() throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float");
	}
	return in.readFloat();
    } 

    public double readDouble() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double");
	}
	return in.readDouble();
    }

    public String readString() throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading string");
	}
	return (String) in.readUTF();
    } 

    public Object readObject() throws IOException, ClassNotFoundException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object");
	}
	return in.readObject();
    } 

    public void readArray(boolean [] destination) throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(byte [] destination) throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(char [] destination) throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(short [] destination) throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(int [] destination) throws IOException {  
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(long [] destination) throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(float [] destination) throws IOException { 
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(double [] destination) throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(Object [] destination) throws IOException, 
						    ClassNotFoundException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object[" 
			       + destination.length + "]");
	}
	in.readArray(destination, 0, destination.length); 
    }

    public void readArray(boolean [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading boolean[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(byte [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading byte[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(char [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading char[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }


    public void readArray(short [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading short[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(int [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading int[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(long [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading long[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(float [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading float[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }

    public void readArray(double [] destination, int offset, int size) 
							throws IOException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading double[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }	

    public void readArray(Object [] destination, int offset, int size) 
				throws IOException, ClassNotFoundException {
	if(DEBUG_LEVEL >= HIGH_DEBUG_LEVEL) {
	    System.err.println("ReadMessage: reading object[" + offset + "+"
			       + size + "]");
	}
	in.readArray(destination, offset, size); 
    }	
}  
