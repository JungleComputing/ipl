package ibis.io.jme;

import java.io.IOException;
import java.io.UTFDataFormatException;

public class HessianObjectInputStream extends ObjectInputStream implements HessianConstants {

	public HessianObjectInputStream(DataInputStream in) throws IOException {
		super(in);
	}

	public String serializationImplName() {
		return "Hessian2";
	}

	public void readArray(boolean[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readBoolean();
		}
	}

	public void readArray(char[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readChar();
		}
	}

	public void readArray(short[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readShort();
		}
	}

	public void readArray(int[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readInt();
		}
	}

	public void readArray(long[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readLong();
		}
	}

	public void readArray(float[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readFloat();
		}
	}

	public void readArray(double[] destination, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			destination[offset + i] = readDouble();
		}
	}

	public boolean readBoolean() throws IOException {
		byte b = readByte();
		if (b == 'T') {
			return true;
		}
		else if (b == 'F') {
			return false;
		}
		throw new StreamCorruptedException("Expected boolean: " + b);
	}
	
	public char readChar() throws IOException {
		/* Hessian only supports strings so we read a string of length 1. */
		int b = in.read();
		if (b != 1) {
			throw new StreamCorruptedException("Expected char:" + b);
		}
		return (char)readUTF8Char();
	}
	
	public double readDouble() throws IOException {
		/*
		   # 64-bit IEEE double
     	   ::= 'D' b7 b6 b5 b4 b3 b2 b1 b0
           ::= x67                   # 0.0
           ::= x68                   # 1.0
           ::= x69 b0                # byte cast to double
                                     #  (-128.0 to 127.0)
           ::= x6a b1 b0             # short cast to double
           ::= x6b b3 b2 b1 b0       # 32-bit float cast to double
		 */
		byte b = in.readByte();
		switch (b) {
		case 'D':
			return in.readDouble();
		case DOUBLE_ZERO:
			return (double)0;
		case DOUBLE_ONE:
			return (double)1;
		case DOUBLE_BYTE:
			return (double)in.readByte();
		case DOUBLE_SHORT:
			return (double)in.readShort();
		case DOUBLE_FLOAT:
			return (double)in.readFloat();
		}
		throw new StreamCorruptedException("Expected double:" + b);
	}

	public float readFloat() throws IOException {
		/* 
		We use the type double for this but don't support 'D'
        # 64-bit IEEE double
        ::= 'D' b7 b6 b5 b4 b3 b2 b1 b0
        ::= x67                   # 0.0
        ::= x68                   # 1.0
        ::= x69 b0                # byte cast to double
                                  #  (-128.0 to 127.0)
        ::= x6a b1 b0             # short cast to double
        ::= x6b b3 b2 b1 b0       # 32-bit float cast to double
        */
		int b = in.read();
		switch (b) {
		case DOUBLE_ZERO:
			return (float)0;
		case DOUBLE_ONE:
			return (float)1;
		case DOUBLE_BYTE:
			return (float)in.readByte();
		case DOUBLE_SHORT:
			return (float)in.readShort();
		case DOUBLE_FLOAT:
			return (float)in.readFloat();
		}
		throw new StreamCorruptedException("Expected float: " + b);
	}

	public int readInt() throws IOException {
		/*
		 # 32-bit signed integer
        ::= 'I' b3 b2 b1 b0
        ::= [x80-xbf]             # -x10 to x3f
        ::= [xc0-xcf] b0          # -x800 to x7ff
        ::= [xd0-xd7] b1 b0       # -x40000 to x3ffff
		 */
		int b = in.read();
		
		if (b >= INT_BYTE_ENCODED_MIN && b <= INT_BYTE_ENCODED_MAX) {
			return (int)(b - INT_BYTE_ZERO);
		}
		else if (b >= INT_SHORT_ENCODED_MIN && b <= INT_SHORT_ENCODED_MAX) {
			return (int)(((b  - INT_SHORT_ZERO) << 8) + (in.read() & 0xff));
		}
		else if (b >= INT_TRIP_ENCODED_MIN && b <= INT_TRIP_ENCODED_MAX) {
			return (int)((b - INT_TRIP_ZERO) << 16) + ((in.read() & 0xff) << 8) + (in.read() & 0xff);
		}
		else if (b == 'I') {
			return (int)( 
					((in.read() & 0xff) << 24) + 
					((in.read() & 0xff) << 16) +
					((in.read() & 0xff) << 8) +
					(in.read() & 0xff));
		}
		else {
			throw new StreamCorruptedException("Expected int: " + b);
		}
	}

	public long readLong() throws IOException {
		/*
	       # 64-bit signed long integer
long       ::= 'L' b7 b6 b5 b4 b3 b2 b1 b0
           ::= [xd8-xef]             # -x08 to x0f
           ::= [xf0-xff] b0          # -x800 to x7ff
           ::= [x38-x3f] b1 b0       # -x40000 to x3ffff
           ::= x77 b3 b2 b1 b0       # 32-bit integer cast to long
		 */
		int b = in.read();
		
		if (b >= LONG_BYTE_ENCODED_MIN && b <= LONG_BYTE_ENCODED_MAX) {
			return (long)(b - LONG_BYTE_ZERO);
		}
		else if (b >= LONG_SHORT_ENCODED_MIN && b <= LONG_SHORT_ENCODED_MAX) {
			return (long)(((b  - LONG_SHORT_ZERO) << 8) + (in.read() & 0xff));
		}
		else if (b >= LONG_TRIP_ENCODED_MIN && b <= LONG_TRIP_ENCODED_MAX) {
			return (long)((b - LONG_TRIP_ZERO) << 16) + ((in.read() & 0xff) << 8) + (in.read() & 0xff);
		}
		else if (b == LONG_INT) {
			return (long)((in.read() & 0xff) << 16) + ((in.read() & 0xff) << 8) + (in.read() & 0xff);
		}
		else if (b == 'L') {
			return (long)( 
					((in.read() & 0xff) << 56) +
					((in.read() & 0xff) << 48) +
					((in.read() & 0xff) << 40) +
					((in.read() & 0xff) << 32) +
					((in.read() & 0xff) << 24) + 
					((in.read() & 0xff) << 16) +
					((in.read() & 0xff) << 8) +
					(in.read() & 0xff));
		}
		else {
			throw new StreamCorruptedException("Expected long: " + b);
		}
	}

	public short readShort() throws IOException {
		/*
		 Hessian only supports ints so we use that type
		 but only support part of it.
		 # 32-bit signed integer
         ::= 'I' b3 b2 b1 b0
         ::= [x80-xbf]             # -x10 to x3f
         ::= [xc0-xcf] b0          # -x800 to x7ff
         ::= [xd0-xd7] b1 b0       # -x40000 to x3ffff
		 */
		int b = in.read();
		
		if (b >= INT_BYTE_ENCODED_MIN && b <= INT_BYTE_ENCODED_MAX) {
			return (short)(b - INT_BYTE_ZERO);
		}
		else if (b >= INT_SHORT_ENCODED_MIN && b <= INT_SHORT_ENCODED_MAX) {
			return (short)(((b - INT_SHORT_ZERO) << 8) + (in.read() & 0xff));
		}
		else {
			throw new StreamCorruptedException("Expected short: " + b);
		}
	}
	

	private int readUTF8Char() throws IOException {
		int ch = in.read();

	    if (ch < 0x80) {
	      return ch;
	    }	
	    else if ((ch & 0xe0) == 0xc0) {
	      int ch1 = in.read();
	      int v = ((ch & 0x1f) << 6) + (ch1 & 0x3f);

	      return v;
	    }
	    else if ((ch & 0xf0) == 0xe0) {
	      int ch1 = in.read();
	      int ch2 = in.read();
	      int v = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);

	      return v;
	    }
	    throw new UTFDataFormatException("UTF Data Format Exception");
	}
	
	public String readString() throws IOException {
		/*
           # UTF-8 encoded character string split into 64k chunks
           ::= 's' b1 b0 <utf8-data> string  # non-final chunk
           ::= 'S' b1 b0 <utf8-data>         # string of length
                                             #  0-65535
           ::= [x00-x1f] <utf8-data>         # string of length
                                             #  0-31
		 */
		int b = in.read();
		if (b == 'N') {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		boolean lastChunk = false;
		do {
			if (b >= STRING_BYTE_MIN && b <= STRING_BYTE_MAX) {
				for(int i = 0; i < b; i++) {
					int c = readUTF8Char();
					sb.append(c);
					lastChunk = true;
				}
			}
			else if (b == 's' || b == 'S') {
				if (b == 'S') {
					lastChunk = true;
				}
				int length = (in.read() << 8) + in.read();

				for (int i = 0; i < length; i++) {
					int c = readUTF8Char();
					sb.append(c);
				}

				b = in.read();
			}
			else {
				throw new StreamCorruptedException("Expecting string:" + b);
			}
		} while (!lastChunk);
		
		return sb.toString();
	}
}
