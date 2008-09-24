package ibis.io.jme;

import ibis.io.Replacer;

import java.io.IOException;

public class HessianObjectOutputStream extends ObjectOutputStream
implements ObjectOutput, HessianConstants {
	
	public HessianObjectOutputStream(DataOutputStream out) throws IOException {
		super(out);
	}
	public String serializationImplName() {
		return "Hessian2";
	}

	public void writeArrayBoolean(boolean[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeBoolean(source[offset + i]);
		}
	}
	public void writeArrayByte(byte[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeByte(source[offset + i]);
		}
	}
	public void writeArrayChar(char[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeChar(source[offset + i]);
		}
	}
	public void writeArrayShort(short[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeShort(source[offset + i]);
		}
	}
	public void writeArrayInt(int[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeInt(source[offset + i]);
		}
	}
	public void writeArrayLong(long[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeLong(source[offset + i]);
		}
	}
	public void writeArrayFloat(float[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeFloat(source[offset + i]);
		}
	}
	
	public void writeArrayDouble(double[] source, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeDouble(source[offset + i]);
		}
	}
	
	public void writeBoolean(boolean value) throws IOException {
		if (value) {
			out.write((byte)'T');
		}
		else {
			out.write((byte)'F');
		}
	}
	public void writeByte(byte value) throws IOException {
		out.write(value);
	}
	public void writeChar(char value) throws IOException {
		/* Hessian only supports strings so we write a string of length 1. */
		out.write(1);
		writeUTF8Char(value);
	}
	public void writeDouble(double value) throws IOException {
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
		int i = (int) value;

		if (i == value) {
			if (i == 0) {
				out.write(DOUBLE_ZERO);
				return;
			}
			else if (i == 1) {
				out.write(DOUBLE_ONE);
				return;
			}
			else if (-0x80 <= i && i < 0x80) {
				out.write(DOUBLE_BYTE);
				out.write(i);
				return;
			}
			else if (-0x8000 <= i && i < 0x8000) {
				out.write(DOUBLE_SHORT);
				out.write(i >> 8);
				out.write(i);
				return;
			}
		}
		float f = (float) value;

		if (f == value) {
			int bits = Float.floatToIntBits(f);

			out.write(DOUBLE_FLOAT);
			out.write(bits >> 24);
			out.write(bits >> 16);
			out.write(bits >> 8);
			out.write(bits);
			return;
		}

		long bits = Double.doubleToLongBits(value);

		out.write((byte)'D');
		out.write((byte)bits >> 56);
		out.write((byte)bits >> 48);
		out.write((byte)bits >> 40);
		out.write((byte)bits >> 32);
		out.write((byte)bits >> 24);
		out.write((byte)bits >> 16);
		out.write((byte)bits >> 8);
		out.write((byte)bits);
	}
	public void writeFloat(float value) throws IOException {
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
		int i = (int) value;

		if (i == value) {
			if (i == 0) {
				out.write(DOUBLE_ZERO);
				return;
			}
			else if (i == 1) {
				out.write(DOUBLE_ONE);
				return;
			}
			else if (-0x80 <= i && i < 0x80) {
				out.write(DOUBLE_BYTE);
				out.write(i);
				return;
			}
			else if (-0x8000 <= i && i < 0x8000) {
				out.write(DOUBLE_SHORT);
				out.write(i >> 8);
				out.write(i);
				return;
			}
		}

		int bits = Float.floatToIntBits(value);

		out.write(DOUBLE_FLOAT);
		out.write(bits >> 24);
		out.write(bits >> 16);
		out.write(bits >> 8);
		out.write(bits);

	}
	
	public void writeInt(int value) throws IOException {
		/*
        		# 32-bit signed integer
                ::= 'I' b3 b2 b1 b0
                ::= [x80-xbf]             # -x10 to x3f
                ::= [xc0-xcf] b0          # -x800 to x7ff
                ::= [xd0-xd7] b1 b0       # -x40000 to x3ffff
		 */
		if (value >= INT_BYTE_MIN && value <= INT_BYTE_MAX) {
			out.write(value + INT_BYTE_ZERO);
		}
		else if (value >= INT_SHORT_MIN && value <= INT_SHORT_MAX) {
			out.write((value >> 8) + INT_SHORT_ZERO);
			out.write(value);
		}
		else if (value >= INT_TRIP_MIN && value <= INT_TRIP_MAX) {
			out.write((value >> 16) + INT_TRIP_ZERO);
			out.write(value >> 8);
			out.write(value);
		}
		else {
			out.write((byte)'I');
			out.write(value >> 24);
			out.write(value >> 16);
			out.write(value >> 8);
			out.write(value);
		}
	}
	public void writeShort(short value) throws IOException {
		/*
		Hessian only supports ints so we use a subset encoding
		# 32-bit signed integer
        ::= 'I' b3 b2 b1 b0
        ::= [x80-xbf]             # -x10 to x3f
        ::= [xc0-xcf] b0          # -x800 to x7ff
        ::= [xd0-xd7] b1 b0       # -x40000 to x3ffff
		 */
		if (value >= INT_BYTE_MIN && value <= INT_BYTE_MAX) {
			out.write(value + INT_BYTE_ZERO);
		}
		else {
			out.write((value >> 8) + INT_SHORT_ZERO);
			out.write(value);
		}
	}
	public void writeString(String val) throws IOException {
		/*
        # UTF-8 encoded character string split into 64k chunks
        ::= 's' b1 b0 <utf8-data> string  # non-final chunk
        ::= 'S' b1 b0 <utf8-data>         # string of length
                                          #  0-65535
        ::= [x00-x1f] <utf8-data>         # string of length
                                          #  0-31
		 */
		if (null == val) {
			out.write((byte)'N');
			return;
		}
		int length = val.length();
		int remaining = length;
		do {
			if (remaining <= STRING_BYTE_MAX) {
				out.write((byte)remaining);
				for (int i = length - remaining; i < length; i++) {
					writeUTF8Char(val.charAt(i));
				}
				remaining = 0;
			}
			else if (remaining <= STRING_CHUNK_SIZE) {
				out.write((byte)'S');
				for (int i = length - remaining; i < length; i++) {
					writeUTF8Char(val.charAt(i));
				}
				remaining = 0;
			}
			else {
				out.write((byte)'s');
				int offset = length - remaining;
				for (int i = 0;i < STRING_CHUNK_SIZE ; i++) {
					writeUTF8Char(val.charAt(offset + i));
				}
				remaining -= STRING_CHUNK_SIZE;
			}
		} while (remaining > 0);
	}
	public void writeLong(long value) throws IOException {
		if (value >= LONG_BYTE_MIN && value <= LONG_BYTE_MAX) {
			out.write((byte)(value + LONG_BYTE_ZERO) & 0xff);
		}
		else if (value >= LONG_SHORT_MIN && value <= LONG_SHORT_MAX) {
			out.write((byte)((value >> 8) + LONG_SHORT_ZERO) & 0xff);
			out.write((byte)(value & 0xff));
		}
		else if (value >= LONG_TRIP_MIN && value <= LONG_TRIP_MAX) {
			out.write((byte)((value >> 16) + LONG_TRIP_ZERO) & 0xff);
			out.write((byte)((value >> 8) & 0xff));
			out.write((byte)(value & 0xff));
		}
		else if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE){
			out.write(LONG_INT);
			out.write((byte)((value >> 24) & 0xff));
			out.write((byte)((value >> 16) & 0xff));
			out.write((byte)((value >> 8) & 0xff));
			out.write((byte)(value) & 0xff);
		}
		else {
			out.write('L');
			out.write((byte)((value >> 56) & 0xff));
			out.write((byte)((value >> 48) & 0xff));
			out.write((byte)((value >> 40) & 0xff));
			out.write((byte)((value >> 32) & 0xff));
			out.write((byte)((value >> 24) & 0xff));
			out.write((byte)((value >> 16) & 0xff));
			out.write((byte)((value >> 8) & 0xff));
			out.write((byte)(value) & 0xff);
		}
	}
	private void writeUTF8Char(int ch) throws IOException{
		if (ch < 0x80)
			out.write(ch);
		else if (ch < 0x800) {
			out.write(0xc0 + ((ch >> 6) & 0x1f));
			out.write(0x80 + (ch & 0x3f));
		}
		else {
			out.write(0xe0 + ((ch >> 12) & 0xf));
			out.write(0x80 + ((ch >> 6) & 0x3f));
			out.write(0x80 + (ch & 0x3f));
		}
	}
}
